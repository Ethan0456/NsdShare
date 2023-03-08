package com.ethan.nsdshare

import android.content.ContentValues.TAG
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Environment
import android.util.Log
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
import kotlin.properties.Delegates

class NsdHelper(
    val ls: MutableLiveData<List<NsdServiceInfo>>
) {
    lateinit var serverSocket: ServerSocket
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

    fun initializeServerSocket(nsdShareViewModel: NsdShareViewModel) {
        // Initializing with '0' because it will assign the open port itself
        serverSocket = ServerSocket(0).also { socket ->
            // Store the chosen port.
            mLocalPort = socket.localPort
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                while (true) {
                    nsdShareViewModel.nsdHelper.socket = serverSocket.accept()
                    listenForFiles(nsdShareViewModel = nsdShareViewModel)
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
                    receiveFile(nsdShareViewModel = nsdShareViewModel)
                }
                listenForFiles(nsdShareViewModel = nsdShareViewModel)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun listenForFiles(nsdShareViewModel: NsdShareViewModel) {
        receiveFile(nsdShareViewModel)
    }

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

    fun receiveFile(nsdShareViewModel: NsdShareViewModel) {
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
                log(Tag.INFO, "Receiving File and socket connected? ${socket.isConnected} ${nsdShareViewModel.nsdHelper.socket.isConnected}")
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead)
                    log(Tag.INFO, "isBeing Received $buffer")
                }
            }
            log(Tag.INFO, "WROTE TO ${file.absolutePath}")
//            }
        }
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