package me.devld.wd.repository

import me.devld.wd.data.Artifact
import me.devld.wd.data.Project
import me.devld.wd.data.ProjectVersion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProjectRepository : JpaRepository<Project, Long> {
    fun findByName(name: String): Project?
}

@Repository
interface ProjectVersionRepository : JpaRepository<ProjectVersion, Long> {

    fun findByProjectIdAndName(projectId: Long, name: String): ProjectVersion?

    fun findTopByProjectIdOrderByVersionDesc(projectId: Long): ProjectVersion?

}

@Repository
interface ArtifactRepository : JpaRepository<Artifact, Long> {
}


