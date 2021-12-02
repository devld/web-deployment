package me.devld.wd.config

import com.fasterxml.jackson.databind.ObjectMapper
import me.devld.wd.data.BaseException
import me.devld.wd.data.Response
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.servlet.http.HttpServletResponse

/**
 * ExceptionHandler
 *
 * @author devld
 */
@RestController
@RestControllerAdvice
class ExceptionHandler(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(AuthenticationException::class)
    fun onAuthenticationException(e: AuthenticationException): Response<Any?> {
        log.debug("unauthorized", e)
        return Response(null, HttpStatus.UNAUTHORIZED, "unauthorized")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun onAccessDeniedException(e: AccessDeniedException): Response<Any?> {
        log.debug("access denied", e)
        return Response(null, HttpStatus.FORBIDDEN, "access denied")
    }

    @ExceptionHandler(BaseException::class)
    fun onBaseException(e: BaseException, response: HttpServletResponse): Response<Any?> =
        Response(null, e.status, e.message)

    fun writeResponse(response: Response<Any?>, resp: HttpServletResponse) {
        resp.status = response.status.value()
        resp.setHeader("Content-Type", "application/json;charset=UTF-8")
        resp.writer.write(objectMapper.writeValueAsString(response))
    }

}
