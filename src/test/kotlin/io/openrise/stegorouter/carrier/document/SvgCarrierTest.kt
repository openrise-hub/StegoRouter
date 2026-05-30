package io.openrise.stegorouter.carrier.document

import io.openrise.stegorouter.carrier.FileType
import io.openrise.stegorouter.config.SvgStegoConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SvgCarrierTest {

    @Test
    fun `embed and extract with comments`() {
        val carrier = createMinimalSvg()
        val payload = "Hello, SVG!".toByteArray()
        val config = SvgStegoConfig(useComments = true, useWhitespace = false, useAttributes = false)
        val svgCarrier = SvgCarrier(config)

        val embedded = svgCarrier.embed(carrier, payload)
        val extracted = svgCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with whitespace`() {
        val carrier = createMinimalSvg()
        val payload = "Test".toByteArray()
        val config = SvgStegoConfig(useComments = false, useWhitespace = true, useAttributes = false)
        val svgCarrier = SvgCarrier(config)

        val embedded = svgCarrier.embed(carrier, payload)
        val extracted = svgCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `embed and extract with attributes`() {
        val carrier = createMinimalSvg()
        val payload = "Attr".toByteArray()
        val config = SvgStegoConfig(useComments = false, useWhitespace = false, useAttributes = true)
        val svgCarrier = SvgCarrier(config)

        val embedded = svgCarrier.embed(carrier, payload)
        val extracted = svgCarrier.extract(embedded)

        assertThat(extracted).isEqualTo(payload)
    }

    @Test
    fun `calculate capacity returns positive value`() {
        val carrier = createMinimalSvg()
        val svgCarrier = SvgCarrier()

        val capacity = svgCarrier.calculateCapacity(carrier)

        assertThat(capacity).isGreaterThan(0)
    }

    @Test
    fun `embed throws exception for invalid SVG`() {
        val invalidCarrier = "not an svg".toByteArray()
        val payload = "Test".toByteArray()
        val svgCarrier = SvgCarrier()

        assertThatThrownBy { svgCarrier.embed(invalidCarrier, payload) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `supported type returns SVG`() {
        val svgCarrier = SvgCarrier()

        assertThat(svgCarrier.supportedType()).isEqualTo(FileType.SVG)
    }

    private fun createMinimalSvg(): String {
        return """<?xml version="1.0"?>
<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100">
  <rect x="10" y="10" width="80" height="80" fill="blue"/>
  <circle cx="50" cy="50" r="40" fill="red"/>
</svg>"""
    }
}
