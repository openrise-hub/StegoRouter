package io.openrise.stegorouter.config

import io.openrise.stegorouter.carrier.archive.ZipEmbeddingMode

data class ZipStegoConfig(
    val embeddingMode: ZipEmbeddingMode = ZipEmbeddingMode.BOTH
)
