package io.openrise.stegorouter.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class KeyDerivationTest {
    @Test
    fun `generate salt produces 16 bytes`() {
        val salt = KeyDerivation.generateSalt()
        assertEquals(16, salt.size)
    }

    @Test
    fun `generate salt produces unique values`() {
        val salt1 = KeyDerivation.generateSalt()
        val salt2 = KeyDerivation.generateSalt()
        assertFalse(salt1.contentEquals(salt2))
    }

    @Test
    fun `derive key produces 32 bytes`() {
        val password = "test-password"
        val salt = KeyDerivation.generateSalt()
        val key = KeyDerivation.deriveKey(password, salt)
        assertEquals(32, key.encoded.size)
    }

    @Test
    fun `derive key with same password and salt produces same key`() {
        val password = "test-password"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val key1 = KeyDerivation.deriveKey(password, salt)
        val key2 = KeyDerivation.deriveKey(password, salt)
        assertContentEquals(key1.encoded, key2.encoded)
    }

    @Test
    fun `derive key with different passwords produces different keys`() {
        val salt = KeyDerivation.generateSalt()
        val key1 = KeyDerivation.deriveKey("password1", salt)
        val key2 = KeyDerivation.deriveKey("password2", salt)
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }

    @Test
    fun `derive key with different salts produces different keys`() {
        val password = "test-password"
        val salt1 = KeyDerivation.generateSalt()
        val salt2 = KeyDerivation.generateSalt()
        val key1 = KeyDerivation.deriveKey(password, salt1)
        val key2 = KeyDerivation.deriveKey(password, salt2)
        assertFalse(key1.encoded.contentEquals(key2.encoded))
    }
}
