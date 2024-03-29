package me.devld.wd.service.deploy

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import me.devld.wd.data.*
import me.devld.wd.data.DeploymentTaskStatus.*
import me.devld.wd.repository.DeploymentLogRepository
import me.devld.wd.repository.DeploymentRepository
import me.devld.wd.repository.DeploymentTaskRepository
import me.devld.wd.service.ProjectService
import me.devld.wd.service.TaskLogService
import me.devld.wd.service.VariableService
import me.devld.wd.service.deploy.ActionParams.Companion.getDescriptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.annotation.Resource
import kotlin.reflect.KClass

/**
 * DeploymentService
 *
 * @author devld
 */
@Component
class DeploymentService(
    private val projectService: ProjectService,
    private val deploymentRepo: DeploymentRepository,
    private val deploymentLogRepo: DeploymentLogRepository,
    private val taskRepo: DeploymentTaskRepository,
    private val variableService: VariableService,
    private val paramResolvers: List<ActionParamResolver>,
    private val logService: TaskLogService,
    actions: List<Action>,
) : DisposableBean {
    companion object {
        private val OBJECT_MAPPER = ObjectMapper()
        private val VARIABLE_PATTERN = Regex("\\{\\{([A-z0-9_]+)(:(.*))?}}")
    }

    private val log = LoggerFactory.getLogger(this::class.java)

    @Resource
    @Lazy
    private lateinit var self: DeploymentService

    private val actions: Map<String, Action> = actions.associateBy { it.name }

    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val submittedTaskIds: MutableMap<Long, Future<*>?> = ConcurrentHashMap()

    fun getDeploymentsByProjectId(projectId: Long): List<DeploymentOut> {
        return deploymentRepo.findAllByProjectId(projectId).map { DeploymentOut(it) }
    }

    fun triggerDeployment(triggerData: DeploymentTriggerIn) {
        val deployment = deploymentRepo.findById(triggerData.deploymentId!!).orElseThrow {
            IllegalArgumentException("Deployment ${triggerData.deploymentId} not found")
        }
        val projectVersion =
            (triggerData.projectVersion?.let {
                projectService.getProjectVersion(
                    deployment.project?.id!!,
                    triggerData.projectVersion!!
                )
            } ?: projectService.getProjectLatestVersion(deployment.project?.id!!))
                ?: throw IllegalArgumentException("Project version not exists")

        val tasks = self.createTasks(triggerData, deployment.steps!!)
        self.rescheduleTasks(tasks.map { it.id!! }, projectVersion.id!!)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun cancelDeployment(logId: Long) {
        deploymentLogRepo.findById(logId).orElseThrow {
            IllegalArgumentException("Deployment log $logId not found")
        }.let { cancelTasks(it.tasks!!.map { t -> t.id!! }) }
    }

    fun cancelTasks(taskIds: List<Long>) {
        if (taskIds.isEmpty()) return
        taskRepo.getByIds(taskIds).forEach {
            if (it.status!!.finished) return@forEach
            self.task(it.id!!) { task ->
                task.status = CANCELED
                task
            }
            submittedTaskIds[it.id!!]?.cancel(true)
        }
    }

    fun deleteDeployment(deploymentId: Long) {
        // TODO
    }

    // ---------- internals ----------

    fun getDeploymentLog(logId: Long): DeploymentLog {
        return deploymentLogRepo.findById(logId).orElseThrow {
            IllegalArgumentException("Deployment log $logId not found")
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun createTasks(triggerData: DeploymentTriggerIn, steps: List<DeploymentStep>): List<DeploymentTask> {
        val log = deploymentLogRepo.save(DeploymentLog().apply {
            deploymentId = triggerData.deploymentId
            createdAt = System.currentTimeMillis()
            createdBy = triggerData.triggerBy
            message = triggerData.message
        })
        val tasks = steps.map { step ->
            taskRepo.save(DeploymentTask().apply {
                deploymentLog = log
                deploymentStep = step
                waitingFor = mutableListOf()
                wakeupTasks = mutableListOf()
                status = PENDING
                taskOrder = step.stepOrder
                createdAt = System.currentTimeMillis()
            })
        }
        tasks.forEachIndexed { index, task ->
            tasks.subList(0, index).reversed().find { !it.deploymentStep!!.async }?.let {
                task.waitingFor!!.add(it.id!!)
            }
            run {
                tasks.subList(index + 1, tasks.size).forEach {
                    task.wakeupTasks!!.add(it.id!!)
                    if (!it.deploymentStep!!.async) return@run
                }
            }
            if (task.waitingFor!!.isNotEmpty() || task.wakeupTasks!!.isNotEmpty()) {
                taskRepo.save(task)
            }
        }
        return tasks
    }

    @Transactional(rollbackFor = [Exception::class])
    fun rescheduleTasks(taskIds: List<Long>, projectVersionId: Long): List<DeploymentTask> {
        if (taskIds.isEmpty()) return emptyList()
        val tasks = taskRepo.getByIds(taskIds)
        val tasksMap = tasks.associateBy { it.id!! }
        val filteredTasks = tasks.filter { task ->
            task.waitingFor!!.isEmpty() || task.waitingFor!!.map { tasksMap[it] ?: taskRepo.getById(it) }.none {
                !it.status!!.finished || (it.status != SUCCESS && !it.deploymentStep!!.ignoreOnFailure)
            }
        }
        if (filteredTasks.isEmpty()) return filteredTasks

        filteredTasks.forEach { task ->
            if (submittedTaskIds.containsKey(task.id!!)) return@forEach
            val f = executor.submit {
                try {
                    runTask(task.id!!, projectVersionId)
                } catch (e: Throwable) {
                    log.error("run task error", e)
                }
            }
            submittedTaskIds[task.id!!] = f
        }

        return filteredTasks
    }

    @Transactional(rollbackFor = [Exception::class])
    fun task(taskId: Long, update: ((DeploymentTask) -> DeploymentTask?)? = null): DeploymentTask? {
        val task = taskRepo.findById(taskId).orElseThrow {
            IllegalArgumentException("Task $taskId not found")
        }
        if (update == null) return task
        val temp = update(task) ?: return null
        return taskRepo.save(temp)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun createActionContext(taskId: Long, projectVersionId: Long): ActionContext {
        val projectVersion = projectService.getProjectVersion(projectVersionId)
            ?: throw IllegalArgumentException("Project version $projectVersionId not found")
        projectVersion.artifacts!!.size // eager fetch
        val task = taskRepo.findById(taskId).orElseThrow {
            IllegalArgumentException("Task $taskId not found")
        }
        task.deploymentLog!!.runtimeData // eager fetch
        task.deploymentStep?.deployment?.project?.name // eager fetch
        return ActionContextImpl(task, projectVersion)
    }

    private fun resolveParam(params: Map<String, String>, descriptor: ActionParamDescriptor): Any? {
        var value: Any? = null
        paramResolvers.forEach {
            if (it.supports(descriptor)) {
                val v = it.resolve(params, descriptor)
                if (v != null) {
                    value = v
                    return@forEach
                }
            }
        }
        return value
    }

    private fun runTask(taskId: Long, projectVersionId: Long) {
        val task = self.task(taskId) {
            if (it.status != PENDING) {
                log.warn("Task $taskId is not pending")
                return@task null
            }
            it.deploymentStep?.action // eager fetch
            it.status = RUNNING
            it.startedAt = System.currentTimeMillis()
            it
        } ?: return

        var success = false
        try {
            val step = task.deploymentStep!!
            val action = actions[step.action!!] ?: throw IllegalArgumentException("Action ${step.action} not found")
            val ctx = self.createActionContext(taskId, projectVersionId)
            action.execute(ctx)

            success = true
        } catch (e: Throwable) {
            log.error("Task $taskId failed", e)
        } finally {
            self.task(taskId) {
                if (it.status == CANCELED) {
                    log.info("Task $taskId canceled")
                    null
                } else {
                    it.status = if (success) SUCCESS else FAILURE
                    it.finishedAt = System.currentTimeMillis()
                    it
                }
            }
            submittedTaskIds.remove(taskId)
            if (task.wakeupTasks?.isNotEmpty()!!) {
                self.rescheduleTasks(task.wakeupTasks!!, projectVersionId)
            }
        }
    }

    override fun destroy() {
        executor.shutdownNow()
    }

    private inner class ActionContextImpl(
        private val task: DeploymentTask,
        private val projectVersion: ProjectVersion,
    ) : ActionContext {
        private val values =
            task.deploymentLog!!.runtimeData?.let { OBJECT_MAPPER.readValue(it) } ?: mutableMapOf<String, String>()

        private val step = task.deploymentStep!!

        private val project = step.deployment?.project ?: throw IllegalArgumentException("Project not found")

        private val projectCurrentVersion = projectService.getProjectCurrentVersion(project.id!!)

        override fun getProject(): Project = project

        override fun getProjectCurrentVersion(): ProjectVersion? = projectCurrentVersion

        override fun getProjectVersion(): ProjectVersion = projectVersion

        override fun <T : ActionParams> parseParams(pClass: KClass<T>): T {
            val vMap = OBJECT_MAPPER.readValue<MutableMap<String, Any?>>(step.actionParams!!)
            val value = pClass.constructors.find { it.parameters.isEmpty() }?.call()
                ?: throw IllegalArgumentException("No empty constructor found for class $pClass")

            val parsedVMap = vMap.filter { it.value != null }.mapValues { (_, v) ->
                var str = v!!.toString()
                var i = 0
                while (i < str.length) {
                    val m = VARIABLE_PATTERN.find(str, i) ?: break
                    val parsedValue = getVariable(m.groupValues[1]) ?: m.groupValues[3]
                    val newStr = str.replaceRange(m.range, parsedValue)
                    i = if (str == newStr) m.range.last + 1
                    else m.range.first
                    str = newStr
                }
                str
            }

            getDescriptor(value::class).forEach {
                val resolvedValue = resolveParam(parsedVMap, it)
                if (it.required && resolvedValue == null) {
                    throw IllegalArgumentException("Required parameter ${it.name} is not set")
                }
                if (resolvedValue != null || it.prop.returnType.isMarkedNullable) {
                    it.prop.setter.call(value, resolvedValue)
                }
            }
            return value
        }

        private fun getVariable(key: String): String? =
            variableService.get(key).let { if (StringUtils.hasText(it)) it else getValue(key) }
                .let { if (StringUtils.hasText(it)) it else null }

        override fun getValue(key: String): String? = values[key]
        override fun setValue(key: String, value: String) {
            values[key] = value
            self.task(task.id!!) {
                it.deploymentLog!!.runtimeData = OBJECT_MAPPER.writeValueAsString(values)
                it
            }
        }

        override fun log(logLevel: LogLevel, msg: String) = logService.log(task.id!!, logLevel, msg)

    }
}
