package io.openrise.stegorouter.compression

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object GzipCompressor {
    fun compress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        
        ByteArrayOutputStream().use { byteStream ->
            GZIPOutputStream(byteStream).use { gzipStream ->
                gzipStream.write(data)
            }
            return byteStream.toByteArray()
        }
    }

    fun decompress(data: ByteArray): ByteArray {
        if (data.isEmpty()) return ByteArray(0)
        
        ByteArrayInputStream(data).use { byteStream ->
            GZIPInputStream(byteStream).use { gzipStream ->
                val buffer = ByteArrayOutputStream()
                val tempBuffer = ByteArray(8192)
                var bytesRead: Int
                
                while (gzipStream.read(tempBuffer).also { bytesRead = it } != -1) {
                    buffer.write(tempBuffer, 0, bytesRead)
                }
                
                return buffer.toByteArray()
            }
        }
    }
}
