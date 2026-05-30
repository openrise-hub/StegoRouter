package io.openrise.stegorouter.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

object CryptoProvider {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun isProviderInstalled(): Boolean {
        return Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null
    }
}
