package io.openrise.stegorouter.crypto

import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.GCMBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

object AesGcmCipher {
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    fun encrypt(data: ByteArray, key: SecretKeySpec): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        val parameters = AEADParameters(KeyParameter(key.encoded), TAG_LENGTH_BITS, iv)
        cipher.init(true, parameters)

        val ciphertext = ByteArray(cipher.getOutputSize(data.size))
        var offset = cipher.processBytes(data, 0, data.size, ciphertext, 0)
        offset += cipher.doFinal(ciphertext, offset)

        return iv + ciphertext.copyOf(offset)
    }

    fun decrypt(encryptedData: ByteArray, key: SecretKeySpec): ByteArray {
        require(encryptedData.size > IV_LENGTH_BYTES) { "Invalid encrypted data" }

        val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)

        val cipher = GCMBlockCipher.newInstance(AESEngine.newInstance())
        val parameters = AEADParameters(KeyParameter(key.encoded), TAG_LENGTH_BITS, iv)
        cipher.init(false, parameters)

        val plaintext = ByteArray(cipher.getOutputSize(ciphertext.size))
        var offset = cipher.processBytes(ciphertext, 0, ciphertext.size, plaintext, 0)
        offset += cipher.doFinal(plaintext, offset)

        return plaintext.copyOf(offset)
    }
}
