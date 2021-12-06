package me.devld.wd.service.deploy.ssh

import me.devld.wd.config.AppConfig
import me.devld.wd.config.security.DataEncryption
import me.devld.wd.data.AuthMethod
import me.devld.wd.data.Server
import me.devld.wd.data.VariableType
import me.devld.wd.service.deploy.Action
import me.devld.wd.service.deploy.ActionContext
import me.devld.wd.service.deploy.ActionParam
import me.devld.wd.service.deploy.ActionParams
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.*
import kotlin.reflect.KClass

abstract class SSHAction<T : SSHActionParams>(
    config: AppConfig,
    private val enc: DataEncryption,
    private val timer: Timer
) : Action {
    companion object {
        private const val SSH_DIR_NAME = "ssh"
        private const val KNOWN_HOSTS_FILE = "known_hosts"
    }

    protected val log: Logger = LoggerFactory.getLogger(this.javaClass)

    private val paramsType: KClass<T>
    private val dataDir = Paths.get(config.dataDir, SSH_DIR_NAME)

    init {
        if (!dataDir.toFile().exists()) {
            dataDir.toFile().mkdir()
        }
        @Suppress("UNCHECKED_CAST")
        paramsType = this::class.supertypes.first().arguments.first().type!!.classifier as KClass<T>
    }

    final override fun execute(ctx: ActionContext) {
        val params = ctx.parseParams(paramsType)
        val server = params.server

        val sc = SSHClient()

        var timeoutTask: TimerTask? = null

        if (params.timeout > 0) {
            val thread = Thread.currentThread()
            timeoutTask = object : TimerTask() {
                override fun run() {
                    sc.close()
                    thread.interrupt()
                }
            }
            timer.schedule(timeoutTask, (params.timeout * 1000).toLong())
        }

        sc.addHostKeyVerifier(FirstHostKeyVerifier(dataDir.resolve(KNOWN_HOSTS_FILE).toFile()))
        sc.connect(server.ip!!, server.port!!)
        sc.connection.keepAlive.keepAliveInterval = params.keepAlive

        try {
            when (server.authMethod!!) {
                AuthMethod.PASSWORD -> {
                    sc.authPassword(server.user!!, String(enc.decrypt(server.password!!)))
                }
                AuthMethod.PUBLIC_KEY -> {
                    sc.authPublickey(
                        server.user!!,
                        RSAKeyProvider(String(enc.decrypt(server.privateKey!!)))
                    )
                }
            }
            execute(ctx, sc, params)
        } finally {
            timeoutTask?.cancel()
            sc.close()
        }
    }

    protected abstract fun execute(ctx: ActionContext, ssh: SSHClient, params: T)
}

abstract class SSHActionParams : ActionParams {
    @ActionParam("服务器", "", VariableType.SERVER)
    lateinit var server: Server

    @ActionParam("超时时间", "单位：秒，小于等于 0 即为永不超时", VariableType.NUMBER, false)
    var timeout: Int = 0

    @ActionParam("连接心跳", "单位：秒，小于等于 0 即为不发送心跳", VariableType.NUMBER, false)
    var keepAlive: Int = 0
}

class FirstHostKeyVerifier(f: File) : OpenSSHKnownHosts(f) {
    override fun hostKeyUnverifiableAction(hostname: String?, key: PublicKey?): Boolean {
        val type = KeyType.fromKey(key)
        log.warn("Warning: Permanently added '$hostname' ($type) to the list of known hosts.")
        synchronized(this) {
            entries().add(HostEntry(null, hostname, type, key))
            write()
        }
        return true
    }
}

class RSAKeyProvider(privateKeyStr: String) : KeyProvider {
    companion object {
        private val KEY_FACTORY = KeyFactory.getInstance("RSA")
    }

    private val privateKey = privateKeyStr.let {
        val privateKeySpec = PKCS8EncodedKeySpec(
            Base64.getDecoder().decode(
                it.trim()
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\n", "")
            )
        )
        KEY_FACTORY.generatePrivate(privateKeySpec) as RSAPrivateCrtKey
    }

    private val publicKey = privateKey.let {
        val publicKeySpec = RSAPublicKeySpec(
            it.modulus,
            it.publicExponent
        )
        KEY_FACTORY.generatePublic(publicKeySpec) as RSAPublicKey
    }

    override fun getPrivate(): PrivateKey = privateKey
    override fun getPublic(): PublicKey = publicKey
    override fun getType(): KeyType = KeyType.RSA
}
