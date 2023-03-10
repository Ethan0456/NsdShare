package com.example.nsdshare

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.Channels
import java.nio.channels.CompletionHandler
import java.nio.file.StandardOpenOption
import java.util.concurrent.CompletableFuture

class AsyncFileSender(private val host: InetAddress, private val port: Int) {

    companion object {
        private const val TAG = "AsyncFileSender"
        private const val BUFFER_SIZE = 8192 // 8KB
        private const val FIXED_HEADER_SIZE = 512
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun sendFile(file: File) = withContext(Dispatchers.IO) {
        try {
            val fileChannel = AsynchronousFileChannel.open(
                file.toPath(),
                StandardOpenOption.READ
            )

            val fileSize = fileChannel.size()
            val socketChannel = AsynchronousSocketChannel.open()

            val completion = socketChannel.connect(InetSocketAddress(host, port))
            completion.get()

            val outputStream = Channels.newOutputStream(socketChannel)

            // Send fixed-size header with filename length and file size
            val headerBuffer = ByteBuffer.allocate(FIXED_HEADER_SIZE)
            headerBuffer.putInt(file.name.length)
            log(Tag.INFO, "FILENAME LENGTH : ${file.name.length}")
            headerBuffer.putLong(fileSize)
            headerBuffer.flip()
            outputStream.write(headerBuffer.array())

            // Send file name
            outputStream.write(file.name.toByteArray())
            log(Tag.INFO, "FILENAME byte array: ${file.name.toByteArray()}")
            outputStream.flush()

            // Send file content
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            var totalBytesRead = 0L
            while (totalBytesRead < fileSize) {
                val bytesRead = fileChannel.read(buffer, totalBytesRead).get()
                if (bytesRead <= 0) {
                    break
                }

                totalBytesRead += bytesRead
                buffer.flip()
                outputStream.write(buffer.array(), 0, bytesRead)
                buffer.clear()
            }

            outputStream.flush()
            socketChannel.shutdownOutput()
            socketChannel.close()
            fileChannel.close()

        } catch (e: Exception) {
            Log.e(TAG, "Error sending file: ${e.message}")
            e.printStackTrace()
        }
    }
}

//    @RequiresApi(Build.VERSION_CODES.O)
//    suspend fun sendFile(file: File) = withContext(Dispatchers.IO) {
//        try {
//            val fileChannel = AsynchronousFileChannel.open(
//                file.toPath(),
//                StandardOpenOption.READ
//            )
//
//            val fileSize = fileChannel.size()
//            val socketChannel = AsynchronousSocketChannel.open()
//
//            val completion = socketChannel.connect(InetSocketAddress(host, port))
//            completion.get()
//
//            val outputStream = Channels.newOutputStream(socketChannel)
//
//            // Send file name
//            outputStream.write(file.name.toByteArray())
//            outputStream.write("$$$".toByteArray())
//            outputStream.flush()
//
//            // Send file content
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//            var totalBytesRead = 0L
//            while (totalBytesRead < fileSize) {
//                val bytesRead = fileChannel.read(buffer, totalBytesRead).get()
//                if (bytesRead <= 0) {
//                    break
//                }
//
//                totalBytesRead += bytesRead
//                buffer.flip()
//                outputStream.write(buffer.array(), 0, bytesRead)
//                buffer.clear()
//            }
//
//            outputStream.flush()
//            socketChannel.shutdownOutput()
//            socketChannel.close()
//            fileChannel.close()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error sending file: ${e.message}")
//            e.printStackTrace()
//        }
//    }


//    @RequiresApi(Build.VERSION_CODES.O)
//    suspend fun sendFile(file: File) = withContext(Dispatchers.IO) {
//        try {
//            val fileChannel = AsynchronousFileChannel.open(
//                file.toPath(),
//                StandardOpenOption.READ
//            )
//
//            val fileSize = fileChannel.size()
//            val socketChannel = AsynchronousSocketChannel.open()
//
//            log(Tag.INFO, "HOST : $host")
//
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//
//            // Use a CompletableFuture to wait for the socket channel to connect
//            val connectFuture = CompletableFuture<Void>()
//            socketChannel.connect(
//                InetSocketAddress(host, port),
//                null,
//                @RequiresApi(Build.VERSION_CODES.O)
//                object :
//                    CompletionHandler<Void, Any?> {
//                    override fun completed(result: Void?, attachment: Any?) {
//                        // Connection successful, signal future and write data
//                        connectFuture.complete(null)
//                    }
//
//                    override fun failed(exc: Throwable?, attachment: Any?) {
//                        // Connection failed, signal future with error
//                        connectFuture.completeExceptionally(exc)
//                    }
//                })
//
//            // Wait for the connection to complete
//            connectFuture.get()
//
//            log(Tag.INFO, "CONNECTION SUCCESSFUL")
//
//            var totalBytesRead = 0L
//            while (totalBytesRead < fileSize) {
//                val bytesRead = fileChannel.read(buffer, totalBytesRead).get()
//                if (bytesRead <= 0) {
//                    break
//                }
//
//                log(Tag.INFO, "WRITING DATA TO SOCKET")
//
//                totalBytesRead += bytesRead
//                buffer.flip()
//                socketChannel.write(buffer).get()
//                buffer.compact()
//            }
//
//            socketChannel.shutdownOutput()
//            socketChannel.close()
//            fileChannel.close()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error sending file: ${e.message}")
//            e.printStackTrace()
//        }
//    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    suspend fun sendFile(file: File) = withContext(Dispatchers.IO) {
//        try {
//            val fileChannel = AsynchronousFileChannel.open(
//                file.toPath(),
//                StandardOpenOption.READ
//            )
//
//            val fileSize = fileChannel.size()
//            val socketChannel = AsynchronousSocketChannel.open()
//            log(Tag.INFO, "HOST : $host")
//
//            socketChannel.connect(InetSocketAddress(host, port), null, null, completion
//
//            log(Tag.INFO, "CONNECTION SUCCESSFULL")
//
//            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
//
//            var totalBytesRead = 0L
//            while (totalBytesRead < fileSize) {
//                val bytesRead = fileChannel.read(buffer, totalBytesRead).get()
//                if (bytesRead <= 0) {
//                    break
//                }
//
//                log(Tag.INFO, "WRITING DATA TO SOCKET")
//
//                totalBytesRead += bytesRead
//                buffer.flip()
//                socketChannel.write(buffer).get()
//                buffer.compact()
//            }
//
//            socketChannel.shutdownOutput()
//            socketChannel.close()
//            fileChannel.close()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Error sending file: ${e.message}")
//            e.printStackTrace()
//        }
//    }
//}