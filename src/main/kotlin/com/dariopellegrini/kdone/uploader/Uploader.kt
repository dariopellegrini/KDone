package com.dariopellegrini.kdone.uploader

import java.io.File

interface Uploader {
    suspend fun save(modelName: String, fileName: String, file: File, contentType: String): String?
    suspend fun delete(url: String)
}