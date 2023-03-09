package com.ethan.nsdshare

import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Environment
import android.os.LocaleList
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.nsdshare.NsdShareViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import kotlin.properties.Delegates

class NsdHelper(
    val contentResolver: ContentResolver,
    val ls: MutableLiveData<List<NsdServiceInfo>>
) {
    var serverSocket: ServerSocket? = null
    private var mLocalPort: Int = 0
    private var mServiceName: String = "NsdChat"
    lateinit var nsdManager: NsdManager
    private val SERVICE_TYPE = "_nsdchat._tcp."
    private var resolvedPort by Delegates.notNull<Int>()
    private lateinit var resolvedHost: InetAddress
    private lateinit var resolvedmService : NsdServiceInfo
    var attributes: MutableMap<String, ByteArray> = mutableMapOf()
    lateinit var deviceName: String
    val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    val appDownloadPath = File(downloadsDirectory,"NsdShare")
    val downloadsPath = {
        if (!appDownloadPath.exists()) {
            appDownloadPath.mkdirs().toString()
        }
        appDownloadPath.toString()
    }

    var isServiceRunning = MutableLiveData(false)

    private var _isConnected = MutableLiveData(false)
    var isConnected: LiveData<Boolean> = _isConnected
    var socket: Socket = Socket()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initializeServerSocket(nsdShareViewModel: NsdShareViewModel) {
        // Initializing with '0' because it will assign the open port itself
        serverSocket = ServerSocket(0).also { socket ->
            // Store the chosen port.
            mLocalPort = socket.localPort
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (true) {
                    nsdShareViewModel.nsdHelper.socket = serverSocket!!.accept()
                    if (nsdShareViewModel.nsdHelper.socket.isConnected) {
                        log(Tag.INFO, "THIS MEANS SOMEONE WANTS TO SEND FILE")
                        val nextFileName = writeSocketToString(nsdShareViewModel.nsdHelper.socket)
                        nsdShareViewModel._history.postValue(nsdShareViewModel._history.value?.plus(listOf(nextFileName)))
                        writeSocketToFile(contentResolver, nsdShareViewModel.nsdHelper.socket, nextFileName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initializeSocket(nsdShareViewModel: NsdShareViewModel, serviceInfo: NsdServiceInfo) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                log(Tag.INFO, "ServiceInfo Host: ${serviceInfo.host.toString().replace("/","")}")
                nsdShareViewModel.nsdHelper.socket = Socket(serviceInfo.host, serviceInfo.port)
                if (socket.isConnected) {
                    _isConnected.postValue(true)
                }
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
                Log.e(TAG, "Error writing file to socket: ${e.message}")
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
                Log.e(TAG, "Error writing data to socket: ${e.message}")
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
                inputStream.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error reading data from socket: ${e.message}")
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
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        }
        // If using API level 29 or higher, update the IS_PENDING flag to 0 to finalize the creation of the file
    }
    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            mServiceName = NsdServiceInfo.serviceName
            isServiceRunning.postValue(true)
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            log(Tag.INFO, "Service Registration Failed")
            isServiceRunning.postValue(false)
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            log(Tag.INFO, "Service UnRegistration Successfully")
            isServiceRunning.postValue(false)
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            log(Tag.INFO, "Service UnRegistration Failed")
            isServiceRunning.postValue(true)
        }
    }

    fun registerService(context: Context) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = mServiceName
            serviceType = SERVICE_TYPE
            log(Tag.INFO, "Setting Attributes ${deviceName}")
            setAttribute("dName",deviceName)
            setPort(mLocalPort)
        }

        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(regType: String) {
            log(Tag.INFO, "Service Discovery Started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            log(Tag.INFO, "Service Discovery Success - $service")
            when {
                service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
                    // transport layer for this service.
                    log(Tag.INFO, "Unknown Service Type - ${service.serviceType} - ${SERVICE_TYPE}")
                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    log(Tag.INFO, "Same machine - ${mServiceName}")
                service.serviceName.contains("NsdChat") -> {
                    log(Tag.INFO, "Different machine - ${mServiceName}")
                    nsdManager.resolveService(service, resolveListener)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "service lost: $service")
            ls.postValue(ls.value!!.minus(listOf(service)))
            log(Tag.INFO, "Removed $service from list")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            log(Tag.INFO, "Resolve failed - $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            log(Tag.INFO, "Resolve Succeded - $serviceInfo")

            if (serviceInfo.serviceName == mServiceName) {
                log(Tag.INFO, "Same IP")
                return
            }
            resolvedmService = serviceInfo
            resolvedPort = serviceInfo.port
            resolvedHost = serviceInfo.host
            ls.postValue(ls.value!!.plus(listOf(serviceInfo)))
//            _isResolved.postValue(true)
            attributes = serviceInfo.attributes

            log(Tag.INFO, "RESOLVED : $resolvedHost, $resolvedPort, $resolvedmService, ${attributes.get("dName")}")
            log(Tag.INFO, "${attributes.get("dName")}")
        }
    }

    fun tearDown() {
        nsdManager.apply {
            unregisterService(registrationListener)
            stopServiceDiscovery(discoveryListener)
        }
    }
}

///////////////////////////////////////////////////////////////////////////////////////////////////////////
//fun sendFile(nsdShareViewModel: NsdShareViewModel, filePath: File) {
//    CoroutineScope(Dispatchers.IO).launch {
//        val fileToSend = filePath
//        val buffer = ByteArray(1024)
//        var bytesRead: Int
//        val inputStream: InputStream = FileInputStream(fileToSend)
//        if (inputStream != null && nsdShareViewModel.nsdHelper.socket.isConnected) {
//            val outputStream: OutputStream = nsdShareViewModel.nsdHelper.socket.getOutputStream()
//            log(Tag.INFO, "Sending File")
//            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                outputStream.write(buffer, 0, bytesRead)
//            }
//            outputStream.close()
//        }
//    }
//}
//fun receiveFile(nsdShareViewModel: NsdShareViewModel) {
//    CoroutineScope(Dispatchers.IO).launch {
////            while(true) {
////            val file = File("/storage/emulated/0/Download/", "NewFile.png")
//        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
//        val downloadsPath = downloadsDirectory.getAbsolutePath()
//        var file = File(downloadsPath, "new")
//        if (!file.exists()) {
//            try {
//                file.createNewFile()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//        val fileOutputStream = FileOutputStream(file)
//        val buffer = ByteArray(1024)
//        var bytesRead: Int
//        val inputStream = nsdShareViewModel.nsdHelper.socket.getInputStream()
//        if (nsdShareViewModel.nsdHelper.socket.isConnected && inputStream != null) {
//            log(Tag.INFO, "Receiving File and socket connected? ${socket.isConnected} ${nsdShareViewModel.nsdHelper.socket.isConnected}")
//            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                fileOutputStream.write(buffer, 0, bytesRead)
//                log(Tag.INFO, "isBeing Received $buffer")
//            }
//        }
//        log(Tag.INFO, "WROTE TO ${file.absolutePath}")
////            }
//    }
//}
//
