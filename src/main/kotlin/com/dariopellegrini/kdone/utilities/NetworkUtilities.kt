package com.dariopellegrini.kdone.utilities

import com.dariopellegrini.kdone.exceptions.ServerException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class NetworkUtilities {
    companion object {
        suspend fun executeGet(targetURL: String): String = withContext(Dispatchers.IO) {
            val result = StringBuilder()
            val url = URL(targetURL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val rd = BufferedReader(InputStreamReader(connection.inputStream))
            var line: String? = rd.readLine()
            while (line != null) {
                result.append(line)
                line = rd.readLine()
            }
            rd.close()
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                result.toString()
            } else {
                throw ServerException(responseCode, result.toString())
            }
        }
    }
}