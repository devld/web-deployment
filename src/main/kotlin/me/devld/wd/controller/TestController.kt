package me.devld.wd.controller

import me.devld.wd.config.AppConfig
import me.devld.wd.data.Project
import me.devld.wd.deploy.DeploymentService
import me.devld.wd.service.ProjectService
import org.springframework.web.bind.annotation.*

/**
 * TestController
 *
 * @author devld
 */
@RestController
class TestController(
    private val appConfig: AppConfig,
    private val projectService: ProjectService,
    private val deploymentService: DeploymentService
) {

    @GetMapping("/test")
    fun test(): String {
        return appConfig.dataDir
    }

    @GetMapping("/test/project/{name}")
    fun getProject(@PathVariable name: String): Project? = projectService.getProject(name)

    @PostMapping("/test/deployment/{id}")
    fun triggerDeployment(
        @PathVariable id: Long,
        @RequestParam("version", required = false) version: String?
    ) {
        deploymentService.triggerDeployment(id, version)
    }

}
