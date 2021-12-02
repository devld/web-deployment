package me.devld.wd.config.security

import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class BaseTokenAuthenticationFilter(protected val authenticationManager: AuthenticationManager) :
    OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        @NonNull httpServletRequest: HttpServletRequest, @NonNull httpServletResponse: HttpServletResponse,
        @NonNull filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication != null && authentication.isAuthenticated) {
            log.debug("already authenticated")
            filterChain.doFilter(httpServletRequest, httpServletResponse)
            return
        }
        var result: Authentication? = null
        try {
            result = attemptAuthentication(httpServletRequest, httpServletResponse)
        } catch (e: AuthenticationException) {
            log.debug("token auth failed", e)
        }
        if (result != null) {
            SecurityContextHolder.getContext().authentication = result
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse)
    }

    @Throws(AuthenticationException::class, IOException::class, ServletException::class)
    protected fun attemptAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): Authentication? {
        val authentication = obtainAuthentication(request)
        if (authentication == null) {
            log.debug("skip...")
            return null
        }
        return authenticationManager.authenticate(authentication)
    }

    /**
     * 从请求中提取 authentication
     *
     * @param request request
     * @return authentication or null
     * @throws IOException if any
     */
    @Throws(IOException::class, ServletException::class)
    protected abstract fun obtainAuthentication(request: HttpServletRequest): Authentication?
}

abstract class BaseTokenAuthenticationProvider(protected val userService: UserDetailsService) :
    AuthenticationProvider {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication? {
        val username = obtainUsername(authentication)
        if (StringUtils.hasText(username)) {
            log.debug("invalid username got")
            return null
        }
        val user = userService.loadUserByUsername(username)
        if (!user.isAccountNonLocked) {
            throw LockedException("account locked")
        }
        if (!user.isEnabled) {
            throw DisabledException("account disabled")
        }
        val result = createAuthentication(user)
        result.isAuthenticated = true
        return result
    }

    protected abstract fun obtainUsername(authentication: Authentication): String

    protected abstract fun createAuthentication(userDetails: UserDetails): Authentication
}
