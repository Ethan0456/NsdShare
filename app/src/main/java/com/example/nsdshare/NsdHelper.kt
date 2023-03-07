package com.ethan.nsdshare

import android.content.ContentValues.TAG
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.properties.Delegates

class NsdHelper(
    val deviceName: String,
    val ls: MutableLiveData<List<NsdServiceInfo>>
) {
    private lateinit var serverSocket: ServerSocket
    private var mLocalPort: Int = 0
    private var mServiceName: String = "NsdChat"+"$"+deviceName
    lateinit var nsdManager: NsdManager
    private val SERVICE_TYPE = "_nsdchat._tcp."
    private var resolvedPort by Delegates.notNull<Int>()
    private lateinit var resolvedHost: InetAddress
    private lateinit var resolvedmService : NsdServiceInfo
    var attributes: MutableMap<String, ByteArray> = mutableMapOf()

    var isServiceRunning = MutableLiveData<Boolean>(false)

    private var _isResolved = MutableLiveData<Boolean>(false)
    var isResolved: LiveData<Boolean> = _isResolved

    fun initializeServerSocket() {
        // Initializing with '0' because it will assign the open port itself
        serverSocket = ServerSocket(0).also { socket ->
            // Store the chosen port.
            mLocalPort = socket.localPort
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            mServiceName = NsdServiceInfo.serviceName
            isServiceRunning.postValue(true)
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            log(Tag.INFO, "Service Registration Failed")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            log(Tag.INFO, "Service UnRegistration Successfully")
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
            attributes['deviceName'] = deviceName
            setPort(mLocalPort)
        }

        nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    fun discoverServices() {
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun onClickResolve(service: NsdServiceInfo) {
        nsdManager.resolveService(service, resolveListener)
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
                    log(Tag.INFO, "Added $service to list")
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
//            resolvedmService = serviceInfo
//            resolvedPort = serviceInfo.port
//            resolvedHost = serviceInfo.host
//
            ls.postValue(ls.value!!.plus(listOf(serviceInfo)))
            _isResolved.postValue(true)

//            log(Tag.INFO, "RESOLVED : $resolvedHost, $resolvedPort, $resolvedmService")
        }
    }

    fun tearDown() {
        nsdManager.apply {
            unregisterService(registrationListener)
            stopServiceDiscovery(discoveryListener)
        }
    }
}