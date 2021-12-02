package me.devld.wd.service

import me.devld.wd.data.NotFoundException
import me.devld.wd.data.User
import me.devld.wd.repository.UserRepository
import org.springframework.stereotype.Service

/**
 * UserService
 *
 * @author devld
 */
@Service
class UserService(private val userRepo: UserRepository) {

    fun findUserByUsername(username: String): User {
        return userRepo.findByUsername(username) ?: throw NotFoundException("User not found")
    }

}
