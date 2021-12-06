package me.devld.wd.data

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

abstract class BaseException(status: HttpStatus, reason: String) : ResponseStatusException(status, reason)

class NotFoundException(reason: String) : BaseException(HttpStatus.NOT_FOUND, reason)
