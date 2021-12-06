package me.devld.wd.service

import me.devld.wd.data.Project
import me.devld.wd.data.ProjectVersion
import me.devld.wd.repository.ProjectRepository
import me.devld.wd.repository.ProjectVersionRepository
import org.springframework.stereotype.Service

/**
 * ProjectService
 *
 * @author devld
 */
@Service
class ProjectService(
    private val projectRepo: ProjectRepository,
    private val projectVersionRepo: ProjectVersionRepository,
) {

    fun getProjectCurrentVersion(projectId: Long): ProjectVersion? {
        // TODO
        return null
    }

    fun getProjectVersion(projectId: Long, version: String): ProjectVersion? =
        projectVersionRepo.findByProjectIdAndName(projectId, version)

    fun getProjectLatestVersion(projectId: Long): ProjectVersion? =
        projectVersionRepo.findTopByProjectIdOrderByVersionDesc(projectId)

    fun getProject(name: String): Project? = projectRepo.findByName(name)

    fun getProject(id: Long): Project? = projectRepo.findById(id).orElse(null)

    fun getProjectVersion(id: Long): ProjectVersion? = projectVersionRepo.findById(id).orElse(null)

}
