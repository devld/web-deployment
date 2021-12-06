package me.devld.wd.data

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Null

class UserDto(private val user: User) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    @JsonIgnore
    override fun getPassword(): String? = user.password

    override fun getUsername(): String = user.username!!

    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

}

class DeploymentTriggerIn(
    @field:NotNull
    var deploymentId: Long? = null,
    var projectVersion: String? = null,
    @field:NotBlank
    var message: String? = null,
    @field:Null
    var triggerBy: String? = null
)


class DeploymentOut(var id: Long, var description: String?, steps: List<DeploymentStepOut>) {
    constructor(deployment: Deployment) : this(
        deployment.id!!,
        deployment.description,
        deployment.steps!!.map { DeploymentStepOut(it) })
}

class DeploymentStepOut(
    var id: Long,
    var deploymentId: Long,
    var async: Boolean,
    var ignoreOnFailure: Boolean,
    var stepOrder: Int,
    var action: String,
    var actionParams: String?
) {
    constructor(step: DeploymentStep) : this(
        step.id!!,
        step.deployment!!.id!!,
        step.async,
        step.ignoreOnFailure,
        step.stepOrder!!,
        step.action!!,
        step.actionParams
    )
}


