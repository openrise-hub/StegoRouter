package io.openrise.stegorouter.capacity

import io.openrise.stegorouter.carrier.CarrierAlgorithm
import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.routing.UnsupportedFileTypeException
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CapacityCalculatorTest {
    @Test
    fun `calculate capacity delegates to registered algorithm`() {
        val calculator = CapacityCalculator()
        val algorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
            on { calculateCapacity(org.mockito.kotlin.any()) } doReturn 1024L
        }
        calculator.register(algorithm)

        val carrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertEquals(1024L, calculator.calculateCapacity(carrier))
    }

    @Test
    fun `calculate capacity throws for unregistered file type`() {
        val calculator = CapacityCalculator()
        val carrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        assertFailsWith<UnsupportedFileTypeException> {
            calculator.calculateCapacity(carrier)
        }
    }

    @Test
    fun `calculate capacity throws for unknown file type`() {
        val calculator = CapacityCalculator()
        val carrier = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05)
        assertFailsWith<UnsupportedFileTypeException> {
            calculator.calculateCapacity(carrier)
        }
    }

    @Test
    fun `register multiple algorithms`() {
        val calculator = CapacityCalculator()
        val pngAlgorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.PNG
            on { calculateCapacity(org.mockito.kotlin.any()) } doReturn 1024L
        }
        val bmpAlgorithm = mock<CarrierAlgorithm> {
            on { supportedType() } doReturn FileType.BMP
            on { calculateCapacity(org.mockito.kotlin.any()) } doReturn 2048L
        }
        calculator.register(pngAlgorithm)
        calculator.register(bmpAlgorithm)

        val pngCarrier = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00)
        val bmpCarrier = byteArrayOf(0x42, 0x4D, 0x00, 0x00, 0x00, 0x00)

        assertEquals(1024L, calculator.calculateCapacity(pngCarrier))
        assertEquals(2048L, calculator.calculateCapacity(bmpCarrier))
    }
}
