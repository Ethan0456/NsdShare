package com.example.nsdshare

import androidx.compose.runtime.MutableState
import java.io.File

//data class ShareUnit(
//    val hash: String,
//    val file: File,
//    var fileStatus: String,
//)

data class ShareUnit(
    val hash: String,
    val file: File,
    var progress: MutableState<Boolean>
)
