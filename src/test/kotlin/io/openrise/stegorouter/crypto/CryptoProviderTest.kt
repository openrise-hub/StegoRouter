package io.openrise.stegorouter.crypto

import kotlin.test.Test
import kotlin.test.assertTrue

class CryptoProviderTest {
    @Test
    fun `BouncyCastle provider is installed`() {
        assertTrue(CryptoProvider.isProviderInstalled())
    }
}
