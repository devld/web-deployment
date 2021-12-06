package me.devld.wd.service

import me.devld.wd.config.AppConfig
import me.devld.wd.data.Artifact
import me.devld.wd.data.ArtifactType
import me.devld.wd.data.NotFoundException
import me.devld.wd.repository.ArtifactRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Paths

/**
 * ArtifactService
 *
 * @author devld
 */
@Service
class ArtifactService(private val artifactRepo: ArtifactRepository, config: AppConfig) {

    companion object {
        private const val DIR_NAME = "artifacts"
    }

    private val dir = Paths.get(config.dataDir, DIR_NAME)

    init {
        if (!dir.toFile().exists()) {
            dir.toFile().mkdir()
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun upload(name: String, type: ArtifactType, file: MultipartFile): Artifact {
        val artifact = artifactRepo.save(Artifact().apply {
            this.name = name
            this.size = file.size
            this.type = type
            this.createdAt = System.currentTimeMillis()
        })
        file.transferTo(dir.resolve(artifact.id.toString()))
        return artifact
    }

    fun getArtifact(id: Long): Artifact? {
        return artifactRepo.findById(id).orElseThrow {
            NotFoundException("Artifact not found")
        }
    }

    fun getFile(id: Long): File {
        return dir.resolve(id.toString()).toFile().also {
            if (!it.exists()) {
                throw NotFoundException("Artifact not found")
            }
        }
    }

}
