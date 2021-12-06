package me.devld.wd.service

import me.devld.wd.service.deploy.LogLevel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * TaskLogService
 *
 * @author devld
 */
@Service
class TaskLogService {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun log(taskId: Long, logLevel: LogLevel, msg: String) {
        val s = "[%6d] %s".format(taskId, msg)
        // TODO
        when (logLevel) {
            LogLevel.DEBUG -> log.debug(s)
            LogLevel.INFO -> log.info(s)
            LogLevel.WARN -> log.warn(s)
            LogLevel.ERROR -> log.error(s)
        }
    }

}
