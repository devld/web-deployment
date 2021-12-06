package me.devld.wd.config.security

import me.devld.wd.config.AppConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Paths
import java.util.*
import javax.crypto.*
import javax.crypto.spec.SecretKeySpec

/**
 * DataEncryption
 *
 * @author devld
 */
@Component
class DataEncryption(config: AppConfig) {

    companion object {
        private const val KEY_FILE_NAME = "enc.key"
    }

    private val log = LoggerFactory.getLogger(DataEncryption::class.java)

    private val keyFile = Paths.get(config.dataDir, KEY_FILE_NAME).toFile()

    private val key: SecretKey

    init {
        if (!keyFile.exists()) {
            log.info("Generating new key file...")
            generateKey()
        }
        key = SecretKeySpec(keyFile.readBytes(), "AES")
    }

    private fun generateKey() {
        if (keyFile.exists()) {
            throw IllegalStateException("Key file already exists")
        }
        keyFile.createNewFile()
        val key = KeyGenerator.getInstance("AES").let {
            it.init(128)
            it.generateKey().encoded
        }
        keyFile.writeBytes(key)
    }

    /**
     * aes encrypt
     */
    fun encrypt(bytes: ByteArray): ByteArray {
        try {
            val c = Cipher.getInstance("AES")
            c.init(Cipher.ENCRYPT_MODE, key)
            return c.doFinal(bytes)
        } catch (e: Exception) {
            when (e) {
                is IllegalBlockSizeException, is BadPaddingException
                -> throw IllegalArgumentException("invalid input", e)
                else -> throw e
            }
        }
    }

    fun decrypt(bytes: ByteArray): ByteArray {
        try {
            val c = Cipher.getInstance("AES")
            c.init(Cipher.DECRYPT_MODE, key)
            return c.doFinal(bytes)
        } catch (e: Exception) {
            when (e) {
                is IllegalBlockSizeException, is BadPaddingException
                -> throw IllegalArgumentException("invalid input", e)
                else -> throw e
            }
        }
    }

    fun encryptToString(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(encrypt(bytes))
    }

    fun decrypt(enc: String): ByteArray {
        return decrypt(Base64.getDecoder().decode(enc.toByteArray()))
    }

}
