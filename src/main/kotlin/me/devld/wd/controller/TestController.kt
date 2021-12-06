package me.devld.wd.controller

import me.devld.wd.config.AppConfig
import me.devld.wd.config.security.DataEncryption
import me.devld.wd.data.DeploymentTriggerIn
import me.devld.wd.data.Project
import me.devld.wd.service.ProjectService
import me.devld.wd.service.deploy.DeploymentService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

/**
 * TestController
 *
 * @author devld
 */
@RestController
@RequestMapping("/test")
class TestController(
    private val appConfig: AppConfig,
    private val projectService: ProjectService,
    private val deploymentService: DeploymentService,
    private val enc: DataEncryption,
) {

    @GetMapping
    fun test(): String {
        return appConfig.dataDir
    }

    @GetMapping("/project/{name}")
    fun getProject(@PathVariable name: String): Project? = projectService.getProject(name)

    @PostMapping("/deploy")
    fun triggerDeployment(@Valid @RequestBody data: DeploymentTriggerIn) {
        deploymentService.triggerDeployment(data)
    }

    @GetMapping("/encrypt")
    fun encrypt(@RequestParam s: String): String {
        return enc.encryptToString(s.toByteArray())
    }

    @GetMapping("/decrypt")
    fun decrypt(@RequestParam e: String): String {
        return String(enc.decrypt(e))
    }

}
