package com.example.nsdshare

import android.accounts.AccountManager
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ethan.nsdshare.NsdHelper
import java.io.File


class NsdShareViewModel(
    val context: Context
): ViewModel() {
    val _history = MutableLiveData(listOf(ShareUnit("filename",155,"pending...")))
    val history: LiveData<List<ShareUnit>> = _history
    val connectionStatus = MutableLiveData("")
    var deviceName = Build.MODEL
    var userName: String = getUserNameFromAccountManager()
    var customDeviceName = "$userName's $deviceName"
    val discoverDeviceList = MutableLiveData<List<NsdServiceInfo>>(listOf())
    val nsdHelper = NsdHelper(discoverDeviceList)
    var selectedFile: File = File("/storage/emulated/0/Download/NsdShare/NewFile")

    fun registerDeviceName() {
        nsdHelper.deviceName = customDeviceName
    }

    fun connectToResolvedServer(nsdShareViewModel: NsdShareViewModel, serviceInfo: NsdServiceInfo) {
        nsdHelper.initializeSocket(nsdShareViewModel, serviceInfo)
    }

    private fun getUserNameFromAccountManager(): String {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")
        return if (accounts.size > 0) {
            accounts[0].name
        } else {
            "Unknown User"
        }
    }

    fun changeConnectionStatus(status: String) {
        connectionStatus.value = status
    }
    fun initializeServerSocket(nsdShareViewModel: NsdShareViewModel) {
        nsdHelper.initializeServerSocket(nsdShareViewModel = nsdShareViewModel)
    }

    fun startNsdService(nsdShareViewModel: NsdShareViewModel) {
        nsdHelper.registerService(context)
        nsdHelper.discoverServices()
    }
}