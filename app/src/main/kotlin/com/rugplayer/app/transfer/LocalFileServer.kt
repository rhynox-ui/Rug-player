package com.rugplayer.app.transfer

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder

/**
 * A deliberately tiny HTTP/1.1 server: it accepts a GET request and streams
 * back exactly one file. No routing, no other verbs — just enough to let a
 * second phone on the same Wi-Fi pull a video directly from this device.
 */
class LocalFileServer(private val contentResolver: ContentResolver) {

    private var serverSocket: ServerSocket? = null

    fun start(
        scope: CoroutineScope,
        uri: Uri,
        fileName: String,
        fileSizeBytes: Long,
        onClientConnected: () -> Unit = {},
    ): Int {
        val socket = ServerSocket(0)
        serverSocket = socket

        scope.launch(Dispatchers.IO) {
            while (isActive) {
                val client = try {
                    socket.accept()
                } catch (e: IOException) {
                    break
                }
                onClientConnected()
                launch(Dispatchers.IO) {
                    runCatching { serveFile(client, uri, fileName, fileSizeBytes) }
                }
            }
        }
        return socket.localPort
    }

    private fun serveFile(client: Socket, uri: Uri, fileName: String, fileSizeBytes: Long) {
        client.use { socket ->
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            reader.readLine() ?: return
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
            }

            val output = socket.getOutputStream()
            val encodedName = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
            val header = buildString {
                append("HTTP/1.1 200 OK\r\n")
                append("Content-Type: application/octet-stream\r\n")
                append("Content-Length: $fileSizeBytes\r\n")
                append("Content-Disposition: attachment; filename=\"$encodedName\"\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(header.toByteArray(Charsets.US_ASCII))

            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
            }
            output.flush()
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
    }
}
