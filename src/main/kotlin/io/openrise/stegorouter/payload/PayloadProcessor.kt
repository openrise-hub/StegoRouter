package io.openrise.stegorouter.payload

import io.openrise.stegorouter.compression.GzipCompressor
import io.openrise.stegorouter.crypto.AesGcmCipher
import io.openrise.stegorouter.crypto.KeyDerivation
import javax.crypto.spec.SecretKeySpec

object PayloadProcessor {
    fun prepare(data: ByteArray, password: String): ByteArray {
        val compressed = GzipCompressor.compress(data)
        val salt = KeyDerivation.generateSalt()
        val key = KeyDerivation.deriveKey(password, salt)
        val encrypted = AesGcmCipher.encrypt(compressed, key)
        return salt + encrypted
    }

    fun recover(data: ByteArray, password: String): ByteArray {
        require(data.size > 16) { "Invalid payload data" }
        
        val salt = data.copyOfRange(0, 16)
        val encrypted = data.copyOfRange(16, data.size)
        val key = KeyDerivation.deriveKey(password, salt)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)
        return GzipCompressor.decompress(decrypted)
    }
}
