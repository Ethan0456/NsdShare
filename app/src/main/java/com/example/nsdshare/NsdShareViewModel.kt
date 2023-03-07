package com.example.nsdshare

import android.content.Context
import android.net.nsd.NsdServiceInfo
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ethan.nsdshare.NsdHelper

class NsdShareViewModel(
    val context: Context
): ViewModel() {
    val _history = MutableLiveData<List<ShareUnit>>(listOf(ShareUnit("filename",155,"pending...")))
    val history: LiveData<List<ShareUnit>> = _history
    val connectionStatus = MutableLiveData<String>("")
    var _deviceName = MutableLiveData<String>("")
    var deviceName: LiveData<String> = _deviceName

    val discoverDeviceList = MutableLiveData<List<NsdServiceInfo>>(listOf())

    val nsdHelper = NsdHelper(deviceName.value.toString(), discoverDeviceList)


    fun onDeviceNameChange(newTxt: String) {
        _deviceName.value = newTxt
    }

    fun changeConnectionStatus(status: String) {
        connectionStatus.value = status
    }

    fun startNsdService() {
        nsdHelper.initializeServerSocket()
        nsdHelper.registerService(context)
        nsdHelper.discoverServices()
    }
}