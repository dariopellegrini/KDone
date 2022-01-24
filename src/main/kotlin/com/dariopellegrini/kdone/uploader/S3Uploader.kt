package com.dariopellegrini.kdone.uploader

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

open class S3Uploader(val baseFolder: String,
                 val baseURL: String,
                 val bucketName: String,
                 accessKey: String,
                 secretKey: String,
                 serviceEndpoint: String,
                 signingRegion: String): Uploader {

    private val doCred = AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey))

    val s3Client = AmazonS3ClientBuilder.standard()
        .withCredentials(doCred)
        .withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                serviceEndpoint,
                signingRegion
            )
        )
        .withClientConfiguration(ClientConfiguration().apply {
            signerOverride = "S3SignerType"
        })
        .build()

    override suspend fun save(modelName: String, fileName: String, file: File, contentType: String): String? {
        return upload(modelName, fileName.replace(" ", ""), file, contentType)
    }

    private suspend fun upload(folder: String, name: String, file: File, contentType: String): String = withContext(Dispatchers.IO) {
            val randomString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                .toList()
                .shuffled()
                .joinToString("")
                .substring(0, 10)
            val fileName = "${System.currentTimeMillis()}$randomString-$name"
            try {
                val request = PutObjectRequest(bucketName, "$baseFolder/$folder/$fileName", file)
                val metadata = ObjectMetadata()
                metadata.contentType = contentType
                metadata.cacheControl = "max-age=60"
                request.metadata = metadata
                s3Client.putObject(
                    request.withCannedAcl(
                        CannedAccessControlList.PublicRead))
                if (file.exists()) {
                    file.delete()
                }
                return@withContext "$baseURL/$baseFolder/$folder/$fileName"
            } catch (e: Exception) {
                if (file.exists()) {
                    file.delete()
                }
                throw e
            }
    }

    override suspend fun delete(url: String) {
        return withContext(Dispatchers.IO) {
            val fileToDelete = url.replace("$baseURL/", "")
            try {
                s3Client.deleteObject(bucketName, fileToDelete)
            } catch (e: Exception) {
                println("$e")
            }
        }
    }
}