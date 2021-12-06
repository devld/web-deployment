package me.devld.wd.controller

import me.devld.wd.data.DeploymentOut
import me.devld.wd.data.DeploymentTriggerIn
import me.devld.wd.service.deploy.DeploymentService
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

/**
 * DeploymentController
 *
 * @author devld
 */
@RestController
@RequestMapping("/deployment")
class DeploymentController(private val deploymentService: DeploymentService) {

    @GetMapping("/project/{projectId}")
    fun getDeploymentsByProject(@PathVariable projectId: Long): List<DeploymentOut> =
        deploymentService.getDeploymentsByProjectId(projectId)

    @PostMapping
    fun triggerDeployment(@Valid @RequestBody data: DeploymentTriggerIn) =
        deploymentService.triggerDeployment(data)

    @DeleteMapping("/{deploymentId}")
    fun deleteDeployment(@PathVariable deploymentId: Long) =
        deploymentService.deleteDeployment(deploymentId)

}
