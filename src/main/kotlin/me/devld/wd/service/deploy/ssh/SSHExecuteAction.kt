package me.devld.wd.service.deploy.ssh

import me.devld.wd.config.AppConfig
import me.devld.wd.config.security.DataEncryption
import me.devld.wd.data.VariableType
import me.devld.wd.service.deploy.ActionContext
import me.devld.wd.service.deploy.ActionParam
import me.devld.wd.service.deploy.LogLevel
import net.schmizz.sshj.SSHClient
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.*

/**
 * SSHExecuteAction
 *
 * @author devld
 */
@Component
class SSHExecuteAction(
    config: AppConfig,
    enc: DataEncryption,
    timer: Timer
) : SSHAction<SSHExecuteActionParams>(config, enc, timer) {
    override val name = "ssh"

    override fun execute(ctx: ActionContext, ssh: SSHClient, params: SSHExecuteActionParams) {
        ssh.startSession().use { session ->

            val envCmd = params.env.split("\n").filter { it.isNotEmpty() }
                .joinToString("\n") { "export $it" }
            val c = session.exec(envCmd + "\n" + params.script)
            var sc1: Thread? = null
            var sc2: Thread? = null
            try {
                if (!params.silence) {
                    sc1 = StreamConsumer(c.inputStream) {
                        ctx.log(LogLevel.INFO, it)
                    }.apply { start() }
                }
                sc2 = StreamConsumer(c.errorStream) {
                    ctx.log(LogLevel.ERROR, it)
                }.apply { start() }

                c.join()
            } finally {
                sc1?.interrupt()
                sc2?.interrupt()
            }
        }
    }

    private class StreamConsumer(private val s: InputStream, private val write: (String) -> Unit) :
        Thread("SSHExecuteActionStreamConsumer") {
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

class SSHExecuteActionParams : SSHActionParams() {
    @ActionParam("环境变量", "执行脚本前设置的环境变量，一行一个。如 FOO=BAR", VariableType.STRING, false)
    var env: String = ""

    @ActionParam("脚本", "要执行的脚本内容", VariableType.STRING)
    var script: String = ""

    @ActionParam("忽略 stdout", "是否忽略 stdout", VariableType.BOOLEAN, false)
    var silence: Boolean = false
}
