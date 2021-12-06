package me.devld.wd.service.deploy.ssh

import me.devld.wd.config.AppConfig
import me.devld.wd.config.security.DataEncryption
import me.devld.wd.data.Artifact
import me.devld.wd.data.VariableType
import me.devld.wd.service.ArtifactService
import me.devld.wd.service.deploy.ActionContext
import me.devld.wd.service.deploy.ActionParam
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.xfer.LocalFileFilter
import net.schmizz.sshj.xfer.LocalSourceFile
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.*

@Component
class SSHUploadAction(
    private val artifactService: ArtifactService,
    config: AppConfig, enc: DataEncryption, timer: Timer
) : SSHAction<SSHUploadActionParams>(config, enc, timer) {
    override val name = "ssh-upload"

    companion object {
        const val VALUE_FILES = "SSH_UPLOAD_FILES"
    }

    override fun execute(ctx: ActionContext, ssh: SSHClient, params: SSHUploadActionParams) {
        val artifacts = ctx.getProjectVersion().artifacts!!
        if (artifacts.isEmpty()) {
            log.warn("version ${ctx.getProject().name}:${ctx.getProjectVersion().name} has no artifacts, skip upload")
            return
        }
        val dest = params.destination!!.trim().let {
            if (it.isEmpty() || it.endsWith("/")) it else "$it/"
        }
        val includes = if (params.includes.isNullOrEmpty()) null else Regex(params.includes!!)
        val excludes = if (params.excludes.isNullOrEmpty()) null else Regex(params.excludes!!)

        ssh.newSFTPClient().use { sftp ->
            val stat = sftp.statExistence(dest)
            var exists = true
            if (stat == null) {
                exists = false
                sftp.mkdirs(dest)
            }
            val uploaded = artifacts.filter {
                includes?.matches(it.name!!) ?: true &&
                        !(excludes?.matches(it.name!!) ?: false)
            }.map {
                val destFile = "$dest${it.name}"
                if (!params.override) {
                    if (exists && sftp.statExistence(destFile) != null) {
                        throw IllegalStateException("file $destFile already exists")
                    }
                }
                sftp.put(ArtifactSourceFile(it), destFile)
                destFile
            }
            ctx.setValue(VALUE_FILES, uploaded.joinToString(" ") { "\"$it\"" })
        }
    }

    private inner class ArtifactSourceFile(private val artifact: Artifact) : LocalSourceFile {
        override fun getName(): String = artifact.name!!
        override fun getLength(): Long = artifact.size!!
        override fun getInputStream(): InputStream = artifactService.getFile(artifact.id!!).inputStream()
        override fun getPermissions(): Int = "0644".toInt(8)
        override fun isFile(): Boolean = true
        override fun isDirectory(): Boolean = false
        override fun getChildren(filter: LocalFileFilter?): MutableIterable<LocalSourceFile> =
            throw UnsupportedOperationException()

        override fun providesAtimeMtime(): Boolean = true
        override fun getLastAccessTime(): Long = artifact.createdAt!! / 1000
        override fun getLastModifiedTime(): Long = artifact.createdAt!! / 1000
    }
}

class SSHUploadActionParams : SSHActionParams() {
    @ActionParam("传输到", "目的目录，如果该目录不存在将会被创建，默认为当前用户 HOME", VariableType.STRING, false)
    var destination: String? = null

    @ActionParam("是否覆盖文件", "", VariableType.BOOLEAN, false)
    var override: Boolean = false

    @ActionParam("包含的文件", "文件名正则表达式", VariableType.STRING, false)
    var includes: String? = null

    @ActionParam("排除的文件", "文件名正则表达式", VariableType.STRING, false)
    var excludes: String? = null
}

