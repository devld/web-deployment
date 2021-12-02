package me.devld.wd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebDeploymentApplication

fun main(args: Array<String>) {
    runApplication<WebDeploymentApplication>(*args)
}
