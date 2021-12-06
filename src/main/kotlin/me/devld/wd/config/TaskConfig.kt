package me.devld.wd.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

/**
 * TaskConfig
 *
 * @author devld
 */
@Configuration
class TaskConfig {

    @Bean(destroyMethod = "cancel")
    fun timer(): Timer = Timer("task-timer")

}
