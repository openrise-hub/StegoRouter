package io.openrise.stegorouter.payload

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class PayloadProcessorTest {
    @Test
    fun `prepare and recover empty data`() {
        val data = ByteArray(0)
        val password = "test-password"
        val prepared = PayloadProcessor.prepare(data, password)
        val recovered = PayloadProcessor.recover(prepared, password)
        assertContentEquals(data, recovered)
    }

    @Test
    fun `prepare and recover text data`() {
        val data = "Hello, PayloadProcessor!".toByteArray()
        val password = "test-password"
        val prepared = PayloadProcessor.prepare(data, password)
        val recovered = PayloadProcessor.recover(prepared, password)
        assertContentEquals(data, recovered)
    }

    @Test
    fun `prepare and recover binary data`() {
        val data = ByteArray(1024) { it.toByte() }
        val password = "test-password"
        val prepared = PayloadProcessor.prepare(data, password)
        val recovered = PayloadProcessor.recover(prepared, password)
        assertContentEquals(data, recovered)
    }

    @Test
    fun `prepare and recover large payload`() {
        val data = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val password = "test-password"
        val prepared = PayloadProcessor.prepare(data, password)
        val recovered = PayloadProcessor.recover(prepared, password)
        assertContentEquals(data, recovered)
    }

    @Test
    fun `recover with wrong password fails`() {
        val data = "test".toByteArray()
        val prepared = PayloadProcessor.prepare(data, "password1")
        assertFailsWith<Exception> {
            PayloadProcessor.recover(prepared, "password2")
        }
    }

    @Test
    fun `recover invalid data fails`() {
        val invalidData = ByteArray(10)
        assertFailsWith<Exception> {
            PayloadProcessor.recover(invalidData, "password")
        }
    }

    @Test
    fun `prepare includes salt prefix`() {
        val data = "test".toByteArray()
        val password = "test-password"
        val prepared = PayloadProcessor.prepare(data, password)
        assert(prepared.size > 16)
    }

    @Test
    fun `multiple preparations produce different outputs`() {
        val data = "test".toByteArray()
        val password = "test-password"
        val prepared1 = PayloadProcessor.prepare(data, password)
        val prepared2 = PayloadProcessor.prepare(data, password)
        assert(!prepared1.contentEquals(prepared2))
    }
}
