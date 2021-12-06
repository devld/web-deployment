package me.devld.wd.service.deploy

import me.devld.wd.data.Project
import me.devld.wd.data.ProjectVersion
import me.devld.wd.data.Server
import me.devld.wd.data.VariableType
import me.devld.wd.repository.ServerRepository
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

interface Action {
    val name: String
    fun execute(ctx: ActionContext)
}

interface ActionParams {
    companion object {
        fun getDescriptor(clazz: KClass<*>): List<ActionParamDescriptor> =
            clazz.memberProperties
                .filter {
                    it.javaField?.isAnnotationPresent(ActionParam::class.java) ?: false
                            && it is KMutableProperty<*>
                }
                .map {
                    it.javaField!!.getAnnotationsByType(ActionParam::class.java).first().let { a ->
                        ActionParamDescriptor(
                            it.name,
                            a!!.name,
                            a.description,
                            a.type,
                            a.required,
                            it as KMutableProperty<*>
                        )
                    }
                }
    }
}

@Target(AnnotationTarget.FIELD)
annotation class ActionParam(
    val name: String,
    val description: String,
    val type: VariableType,
    val required: Boolean = true
)

class ActionParamDescriptor(
    val field: String,
    val name: String,
    val description: String,
    val type: VariableType,
    val required: Boolean,
    val prop: KMutableProperty<*>
)

interface ActionContext {
    fun getProject(): Project
    fun getProjectCurrentVersion(): ProjectVersion?
    fun getProjectVersion(): ProjectVersion

    fun <T : ActionParams> parseParams(pClass: KClass<T>): T

    fun getValue(key: String): String?
    fun setValue(key: String, value: String)

    fun log(logLevel: LogLevel, msg: String)
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

interface ActionParamResolver {
    fun resolve(params: Map<String, String>, descriptor: ActionParamDescriptor): Any?
    fun supports(descriptor: ActionParamDescriptor): Boolean = true
}

@Component
class PrimitiveActionParamResolver : ActionParamResolver {
    override fun resolve(params: Map<String, String>, descriptor: ActionParamDescriptor): Any? {
        val strVal = params[descriptor.field] ?: return null
        return when (descriptor.prop.returnType.classifier) {
            String::class -> strVal
            Int::class -> strVal.toIntOrNull()
            Long::class -> strVal.toLongOrNull()
            Float::class -> strVal.toFloatOrNull()
            Double::class -> strVal.toDoubleOrNull()
            Boolean::class -> strVal.trim().let { it.isNotEmpty() && it.lowercase() != "false" }
            else -> throw IllegalArgumentException("Unsupported type ${descriptor.prop.returnType.classifier}")
        }
    }

    override fun supports(descriptor: ActionParamDescriptor): Boolean {
        val type = descriptor.type
        val kType = descriptor.prop.returnType.classifier
        return (type == VariableType.STRING || type == VariableType.NUMBER || type == VariableType.BOOLEAN) &&
                (kType == String::class || kType == Int::class ||
                        kType == Long::class || kType == Float::class ||
                        kType == Double::class || kType == Boolean::class)
    }

}

@Component
class ServerActionParamResolver(private val serverRepo: ServerRepository) : ActionParamResolver {
    override fun resolve(params: Map<String, String>, descriptor: ActionParamDescriptor): Any? {
        val serverId = params[descriptor.field]?.toLongOrNull() ?: return null
        return serverRepo.findById(serverId).orElse(null)
    }

    override fun supports(descriptor: ActionParamDescriptor): Boolean =
        descriptor.type == VariableType.SERVER &&
                descriptor.prop.returnType.classifier == Server::class
}


