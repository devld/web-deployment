package me.devld.wd.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Configuration

/**
 * AppConfig
 *
 * @author devld
 */
@Configuration
@ConstructorBinding
@ConfigurationProperties(prefix = "app")
data class AppConfig(val dataDir: String = "./data")
