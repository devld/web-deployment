package me.devld.wd.repository

import me.devld.wd.data.Deployment
import me.devld.wd.data.DeploymentStep
import me.devld.wd.data.DeploymentTask
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DeploymentRepository : JpaRepository<Deployment, Long> {
}

@Repository
interface DeploymentStepRepository : JpaRepository<DeploymentStep, Long> {

    fun findByDeploymentId(deploymentId: Long): List<DeploymentStep>

}

@Repository
interface DeploymentTaskRepository : JpaRepository<DeploymentTask, Long> {

    @Query("select t from DeploymentTask t where t.id in ?1")
    fun getByIds(ids: List<Long>): List<DeploymentTask>

    fun findByDeploymentId(deploymentId: Long): List<DeploymentTask>

    fun findByDeploymentIdAndDeploymentStepId(deploymentId: Long, deploymentStepId: Long): List<DeploymentTask>

}
