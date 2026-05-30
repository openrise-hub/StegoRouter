package io.openrise.stegorouter.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

object KeyDerivation {
    private const val ITERATION_COUNT = 210000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    private val secureRandom = SecureRandom()

    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val generator = PKCS5S2ParametersGenerator(SHA256Digest())
        generator.init(password.toByteArray(Charsets.UTF_8), salt, ITERATION_COUNT)
        val keyBytes = (generator.generateDerivedParameters(KEY_LENGTH_BITS) as KeyParameter).key
        return SecretKeySpec(keyBytes, "AES")
    }
}
