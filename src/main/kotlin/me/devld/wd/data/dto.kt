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

class DeploymentTriggerData(
    @field:NotNull
    var deploymentId: Long? = null,
    var projectVersion: String? = null,
    @field:NotBlank
    var message: String? = null,
    @field:Null
    var triggerBy: String? = null
)
