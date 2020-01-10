package com.dariopellegrini.kdone.uploader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class LocalUploader(val filesFolder: String): Uploader {

    private val dirFile = File(filesFolder)

    init {
        if (!dirFile.exists()) {
            dirFile.mkdir()
        }
    }

    override suspend fun save(modelName: String, fileName: String, file: File, contentType: String): String? {
        return upload(modelName, fileName, file, contentType)
    }

    private suspend fun upload(folder: String, name: String, file: File, contentType: String): String = withContext(Dispatchers.IO) {
        val randomString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toList()
            .shuffled()
            .joinToString("")
            .substring(0, 10)
        val firstFolder = randomString[0].toString().toLowerCase()
        val secondFolder = randomString[1].toString().toLowerCase()
        val thirdFolder = randomString[3].toString().toLowerCase()

        val currentFolder = "$filesFolder/$firstFolder/$secondFolder/$thirdFolder/$folder"
        val currentFolderFile = File(currentFolder)
        if (!currentFolderFile.exists()) {
            currentFolderFile.mkdirs()
        }
        val fileName = "$currentFolder/${System.currentTimeMillis()}$randomString-$name"
        try {
            if (file.exists()) {
                Files.copy(file.toPath(), Paths.get(fileName))
                file.delete()
            }
            return@withContext fileName
        } catch (e: Exception) {
            if (file.exists()) {
                file.delete()
            }
            throw e
        }
    }

    override suspend fun delete(url: String) {
        return withContext(Dispatchers.IO) {
            try {
                val result = File(url).delete()
                println(result)
            } catch (e: Exception) {
                println("$e")
            }
        }
    }
}