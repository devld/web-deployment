package me.devld.wd.config.security

import me.devld.wd.data.NotFoundException
import me.devld.wd.data.UserDto
import me.devld.wd.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * SecurityConfig
 *
 * @author devld
 */
@Configuration
class SecurityConfig : WebSecurityConfigurerAdapter() {

    private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

    override fun configure(http: HttpSecurity?) {
        http {
            csrf { disable() }
            httpBasic { disable() }
            logout { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }

            authorizeRequests {
                authorize(anyRequest, permitAll)
            }

            exceptionHandling {
                authenticationEntryPoint = authenticationEntryPoint()
                accessDeniedHandler = accessDeniedHandler()
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(JwtTokenAuthenticationFilter(authenticationManager()))

        }
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        val userDetailsService = userDetailsService()
        auth.authenticationProvider(JwtAuthenticationProvider(userDetailsService))
    }

    @Bean
    fun userDetailsService(userService: UserService) = UserDetailsService { username ->
        try {
            UserDto(userService.findUserByUsername(username))
        } catch (e: NotFoundException) {
            null
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationEntryPoint() = AuthenticationEntryPoint { _, response, e ->
        log.debug("401", e)
        response.status = 401
    }

    @Bean
    fun accessDeniedHandler() = AccessDeniedHandler { _, response, e ->
        log.debug("403", e)
        response.status = 403
    }

}


