package com.dariopellegrini.kdone.extensions

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import java.io.File

fun PartData.FileItem.getFile(): File? {
    this.originalFileName?.let { name ->
        if (!File("tmp").exists()) File("tmp").mkdir()
        val file = File("tmp/$name")
        // use InputStream from part to save file
        this.streamProvider().use { its ->
            // copy the stream to the file with buffering
            file.outputStream().buffered().use {
                // note that this is blocking
                its.copyTo(it)
            }
            return file
        }
    } ?: run {
        return null
    }
}