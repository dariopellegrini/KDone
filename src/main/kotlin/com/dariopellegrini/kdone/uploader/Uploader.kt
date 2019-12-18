package com.dariopellegrini.kdone.uploader

import java.io.File

interface Uploader {
    suspend fun save(folderName: String, fileName: String, file: File, contentType: String): String?
    suspend fun delete(url: String)
}