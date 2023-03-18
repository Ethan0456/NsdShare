package com.example.nsdshare.UI_Components

import androidx.compose.runtime.MutableState
import androidx.lifecycle.MutableLiveData

data class CustomBlockResult(
    val progress: MutableState<Boolean>,
    val progressAmount: MutableLiveData<Long>
)
