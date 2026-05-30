package io.openrise.stegorouter.config

import io.openrise.stegorouter.carrier.image.LsbMode

data class ImageStegoConfig(
    val lsbMode: LsbMode = LsbMode.REPLACEMENT,
    val bitsPerChannel: Int = 1
) {
    init {
        require(bitsPerChannel in 1..2) { "bitsPerChannel must be 1 or 2" }
    }
}
