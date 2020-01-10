package com.dariopellegrini.kdone.extensions

import com.dariopellegrini.kdone.exceptions.BadRequestException
import com.dariopellegrini.kdone.uploader.LocalUploader
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.routing.Route
import io.ktor.routing.get
import java.io.File

fun Route.localUploader(filesFolder: String): LocalUploader {
    val localUploader = LocalUploader(filesFolder)

    try {
        get("${localUploader.filesFolder}/{folder}/{fileName}") {
            try {
                val folder = call.parameters["folder"] ?: throw BadRequestException("Missing folder")
                val fileName = call.parameters["fileName"] ?: throw BadRequestException("Missing folder")
                val file = File("${localUploader.filesFolder}/$folder/$fileName")
                if (file.exists()) {
                    call.respondFile(file)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                call.respondWithException(e)
            }
        }
    } catch (e: Exception) {
        println(e)
    }
    return localUploader
}