package me.devld.wd.repository

import me.devld.wd.data.Server
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * ServerRepository
 *
 * @author devld
 */
@Repository
interface ServerRepository : JpaRepository<Server, Long> {
}
