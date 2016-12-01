package com.aquivalabs.force.ant

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

fun ByteArray.getEntryContent(name: String): String? {
    ZipInputStream(ByteArrayInputStream(this)).use { zipInput ->
        var entry: ZipEntry?
        do {
            entry = zipInput.nextEntry
            if (entry == null)
                break
            if (entry.name != name)
                continue
            val output = ByteArrayOutputStream()
            output.use {
                val buffer = ByteArray(8192)
                var length: Int
                do {
                    length = zipInput.read(buffer, 0, buffer.size)
                    if (length > 0)
                        output.write(buffer, 0, length)
                } while (length > 0)
                return String(output.toByteArray())
            }
        } while (entry != null)
    }
    return null
}