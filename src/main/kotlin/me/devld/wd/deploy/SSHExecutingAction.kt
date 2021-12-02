package me.devld.wd.deploy

import me.devld.wd.config.AppConfig
import me.devld.wd.data.AuthMethod
import me.devld.wd.data.Server
import me.devld.wd.data.VariableType
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * SSHExecutingAction
 *
 * @author devld
 */
@Component
class SSHExecutingAction(config: AppConfig) : Action {
    companion object {
        private const val SSH_DIR_NAME = "ssh"
        private const val KNOWN_HOSTS_FILE = "known_hosts"
        private const val DEFAULT_TIMEOUT: Long = 120000 // 120 seconds
    }

    override val name = "ssh-executing"

    private val dataDir = Paths.get(config.dataDir, SSH_DIR_NAME)

    init {
        if (!dataDir.toFile().exists()) {
            dataDir.toFile().mkdir()
        }
    }

    override fun execute(ctx: ActionContext) {
        val params = ctx.parseParams(SSHExecutingActionParams::class)

        val server = params.server

        val sc = SSHClient()
        sc.addHostKeyVerifier(FirstHostKeyVerifier(dataDir.resolve(KNOWN_HOSTS_FILE).toFile()))
        sc.connect(server.ip!!, server.port!!)

        var session: Session? = null

        try {
            when (server.authMethod!!) {
                AuthMethod.PASSWORD -> {
                    sc.authPassword(server.user!!, server.password!!)
                }
                AuthMethod.PUBLIC_KEY -> {
                    sc.authPublickey(server.user!!, RSAKeyProvider(server.privateKey!!))
                }
            }
            session = sc.startSession()
            start(ctx, session, params)
        } finally {
            session?.close()
            sc.disconnect()
        }
    }

    private fun start(ctx: ActionContext, session: Session, params: SSHExecutingActionParams) {
        params.env.split("\n").filter { it.isNotEmpty() }.forEach {
            val env = it.split(Regex("\n"), 2)
            session.setEnvVar(env[0], if (env.size > 1) env[1] else "")
        }
        val c = session.exec(params.script)
        var sc1: Thread? = null
        var sc2: Thread? = null
        try {
            if (!params.silence) {
                sc1 = StreamConsumer(c.inputStream) {
                    ctx.log(LogLevel.INFO, it)
                }.also { it.start() }
            }
            sc2 = StreamConsumer(c.errorStream) {
                ctx.log(LogLevel.ERROR, it)
            }.also { it.start() }
            c.join(max(DEFAULT_TIMEOUT, params.timeout), TimeUnit.MILLISECONDS)
        } finally {
            sc1?.interrupt()
            sc2?.interrupt()
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

    class StreamConsumer(private val s: InputStream, private val write: (String) -> Unit) :
        Thread("SSHExecutingAction\$StreamConsumer") {
        override fun run() {
            s.bufferedReader().use {
                while (true) {
                    if (currentThread().isInterrupted) break
                    try {
                        val line = it.readLine() ?: break
                        write(line)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        }
    }
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

class SSHExecutingActionParams : ActionParams {
    @ActionParam("服务器", "", VariableType.SERVER)
    lateinit var server: Server

    @ActionParam("环境变量", "执行脚本前设置的环境变量，一行一个。如 FOO=BAR", VariableType.STRING, false)
    var env: String = ""

    @ActionParam("脚本", "要执行的脚本内容", VariableType.STRING)
    var script: String = ""

    @ActionParam("忽略 stdout", "是否忽略 stdout", VariableType.BOOLEAN, false)
    var silence: Boolean = false

    @ActionParam("超时时间", "超时时间，单位：毫秒", VariableType.NUMBER, false)
    var timeout: Long = 0
}
