package com.ethan.nsdshare

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CustomLifeCycleObserver(val context: Context, val nsdHelper: NsdHelper): DefaultLifecycleObserver{
    var isPaused = false
    override fun onPause(owner: LifecycleOwner) {
        log(Tag.INFO, "IN PAUSE")
        nsdHelper?.tearDown()
        isPaused = true
    }

    override fun onResume(owner: LifecycleOwner) {
        log(Tag.INFO, "IN RESUME")
        if (isPaused) {
            nsdHelper?.apply {
                registerService(context)
                discoverServices()
            }
        }
        isPaused = false
    }

    override fun onDestroy(owner: LifecycleOwner) {
        nsdHelper?.tearDown()
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
