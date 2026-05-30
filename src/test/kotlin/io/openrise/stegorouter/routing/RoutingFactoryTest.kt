package io.openrise.stegorouter.routing

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RoutingFactoryTest {
    @Test
    fun `register and resolve algorithm`() {
        val factory = RoutingFactory()
        val algorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
        }
        factory.register(algorithm)

        val carrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertSame(algorithm, factory.resolve(carrier))
    }

    @Test
    fun `resolve throws for unregistered file type`() {
        val factory = RoutingFactory()
        val carrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertFailsWith<UnsupportedFileTypeException> {
            factory.resolve(carrier)
        }
    }

    @Test
    fun `resolve throws for unknown file type`() {
        val factory = RoutingFactory()
        val carrier = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        assertFailsWith<UnsupportedFileTypeException> {
            factory.resolve(carrier)
        }
    }

    @Test
    fun `register multiple algorithms`() {
        val factory = RoutingFactory()
        val pngAlgorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
        }
        val bmpAlgorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.BMP
        }
        factory.register(pngAlgorithm)
        factory.register(bmpAlgorithm)

        val pngCarrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        val bmpCarrier = byteArrayOf(0x42, 0x4D, 0x00, 0x00, 0x00, 0x00)

        assertSame(pngAlgorithm, factory.resolve(pngCarrier))
        assertSame(bmpAlgorithm, factory.resolve(bmpCarrier))
    }

    @Test
    fun `register overwrites existing algorithm`() {
        val factory = RoutingFactory()
        val algorithm1 = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
        }
        val algorithm2 = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
        }
        factory.register(algorithm1)
        factory.register(algorithm2)

        val carrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertSame(algorithm2, factory.resolve(carrier))
    }
}
