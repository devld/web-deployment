package me.devld.wd.config.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.token.KeyBasedPersistenceTokenService
import org.springframework.security.core.token.Token
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.util.StringUtils
import java.security.SecureRandom
import javax.servlet.http.HttpServletRequest

class JwtTokenAuthenticationFilter(authenticationManager: AuthenticationManager) :
    BaseTokenAuthenticationFilter(authenticationManager) {

    private fun getToken(req: HttpServletRequest): String? {
        var token = req.getHeader("Authorization")
        if (!StringUtils.hasText(token)) {
            token = req.getParameter("access_token")
        }
        return if (StringUtils.hasText(token)) token.trim() else null
    }

    override fun obtainAuthentication(request: HttpServletRequest): Authentication? =
        getToken(request)?.let { JwtTokenAuthentication(it) }
}

class JwtTokenAuthentication(private val principal: Any?, private val token: String?) :
    AbstractAuthenticationToken(emptyList()) {

    constructor(principal: Any?) : this(principal, null) {
        isAuthenticated = true
    }

    constructor(token: String?) : this(null, token)

    override fun getCredentials(): Any? = token
    override fun getPrincipal(): Any? = principal

}

class JwtAuthenticationProvider(userDetailsService: UserDetailsService) :
    BaseTokenAuthenticationProvider(userDetailsService) {

    override fun obtainUsername(authentication: Authentication): String = ""

    override fun createAuthentication(userDetails: UserDetails): Authentication =
        JwtTokenAuthentication(userDetails)

    override fun supports(aClass: Class<*>): Boolean = JwtTokenAuthentication::class.java.isAssignableFrom(aClass)

}


class JwtTokenService(
    serverSecret: String?,
    serverInteger: Int,
    secureRandom: SecureRandom?,
    private val validity: Long
) : KeyBasedPersistenceTokenService() {

    private val log = LoggerFactory.getLogger(this.javaClass)

    init {
        setServerSecret(serverSecret)
        setServerInteger(serverInteger)
        setSecureRandom(secureRandom)
    }

    override fun verifyToken(key: String): Token? {
        val token: Token = try {
            super.verifyToken(key)
        } catch (e: IllegalArgumentException) {
            log.debug("illegal token received.", e)
            return null
        }
        val creation = token.keyCreationTime
        if (creation + validity < System.currentTimeMillis()) {
            // expired
            return null
        }
        return token
    }
}
