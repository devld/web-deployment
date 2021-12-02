package me.devld.wd.data

import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @JsonIgnore
    @Version
    open var v: Long? = null
}

@Entity
open class User : BaseEntity() {
    @Column(nullable = false, unique = true, length = 16)
    open var username: String? = null

    @Column(nullable = false, length = 64)
    open var password: String? = null
}

@Entity
open class Variable : BaseEntity() {
    @Column(nullable = false, unique = true, length = 32)
    open var name: String? = null

    @Column(nullable = false, length = 32)
    open var type: VariableType? = null

    @Column(nullable = false, length = 4096)
    open var value: String? = null
}

enum class VariableType {
    STRING,
    NUMBER,
    BOOLEAN,
    SERVER,
}

@Entity
open class Server : BaseEntity() {
    @Column(nullable = false, unique = true, length = 32)
    open var name: String? = null

    @Column(length = 1024)
    open var description: String? = null

    @Column(nullable = false, length = 64)
    open var ip: String? = null
    open var port: Int? = null

    @Column(nullable = false, length = 32)
    open var user: String? = null

    @Column(nullable = false, length = 16)
    open var authMethod: AuthMethod? = null

    @Column(length = 128)
    open var password: String? = null
    open var privateKey: String? = null
}

enum class AuthMethod {
    PASSWORD,
    PUBLIC_KEY,
}

@Entity
open class Project : BaseEntity() {
    @Column(nullable = false, unique = true, length = 32)
    open var name: String? = null

    @Column(length = 1024)
    open var description: String? = null
}

@Entity
open class ProjectVersion : BaseEntity() {

    @Column(nullable = false, unique = true, length = 32)
    open var name: String? = null

    @Column(nullable = false)
    open var version: Int? = null

    @Column(nullable = false)
    open var projectId: Long? = null

    @OneToMany(targetEntity = Artifact::class)
    @JoinColumn(name = "PROJECT_VERSION_ID")
    open var artifacts: List<Artifact>? = null
}

@Entity
open class Artifact : BaseEntity() {
    @Column(nullable = false, length = 128)
    open var name: String? = null

    @Column(nullable = false)
    open var size: Long? = null

    @Column(nullable = false, length = 32)
    open var type: ArtifactType? = null

    @Column(nullable = false, length = 512)
    open var file: String? = null
}

enum class ArtifactType {
    JAR,
    TAR_GZ,
    ZIP,
    ANY
}

@Entity
open class Deployment : BaseEntity() {
    @Column(length = 1024)
    open var description: String? = null

    @ManyToOne
    open var project: Project? = null

    @OneToMany(mappedBy = "deployment", targetEntity = DeploymentStep::class, fetch = FetchType.EAGER)
    @OrderBy("stepOrder")
    open var steps: List<DeploymentStep>? = null
}

@Entity
open class DeploymentStep : BaseEntity() {
    @ManyToOne
    open var deployment: Deployment? = null

    @Column(nullable = false)
    open var async: Boolean = false

    @Column(nullable = false)
    open var ignoreOnFailure: Boolean = false

    @Column(nullable = false)
    open var stepOrder: Int? = null

    @Column(nullable = false)
    open var action: String? = null

    @Column(nullable = false, length = 4096)
    open var actionParams: String? = null
}

@Entity
@Table(indexes = [Index(columnList = "deploymentId")])
open class DeploymentTask : BaseEntity() {
    @Column(nullable = false)
    open var deploymentId: Long? = null

    @ManyToOne
    open var deploymentStep: DeploymentStep? = null

    @Column(nullable = true, length = 4096)
    open var runtimeData: String? = null

    @Column(nullable = false)
    open var status: DeploymentTaskStatus? = null

    @Column(nullable = false, length = 512)
    @Convert(converter = LongListConverter::class)
    open var waitingFor: MutableList<Long>? = null

    @Column(nullable = false, length = 512)
    @Convert(converter = LongListConverter::class)
    open var wakeupTasks: MutableList<Long>? = null

    @Column(nullable = false)
    open var taskOrder: Int? = null

    @Column(nullable = false)
    open var createdAt: Long? = null

    @Column(nullable = true)
    open var startedAt: Long? = null

    @Column(nullable = true)
    open var finishedAt: Long? = null
}

enum class DeploymentTaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILURE,
    CANCELED;

    val finished
        get() = this == SUCCESS || this == FAILURE || this == CANCELED
}
