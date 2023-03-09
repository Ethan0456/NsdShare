package com.example.nsdshare

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ethan.nsdshare.NsdHelper
import java.io.File


class NsdShareViewModel(
    val context: Context,
    val contentResolver: ContentResolver
): ViewModel() {
    val _history = MutableLiveData<List<String>>(listOf())
    val history: LiveData<List<String>> = _history
    val connectionStatus = MutableLiveData("")
    var deviceName = Build.MODEL
    var userName: String = getUserNameFromAccountManager()
    var customDeviceName = "$userName's $deviceName"
    val discoverDeviceList = MutableLiveData<List<NsdServiceInfo>>(listOf())
    val nsdHelper = NsdHelper(contentResolver, discoverDeviceList)
    var selectedFile: File = File("")

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
    @RequiresApi(Build.VERSION_CODES.Q)
    fun initializeServerSocket(nsdShareViewModel: NsdShareViewModel) {
        nsdHelper.initializeServerSocket(nsdShareViewModel = nsdShareViewModel)
    }

    fun startNsdService(nsdShareViewModel: NsdShareViewModel) {
        nsdHelper.registerService(context)
        nsdHelper.discoverServices()
    }
}