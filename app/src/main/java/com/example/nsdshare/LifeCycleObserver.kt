package com.ethan.nsdshare

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.nsdshare.NsdShareViewModel

class CustomLifeCycleObserver(val context: Context, val nsdShareViewModel: NsdShareViewModel): DefaultLifecycleObserver{
    var isPaused = false
    override fun onPause(owner: LifecycleOwner) {
        log(Tag.INFO, "IN PAUSE")
        if (nsdShareViewModel.nsdHelper.isServiceRunning.value == true) {
            nsdShareViewModel.nsdHelper?.tearDown()
            nsdShareViewModel.nsdHelper.socket.close()
            isPaused = true
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        log(Tag.INFO, "IN RESUME")
        if (isPaused && nsdShareViewModel.nsdHelper.isServiceRunning.value == false) {
            nsdShareViewModel.nsdHelper?.apply {
                registerService(context)
                discoverServices()
            }
        }
        isPaused = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        nsdShareViewModel.nsdHelper?.tearDown()
    }
}

fun log(tag: Tag,msg: String) {
    when (tag) {
        Tag.DEBUG -> Log.d(ContentValues.TAG, msg)
        Tag.INFO -> Log.i(ContentValues.TAG, msg)
        Tag.WARNING -> Log.w(ContentValues.TAG, msg)
        Tag.ERROR -> Log.e(ContentValues.TAG, msg)
    }
}
