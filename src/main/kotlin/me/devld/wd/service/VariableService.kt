package me.devld.wd.service

import me.devld.wd.data.Variable
import me.devld.wd.repository.VariableRepository
import org.springframework.stereotype.Service

/**
 * VariableService
 *
 * @author devld
 */
@Service
class VariableService(private val variableRepo: VariableRepository) {

    fun get(name: String): String? = variableRepo.findByName(name)?.value

    fun getByName(name: String): Variable? = variableRepo.findByName(name)

}
