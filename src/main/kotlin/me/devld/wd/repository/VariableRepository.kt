package me.devld.wd.repository

import me.devld.wd.data.Variable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * VariableRepository
 *
 * @author devld
 */
@Repository
interface VariableRepository : JpaRepository<Variable, Long> {

    fun findByName(name: String): Variable?

}
