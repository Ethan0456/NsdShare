package com.example.nsdshare

import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ethan.nsdshare.NsdHelper
import com.example.nsdshare.UI_Components.AcceptDownload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class NsdShareViewModel(
    val context: Context,
    val contentResolver: ContentResolver
): ViewModel() {
    val _history = MutableLiveData<List<ShareUnit>>(listOf())
    val history: LiveData<List<ShareUnit>> = _history
    var deviceName = Build.MODEL
    var userName: String = getUserNameFromAccountManager()
    var customDeviceName = "$userName's $deviceName"
    val discoverDeviceList = MutableLiveData<List<NsdServiceInfo>>(listOf())
    val nsdHelper = NsdHelper(contentResolver, discoverDeviceList)

    val showAskForPermissionDialog = MutableLiveData(false)
    val askForDownloadResponse = MutableLiveData(false)

    fun askForDownloadPermission() {
        CoroutineScope(Dispatchers.IO).launch {
            val completableDeferred = CompletableDeferred<Unit>()
            completableDeferred.await()
            if (completableDeferred.isActive) {
                showAskForPermissionDialog.value = true
            }
        }
        return askforpermission
    }

    fun registerDeviceName() {
        nsdHelper.deviceName = customDeviceName
    }

    private fun getUserNameFromAccountManager(): String {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")
        return if (accounts.size > 0) {
            accounts[0].name
        } else {
            "Someone"
        }
    }
}