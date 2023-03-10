package com.example.nsdshare

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.nio.charset.Charset





class FileShare(
    nsdShareViewModel: NsdShareViewModel
) {

    fun writeFileToSocket(socket: Socket, filepath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileUri = File(filepath)
                val inputStream = FileInputStream(fileUri)
                val outputStream = socket.outputStream

                inputStream.use { input ->
                    outputStream.use { output ->
                        input?.copyTo(output)
                    }
                }
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Error writing file to socket: ${e.message}")
            }
        }
    }


    fun writeStringToSocket(socket: Socket, txt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val byteArray= txt.toByteArray(Charset.defaultCharset())
                val outputStream = socket.outputStream

                outputStream.write(byteArray)
                outputStream.flush()
                outputStream.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Error writing data to socket: ${e.message}")
            }
        }
    }
    fun writeSocketToString(socket: Socket): String {
        var receivedString = ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = socket.getInputStream()
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val data = bufferedReader.readLine()
                receivedString = String(data.toByteArray(Charset.defaultCharset()))
                log(Tag.INFO, "WRITING SOCKET TO STRING! - $receivedString")
                inputStream.close()
            } catch (e: IOException) {
                Log.e(ContentValues.TAG, "Error reading data from socket: ${e.message}")
            }
        }
        return receivedString
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun writeSocketToFile(resolver: ContentResolver, socket: Socket, fileName: String) {
        // Set up a ContentValues object to describe the file we want to create
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return

        // Open an OutputStream to the file using the Uri
        val outputStream: OutputStream = resolver.openOutputStream(uri) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            // Insert the new record into the MediaStore database and get the corresponding Uri

            // Write the contents of the socket to the file using a BufferedOutputStream
            BufferedOutputStream(outputStream).use { bufferedOutputStream ->
                socket.getInputStream().use { inputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        bufferedOutputStream.write(buffer, 0, bytesRead)
                        log(Tag.INFO, "WRITING SOCKET TO FILE! - $bytesRead")
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            log(Tag.INFO, "WRITING SOCKET TO FILE COMPLETED!")
        }
        // If using API level 29 or higher, update the IS_PENDING flag to 0 to finalize the creation of the file
    }
}///////////////////////////////////////////////////////////////////////////////////////////////////////////
fun sendFile(nsdShareViewModel: NsdShareViewModel, filePath: File) {
    CoroutineScope(Dispatchers.IO).launch {
        val fileToSend = filePath
        val buffer = ByteArray(1024)
        var bytesRead: Int
        val inputStream: InputStream = FileInputStream(fileToSend)
        if (inputStream != null && nsdShareViewModel.nsdHelper.socket.isConnected) {
            val outputStream: OutputStream = nsdShareViewModel.nsdHelper.socket.getOutputStream()
            log(Tag.INFO, "Sending File")
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
        }
    }
}
fun receiveFile(contentResolver: ContentResolver, nsdShareViewModel: NsdShareViewModel) {
    CoroutineScope(Dispatchers.IO).launch {
//            while(true) {
//            val file = File("/storage/emulated/0/Download/", "NewFile.png")
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val downloadsPath = downloadsDirectory.getAbsolutePath()
        var file = File(downloadsPath, "new")
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val fileOutputStream = FileOutputStream(file)
        val buffer = ByteArray(1024)
        var bytesRead: Int
        val inputStream = nsdShareViewModel.nsdHelper.socket.getInputStream()
        if (nsdShareViewModel.nsdHelper.socket.isConnected && inputStream != null) {
            log(Tag.INFO, "Receiving File and socket connected? ${nsdShareViewModel.nsdHelper.socket.isConnected} ${nsdShareViewModel.nsdHelper.socket.isConnected}")
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOutputStream.write(buffer, 0, bytesRead)
                log(Tag.INFO, "isBeing Received $buffer")
            }
        }
        log(Tag.INFO, "WROTE TO ${file.absolutePath}")
//            }
    }
}
//
//@RequiresApi(Build.VERSION_CODES.O)
//    fun sendFileAsync(
//        serviceInfo: NsdServiceInfo,
//        nsdShareViewModel: NsdShareViewModel
//    ) {
//        val client = AsynchronousSocketChannel.open()
//        val serverAddress = InetSocketAddress(serviceInfo.host, serviceInfo.port)
//
//        client.connect(serverAddress, null, object : CompletionHandler<Void?, Void?> {
//            override fun completed(result: Void?, attachment: Void?) {
//                // Connection successful, now send filename and file contents
//                for (fileInList in nsdShareViewModel._history.value!!.toList()) {
//                    val file = File(fileInList.file.absolutePath)
//                    val filenameBuffer = ByteBuffer.wrap(file.name.toByteArray())
//                    client.write(filenameBuffer, null, object : CompletionHandler<Int?, Void?> {
//                        override fun completed(result: Int?, attachment: Void?) {
//                            // Filename sent successfully, now send file contents
//                            val fileChannel = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ)
//                            val fileBuffer = ByteBuffer.allocate(file.length().toInt())
//
//                            fileChannel.read(fileBuffer, 0, null, object : CompletionHandler<Int?, Void?> {
//                                override fun completed(result: Int?, attachment: Void?) {
//                                    // File contents read successfully, now send them
//                                    fileBuffer.flip()
//
//                                    client.write(fileBuffer, null, object : CompletionHandler<Int?, Void?> {
//                                        override fun completed(result: Int?, attachment: Void?) {
//                                            // File contents sent successfully
//                                            println("Sent file contents to server")
//                                        }
//
//                                        override fun failed(exc: Throwable?, attachment: Void?) {
//                                            println("Failed to send file contents to server: ${exc?.message}")
//                                        }
//                                    })
//                                }
//
//                                override fun failed(exc: Throwable?, attachment: Void?) {
//                                    println("Failed to read file contents: ${exc?.message}")
//                                }
//                            })
//                        }
//
//                        override fun failed(exc: Throwable?, attachment: Void?) {
//                            println("Failed to send filename: ${exc?.message}")
//                        }
//                    })
//
//                }
//            }
//
//            override fun failed(exc: Throwable?, attachment: Void?) {
//                println("Failed to connect to server: ${exc?.message}")
//            }
//        })
//
//        // Keep the main thread alive until the connection is closed
//        try {
//            Thread.sleep(5000)
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun receiveFileAsync(
//        contentResolver: ContentResolver,
//        nsdShareViewModel: NsdShareViewModel
//    ) {
//
//    }
//@RequiresApi(Build.VERSION_CODES.Q)
//    fun sendFileAsync(
//        serviceInfo: NsdServiceInfo,
//        nsdShareViewModel: NsdShareViewModel
//    ) {
//        val clientChannel = AsynchronousSocketChannel.open()
//        val bufferSize = 1024 * 1024 // 1 MB buffer
//        val buffer = ByteBuffer.allocate(bufferSize)
//
//
//    }
//
//    fun sendFile(
//        serviceInfo: NsdServiceInfo,
//        nsdShareViewModel: NsdShareViewModel
//    ) {
//        initializeSocket(nsdShareViewModel, serviceInfo)
//        log(Tag.INFO, "DID IT CONNECT ? ${nsdShareViewModel.nsdHelper.socket.isConnected}")
//        if (nsdShareViewModel.nsdHelper.socket.isConnected) {
//            val socket = nsdShareViewModel.nsdHelper.socket
//
//            val outputStream = socket.getOutputStream()
//            val dataOutputStream = DataOutputStream(outputStream)
//
//            for (file in nsdShareViewModel.history.value!!.toList()) {
//                // If Sending of File is pending
//                if (file.fileStatus == "pending") {
//                    // Sending Filename
//                    val filename = file.file.name
//                    CoroutineScope(Dispatchers.IO).launch {
//                        try {
//                            log(Tag.INFO, "Writing File Name to Socket")
//                            dataOutputStream.writeUTF(filename + "$")
//                        } catch (e: Exception) {
//                            e.printStackTrace()
//                        }
//                    }
//
//
//                    // Receive ACK
//                    val iStream = nsdShareViewModel.nsdHelper.socket.getInputStream()
//                    val dataInputStream = DataInputStream(iStream)
//                    val ACK = StringBuilder()
//                    var nextByte: Int
//                    CoroutineScope(Dispatchers.IO).launch {
//                        while (dataInputStream.read().also { nextByte = it } != -1 && nextByte.toString() != "$$$") {
//                            ACK.append(nextByte.toChar())
//                        }
//                    }
//
//                    if (ACK.toString() == "$$$") {
//                        // Sending File Itself
//                        val inputStream = FileInputStream(File(file.file.absolutePath))
//                        val buffer = ByteArray(1024)
//                        var bytesRead: Int
//                        do {
//                            bytesRead = inputStream.read(buffer)
//                            CoroutineScope(Dispatchers.IO).launch {
//                                try {
//                                    outputStream.write(buffer, 0, bytesRead)
//                                } catch (e: Exception) {
//                                    e.printStackTrace()
//                                }
//                            }
//                        } while(bytesRead != -1)
//                        // After Successful Transfer
//                        inputStream.close()
//                        outputStream.close()
//                        nsdShareViewModel.nsdHelper.socket.close()
//                        file.fileStatus = "completed"
//
//                    }
//                    else {
//                        log(Tag.INFO, "ACK NOT RECEIVED")
//                    }
//
//
//                } else {
//                    log(Tag.INFO, "ITS NOT FUCKING CONNECTED")
//                }
//            }
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    fun receiveFile(
//        contentResolver: ContentResolver,
//        nsdShareViewModel: NsdShareViewModel
//    ) {
//        // Read the filename from the socket
//        val inputStream = nsdShareViewModel.nsdHelper.socket.getInputStream()
//        val dataInputStream = DataInputStream(inputStream)
//        val filename = StringBuilder()
//        var nextByte: Int
//        CoroutineScope(Dispatchers.IO).launch {
//            while (dataInputStream.read().also { nextByte = it } != -1 && nextByte.toChar() != '$') {
//                filename.append(nextByte.toChar())
//            }
//        }
//
//        // ACK
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                log(Tag.INFO, "Writing File Name to Socket")
//                if (nsdShareViewModel.nsdHelper.socket.isConnected)
//                    DataOutputStream(nsdShareViewModel.nsdHelper.socket.getOutputStream()).writeUTF("$$$")
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//
//
//        // Writing file to Storage
//        writeSocketToFile(contentResolver, nsdShareViewModel.nsdHelper.socket, filename.toString())
//
//}