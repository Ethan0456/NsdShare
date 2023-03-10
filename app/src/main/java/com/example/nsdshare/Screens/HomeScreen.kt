package com.example.nsdshare.Screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.DiscretePathEffect
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavHostController
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import com.example.nsdshare.AsyncFileSender
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.ShareUnit
import com.example.nsdshare.UI_Components.CustomBlock
import com.example.nsdshare.UI_Components.CustomDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(
    nsdShareViewModel: NsdShareViewModel,
    navHostController: NavHostController
) {
    val connectionStatus: State<String> = nsdShareViewModel.connectionStatus.observeAsState("")
    val historyObs = nsdShareViewModel.history.observeAsState()
    val isConnectedObserver = nsdShareViewModel.nsdHelper.isConnected.observeAsState()
    val discoverDeviceListObserver = nsdShareViewModel.nsdHelper.ls.observeAsState()
    var isServiceRunning = nsdShareViewModel.nsdHelper.isServiceRunning.observeAsState()
    var buttonTxt = MutableLiveData("SEND")
    var showDialog = remember { mutableStateOf(false) }
    var selectFile = remember { mutableStateOf(false) }
    var _toastTxt = MutableLiveData("")
    var toastTxtObserver = _toastTxt.observeAsState()
    val context = LocalContext.current

    if (toastTxtObserver.value.toString() != "")
        ToastAnywhere(msg = toastTxtObserver.value.toString())

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NsdSwitch(showDialog = showDialog, nsdShareViewModel = nsdShareViewModel)

        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .weight(9f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items = historyObs.value!!.toList()) {item->
                Button(
                    onClick = {
                    },
                    modifier = Modifier
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    CustomBlock(
                        fileName = item.file.name,
//                        fileStatus = item.fileStatus
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(20.dp)
                    .weight(1f)
                    .fillMaxSize(),
                onClick = {
                    selectFile.value = true
                    log(Tag.INFO, "isConnected = ${isConnectedObserver.value}")
                }
            ) {
                Text(
                    text = "PICK FILE",
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
            }

        }
        CustomDialog(
            showDialog = showDialog,
            title = "Devices On Network",
            composable = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(400.dp)
                ) {
                    items(discoverDeviceListObserver.value!!.toList()) { item->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (nsdShareViewModel.history.value!!.isNotEmpty()) {
                                    log(Tag.INFO, "Connected")
                                    showDialog.value = false
                                    log(Tag.INFO, "YES IT IS CONNECTED THUS TURNING OFF DIALOG")
                                    val asyncFileSender = AsyncFileSender(item.host, item.port)
                                    CoroutineScope(Dispatchers.IO).launch {
                                        for (file in nsdShareViewModel.history.value!!.toList()) {
//                                            if (file.fileStatus == "pending") {
                                            log(Tag.INFO, "Files in history ${file.file.name}")
                                                asyncFileSender.sendFile(file.file)
//                                                file.fileStatus = "completed"
//                                            }
                                        }
                                    }
                                } else {
                                    _toastTxt.value = "Please Select A File First"
                                }
                            }
                        ) {
                            Text(text = String(item.attributes.get("dName")!!, StandardCharsets.UTF_8))
                        }
                    }
                }
            }
        )
        SelectFile(context = context,selectFile = selectFile, nsdShareViewModel = nsdShareViewModel)
    }
}

@SuppressLint("Range")
private fun getDisplayName(contentResolver: ContentResolver, uri: Uri): String {
    var displayName = ""
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.let {
        if (it.moveToFirst()) {
            displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        it.close()
    }
    return displayName
}

private fun getFileFromUri(context: Context, uri: Uri): File {
    val contentResolver = context.contentResolver
    val displayName = getDisplayName(contentResolver, uri)
    val inputStream = contentResolver.openInputStream(uri)
    val file = File(context.cacheDir, displayName)
    val outputStream = FileOutputStream(file)

    inputStream?.copyTo(outputStream)

    return file
}

@Composable
fun SelectFile(
    context: Context,
    selectFile: MutableState<Boolean>,
    nsdShareViewModel: NsdShareViewModel
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                uri?.let {
                    val file = getFileFromUri(context = context, uri)
                    log(Tag.INFO, "UPDATED HISTORY LIST WITH ${file.name}")
                    nsdShareViewModel._history.setValue(nsdShareViewModel._history.value?.plus(listOf(
                        ShareUnit(file.absolutePath.toHashSet().toString(),file)
                    )))
                    log(Tag.INFO, "AB = ${file.path}")
                    selectFile.value = false
                }
            }
        }
    )
    if (selectFile.value) {
        DisposableEffect(Unit) {
            launcher.launch("*/*")
            onDispose {
                log(Tag.INFO, "INSIDE ONDISPOSE")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun NsdSwitch(
    showDialog: MutableState<Boolean>,
    nsdShareViewModel: NsdShareViewModel
) {
    var isChecked by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        onClick = {
            showDialog.value = true
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Start Network Service Discovery",
                modifier = Modifier.padding(16.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = SwitchDefaults.colors(uncheckedThumbColor = MaterialTheme.colorScheme.inverseOnSurface, uncheckedBorderColor = MaterialTheme.colorScheme.inverseOnSurface, uncheckedTrackColor = MaterialTheme.colorScheme.inversePrimary),
                checked = isChecked,
                onCheckedChange = { checked ->
                    isChecked = checked
                    if (checked) {
                        nsdShareViewModel.nsdHelper.initializeServerSocket(nsdShareViewModel)
                        nsdShareViewModel.registerDeviceName()
                        nsdShareViewModel.nsdHelper.registerService(nsdShareViewModel.context)
                        nsdShareViewModel.nsdHelper.discoverServices()
                        showDialog.value = true
                    } else {
                        nsdShareViewModel.nsdHelper.tearDown()
                    }
                }
            )
        }
    }
}

@Composable
fun ToastAnywhere(msg: String) {
    Toast.makeText(LocalContext.current, msg, Toast.LENGTH_SHORT).show()
}
//Button(
//modifier = Modifier
//.background(MaterialTheme.colorScheme.background)
//.padding(20.dp)
//.weight(1f)
//.heightIn(min = 20.dp, max = 50.dp)
//.fillMaxSize(),
//onClick = {
//    showDialog.value = true
//    log(Tag.INFO, "isConnected = ${isConnectedObserver.value}")
//}
//) {
//    Text(text = "CONNECT")
//}
