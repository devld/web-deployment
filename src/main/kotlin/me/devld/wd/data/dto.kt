package me.devld.wd.data

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class Response<T>(
    val data: T,
    @JsonIgnore val status: HttpStatus = HttpStatus.OK,
    val message: String?
)

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

