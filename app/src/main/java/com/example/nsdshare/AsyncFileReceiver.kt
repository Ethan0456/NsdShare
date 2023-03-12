package com.example.nsdshare

import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import com.example.nsdshare.Screens.ToastAnywhere
import com.example.nsdshare.UI_Components.AcceptDownload
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

class AsyncFileReceiver(
    private val port: Int,
    private val contentResolver: ContentResolver,
    private val nsdShareViewModel: NsdShareViewModel
) {
    companion object {
        private const val TAG = "AsyncFileReceiver"
        private const val BUFFER_SIZE = 8192 // 8KB
        private const val HEADER_SIZE = 512
    }
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun receiveFile() = withContext(Dispatchers.IO) {
        try {
            nsdShareViewModel.nsdHelper.uninitializeServerSocket(nsdShareViewModel)

            val serverChannel = AsynchronousServerSocketChannel.open()
            serverChannel.bind(InetSocketAddress(port))

            Log.d(TAG, "Listening for incoming connections on port $port...")
            log(Tag.INFO, "PORT FINAL: $port")

            serverChannel.accept(
                null,
                @RequiresApi(Build.VERSION_CODES.O)
                object : CompletionHandler<AsynchronousSocketChannel, Void?> {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    override fun completed(
                        socketChannel: AsynchronousSocketChannel,
                        attachment: Void?
                    ) {
                        log(
                            Tag.INFO,
                            "Accepted incoming connection from ${socketChannel.remoteAddress}"
                        )

                        // Read the fixed-size header to get the filename length
                        val headerBuffer = ByteBuffer.allocate(HEADER_SIZE)
                        socketChannel.read(headerBuffer).get()
                        headerBuffer.flip()
                        val fileNameLength = headerBuffer.int

                        // Read the filename
                        val fileNameBuffer = ByteBuffer.allocate(fileNameLength)
                        socketChannel.read(fileNameBuffer).get()
                        fileNameBuffer.flip()
                        val fileName = String(fileNameBuffer.array(), Charset.defaultCharset())

                        log(Tag.INFO, "FILENAME : $fileName")
                        log(Tag.INFO, "RECEIVING FILENAME")

                        nsdShareViewModel.showAskForPermissionDialog.postValue(true)
                        nsdShareViewModel.incomingFileName.postValue(fileName)

                        runBlocking {
                            while (nsdShareViewModel.askForDownloadResponse.value == -1) {}
                        }

                        if (nsdShareViewModel.askForDownloadResponse.value == 1) {
                            // Create the file using MediaStore API
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                                put(MediaStore.Downloads.IS_PENDING, 1)
                            }

                            val contentResolver = contentResolver
                            val downloadsUri =
                                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                            val fileUri = contentResolver.insert(downloadsUri, contentValues)


                            // Adding the new file name into the _history list to show on receiver's end
                            val fileShareUnit = ShareUnit(fileName.toHashSet().toString(), File(fileName), mutableStateOf(true))
                            nsdShareViewModel._history.postValue(
                                nsdShareViewModel._history.value?.plus(
                                    listOf(
                                        fileShareUnit
                                    )
                                )
                            )
                            log(Tag.INFO, "FILENAME : ${fileShareUnit.progress.value}")

                            if (fileUri == null) {
                                Log.e(TAG, "Failed to create file: $fileName")
                                return
                            }

                            contentResolver.openFileDescriptor(fileUri, "w", null).use { descriptor ->
                                if (descriptor == null) {
                                    Log.e(TAG, "Failed to open file descriptor for file: $fileName")
                                    return
                                }
                                val fileChannel = FileOutputStream(descriptor.fileDescriptor).channel

                                // Read the file data
                                val dataBuffer = ByteBuffer.allocate(BUFFER_SIZE)
                                var bytesRead: Int
                                do {
                                    bytesRead = socketChannel.read(dataBuffer).get()
                                    if (bytesRead > 0) {
                                        log(Tag.INFO, "RECEIVING FILE")
                                        dataBuffer.flip()
                                        fileChannel.write(dataBuffer)
                                        dataBuffer.clear()
                                    }
                                } while (bytesRead != -1)

                                fileChannel.close()
                                contentValues.clear()
                                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                                contentResolver.update(fileUri, contentValues, null, null)

                                socketChannel.close()

                                Log.d(TAG, "Received file $fileName")
                            }

                            fileShareUnit.progress.value = false
                            nsdShareViewModel.incomingFileName.postValue("")
                            nsdShareViewModel.askForDownloadResponse.postValue(-1)
                        }

                        // Accept another connection
                        serverChannel.accept(null, this)
                    }

                    override fun failed(exc: Throwable?, attachment: Void?) {
                        Log.e(TAG, "Error accepting incoming connection: ${exc?.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving file: ${e.message}")
            e.printStackTrace()
        }
    }
}



//    @RequiresApi(Build.VERSION_CODES.O)
//    suspend fun receiveFile() = withContext(Dispatchers.IO) {
//        try {
//            nsdShareViewModel.nsdHelper.uninitializeServerSocket(nsdShareViewModel)
//
//            val serverChannel = AsynchronousServerSocketChannel.open()
//            serverChannel.bind(InetSocketAddress(port))
//
//            Log.d(TAG, "Listening for incoming connections on port $port...")
//            log(Tag.INFO, "PORT FINAL: $port")
//
//            serverChannel.accept(
//                null,
//                @RequiresApi(Build.VERSION_CODES.O)
//                object : CompletionHandler<AsynchronousSocketChannel, Void?> {
//                    @RequiresApi(Build.VERSION_CODES.Q)
//                    override fun completed(
//                        socketChannel: AsynchronousSocketChannel,
//                        attachment: Void?
//                    ) {
//                        Log.d(
//                            TAG,
//                            "Accepted incoming connection from ${socketChannel.remoteAddress}"
//                        )
//
//                        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//
//                        val fileNameBuilder = StringBuilder()
//                        while (true) {
//                            buffer.clear()
//                            val bytesRead = socketChannel.read(buffer).get()
//                            if (bytesRead <= 0) {
//                                break
//                            }
//                            buffer.flip()
//                            val bytes = ByteArray(bytesRead)
//                            buffer.get(bytes)
//                            val receivedString = String(bytes, Charset.defaultCharset())
//                            fileNameBuilder.append(receivedString)
//                            if (fileNameBuilder.toString().endsWith("$$$")) {
//                                break
//                            }
//                        }
//                        val fileName = fileNameBuilder.toString().substring(0, fileNameBuilder.length - 3)
//                        log(Tag.INFO, "FILENAME : ${fileName}")
//                        log(Tag.INFO, "RECEIVING FILENAME")
//
//                        // Create the file using MediaStore API
//                        val contentValues = ContentValues().apply {
//                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
//                            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
//                            put(MediaStore.Downloads.IS_PENDING, 1)
//                        }
//
//                        val contentResolver = contentResolver
//                        val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//                        val fileUri = contentResolver.insert(downloadsUri, contentValues)
//                        nsdShareViewModel._history.postValue(nsdShareViewModel._history.value?.plus(listOf(
//                            ShareUnit(File(fileName),"pending")
//                        )))
//                        if (fileUri == null) {
//                            Log.e(TAG, "Failed to create file: $fileName")
//                            return
//                        }
//
//                        contentResolver.openFileDescriptor(fileUri, "w", null).use { descriptor ->
//                            if (descriptor == null) {
//                                Log.e(TAG, "Failed to open file descriptor for file: $fileName")
//                                return
//                            }
//                            val fileChannel = FileOutputStream(descriptor.fileDescriptor).channel
//
//                            while (true) {
//                                buffer.clear()
//                                val bytesRead = socketChannel.read(buffer).get()
//                                if (bytesRead <= 0) {
//                                    break
//                                }
//                                log(Tag.INFO, "RECEIVING FILE")
//                                buffer.flip()
//                                fileChannel.write(buffer)
//                            }
//
//                            fileChannel.close()
//                            contentValues.clear()
//                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
//                            contentResolver.update(fileUri, contentValues, null, null)
//
//                            socketChannel.close()
//
//                            Log.d(TAG, "Received file $fileName")
//                        }
//                    }
//
//                    override fun failed(exc: Throwable?, attachment: Void?) {
//                        Log.e(TAG, "Error accepting incoming connection: ${exc?.message}")
//                    }
//                }
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Error receiving file: ${e.message}")
//            e.printStackTrace()
//        }



//    @RequiresApi(Build.VERSION_CODES.O)
//    suspend fun receiveFile() = withContext(Dispatchers.IO) {
//        try {
//            nsdShareViewModel.nsdHelper.uninitializeServerSocket(nsdShareViewModel)
//
//            val serverChannel = AsynchronousServerSocketChannel.open()
//            serverChannel.bind(InetSocketAddress(port))
//
//            Log.d(TAG, "Listening for incoming connections on port $port...")
//            log(Tag.INFO, "PORT FINAL: $port")
//
//            serverChannel.accept(
//                null,
//                @RequiresApi(Build.VERSION_CODES.O)
//                object : CompletionHandler<AsynchronousSocketChannel, Void?> {
//                    @RequiresApi(Build.VERSION_CODES.Q)
//                    override fun completed(
//                        socketChannel: AsynchronousSocketChannel,
//                        attachment: Void?
//                    ) {
//                        Log.d(
//                            TAG,
//                            "Accepted incoming connection from ${socketChannel.remoteAddress}"
//                        )
//
//                        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//
//                        val fileNameBytes = ByteArray(BUFFER_SIZE)
//                        val fileNameBuffer = ByteBuffer.wrap(fileNameBytes)
//                        val fileNameLength = socketChannel.read(fileNameBuffer).get()
//                        fileNameBuffer.flip()
//                        val fileName = String(fileNameBytes, 0, fileNameLength)
//
//                        val file = createDownloadedFile("AI")
//                        val fileChannel = file.outputStream().channel
//                        var totalBytesWritten = 0L
//
//                        while (true) {
//                            buffer.clear()
//                            val bytesRead = socketChannel.read(buffer).get()
//                            if (bytesRead <= 0) {
//                                break
//                            }
//                            log(Tag.INFO, "RECEIVING FILE")
//                            totalBytesWritten += bytesRead
//                            buffer.flip()
//                            fileChannel.write(buffer)
//                        }
//
//                        fileChannel.close()
//                        socketChannel.close()
//
//                        Log.d(TAG, "Received file $fileName with size $totalBytesWritten bytes")
//                    }
//
//                    override fun failed(exc: Throwable?, attachment: Void?) {
//                        Log.e(TAG, "Error accepting incoming connection: ${exc?.message}")
//                    }
//                }
//            )
//        } catch (e: Exception) {
//            Log.e(TAG, "Error receiving file: ${e.message}")
//            e.printStackTrace()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun createDownloadedFile(fileName: String): File {
//        val downloadsDir =
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val filePath = Paths.get(downloadsDir.path, fileName).toString()
//        val file = File(filePath)
//
//        // Create the file in the Downloads folder
//        val values = ContentValues().apply {
//            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
//            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
//            put(MediaStore.Downloads.IS_PENDING, 1)
//        }
//        val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
//        if (uri != null) {
//            contentResolver.openOutputStream(uri)?.use { outputStream ->
//                BufferedOutputStream(outputStream).use { bufferedOutputStream ->
//                    file.inputStream().use { inputStream ->
//                        inputStream.copyTo(bufferedOutputStream)
//                    }
//                }
//                values.clear()
//                values.put(MediaStore.Downloads.IS_PENDING, 0)
//                contentResolver.update(uri, values, null, null)
//            }
//        }
//
//        return file
//    }


