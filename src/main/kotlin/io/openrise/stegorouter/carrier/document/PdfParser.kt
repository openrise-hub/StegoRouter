package io.openrise.stegorouter.carrier.document

import java.io.ByteArrayOutputStream

class PdfParser {
    data class PdfObject(
        val objectNumber: Int,
        val generationNumber: Int,
        val offset: Int,
        val content: ByteArray
    )

    data class PdfStructure(
        val version: String,
        val objects: List<PdfObject>,
        val xrefOffset: Int,
        val trailer: ByteArray
    )

    fun parse(data: ByteArray): PdfStructure {
        val text = String(data, Charsets.ISO_8859_1)

        require(text.startsWith("%PDF-")) { "Invalid PDF file" }

        val versionEnd = text.indexOf('\n')
        val version = text.substring(5, versionEnd).trim()

        val objects = parseObjects(data, text)

        val startXref = text.indexOf("startxref")
        require(startXref >= 0) { "startxref not found" }

        val xrefOffsetMatch = Regex("""startxref\s+(\d+)""").find(text.substring(startXref))
        val xrefOffset = xrefOffsetMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val trailerStart = text.indexOf("trailer")
        val trailerEnd = text.indexOf("startxref")
        val trailer = if (trailerStart >= 0 && trailerEnd > trailerStart) {
            data.copyOfRange(trailerStart, trailerEnd)
        } else {
            ByteArray(0)
        }

        return PdfStructure(version, objects, xrefOffset, trailer)
    }

    fun rebuild(structure: PdfStructure, modifications: Map<Int, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()

        output.write("%PDF-${structure.version}\n".toByteArray(Charsets.ISO_8859_1))
        output.write("%âãÏÓ\n".toByteArray(Charsets.ISO_8859_1))

        val objectOffsets = mutableMapOf<Int, Int>()

        for (obj in structure.objects) {
            objectOffsets[obj.objectNumber] = output.size()

            val content = modifications[obj.objectNumber] ?: obj.content
            output.write("${obj.objectNumber} ${obj.generationNumber} obj\n".toByteArray(Charsets.ISO_8859_1))
            output.write(content)
            output.write("\nendobj\n".toByteArray(Charsets.ISO_8859_1))
        }

        val xrefOffset = output.size()
        output.write("xref\n".toByteArray(Charsets.ISO_8859_1))
        output.write("0 ${structure.objects.size + 1}\n".toByteArray(Charsets.ISO_8859_1))
        output.write("0000000000 65535 f \n".toByteArray(Charsets.ISO_8859_1))

        for (obj in structure.objects) {
            val offset = objectOffsets[obj.objectNumber] ?: 0
            output.write(String.format("%010d %05d n \n", offset, obj.generationNumber).toByteArray(Charsets.ISO_8859_1))
        }

        output.write("trailer\n".toByteArray(Charsets.ISO_8859_1))
        output.write("<< /Size ${structure.objects.size + 1} /Root 1 0 R >>\n".toByteArray(Charsets.ISO_8859_1))
        output.write("startxref\n".toByteArray(Charsets.ISO_8859_1))
        output.write("$xrefOffset\n".toByteArray(Charsets.ISO_8859_1))
        output.write("%%EOF\n".toByteArray(Charsets.ISO_8859_1))

        return output.toByteArray()
    }

    private fun parseObjects(data: ByteArray, text: String): List<PdfObject> {
        val objects = mutableListOf<PdfObject>()
        val pattern = Regex("""(\d+)\s+(\d+)\s+obj""")

        pattern.findAll(text).forEach { match ->
            val objectNumber = match.groupValues[1].toInt()
            val generationNumber = match.groupValues[2].toInt()
            val offset = match.range.first

            val endObjMatch = Regex("""endobj""").find(text, match.range.last)
            if (endObjMatch != null) {
                val contentStart = match.range.last + 1
                val contentEnd = endObjMatch.range.first
                val content = data.copyOfRange(contentStart, contentEnd)

                objects.add(PdfObject(objectNumber, generationNumber, offset, content))
            }
        }

        return objects
    }
}
