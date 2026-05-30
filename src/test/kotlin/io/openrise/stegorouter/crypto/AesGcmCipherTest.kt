package io.openrise.stegorouter.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AesGcmCipherTest {
    @Test
    fun `encrypt and decrypt empty data`() {
        val data = ByteArray(0)
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)
        assertContentEquals(data, decrypted)
    }

    @Test
    fun `encrypt and decrypt text data`() {
        val data = "Hello, AES-GCM!".toByteArray()
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)
        assertContentEquals(data, decrypted)
    }

    @Test
    fun `encrypt and decrypt binary data`() {
        val data = ByteArray(1024) { it.toByte() }
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)
        assertContentEquals(data, decrypted)
    }

    @Test
    fun `encrypt and decrypt large payload`() {
        val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        val decrypted = AesGcmCipher.decrypt(encrypted, key)
        assertContentEquals(data, decrypted)
    }

    @Test
    fun `encrypted data includes IV`() {
        val data = "test".toByteArray()
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        assert(encrypted.size > 12)
    }

    @Test
    fun `multiple encryptions produce different ciphertexts`() {
        val data = "test".toByteArray()
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted1 = AesGcmCipher.encrypt(data, key)
        val encrypted2 = AesGcmCipher.encrypt(data, key)
        assertFalse(encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun `decrypt with wrong key fails`() {
        val data = "test".toByteArray()
        val key1 = KeyDerivation.deriveKey("password1", KeyDerivation.generateSalt())
        val key2 = KeyDerivation.deriveKey("password2", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key1)
        assertFailsWith<Exception> {
            AesGcmCipher.decrypt(encrypted, key2)
        }
    }

    @Test
    fun `decrypt tampered data fails`() {
        val data = "test".toByteArray()
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val encrypted = AesGcmCipher.encrypt(data, key)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0xFF).toByte()
        assertFailsWith<Exception> {
            AesGcmCipher.decrypt(encrypted, key)
        }
    }

    @Test
    fun `decrypt invalid data fails`() {
        val key = KeyDerivation.deriveKey("password", KeyDerivation.generateSalt())
        val invalidData = ByteArray(10)
        assertFailsWith<Exception> {
            AesGcmCipher.decrypt(invalidData, key)
        }
    }
}
