package me.devld.wd.data

import org.springframework.http.HttpStatus

open class BaseException(val status: HttpStatus, message: String) : RuntimeException(message)

class NotFoundException(message: String) : BaseException(HttpStatus.NOT_FOUND, message)
