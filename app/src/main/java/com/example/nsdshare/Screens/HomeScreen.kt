package com.example.nsdshare.Screens

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavHostController
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.UI_Components.CustomDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

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
    var buttonTxtObserver = buttonTxt.observeAsState()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NsdShare",
            color = MaterialTheme.colorScheme.primary,
            style = TextStyle(fontSize = 25.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
        )

        CenterAlignedTopAppBar(
            modifier = Modifier.weight(0.5f),
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = connectionStatus.value)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(9f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(historyObs.value!!.size) { item->
                Button(onClick = {}) {
                    Text(text = item.toString())
                }
            }
        }

        var showDialog = remember { mutableStateOf(false) }
        var selectFile = remember { mutableStateOf(false) }
        Button(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp)
                .weight(1f)
                .heightIn(min = 20.dp, max = 50.dp)
                .fillMaxSize(),
                onClick = {
                    if (buttonTxtObserver.value == "SEND") {
                        selectFile.value = true
                        buttonTxt.value = "CONNECT"
                    }
                    else if (buttonTxtObserver.value == "CONNECT") {
                        nsdShareViewModel.changeConnectionStatus("Waiting...")
                        showDialog.value = true
                        if (!isServiceRunning.value!!) {
                            nsdShareViewModel.registerDeviceName()
                            nsdShareViewModel.initializeServerSocket(nsdShareViewModel)
                            nsdShareViewModel.startNsdService(nsdShareViewModel)
                        }
                        log(Tag.INFO, "isConnected = ${isConnectedObserver.value}")
                        if (isConnectedObserver.value == true) {
                            nsdShareViewModel.changeConnectionStatus("Connected")
                            buttonTxt.value = "SEND"
                            showDialog.value = false
                            nsdShareViewModel.nsdHelper.sendFile(nsdShareViewModel,nsdShareViewModel.selectedFile)
                        }
                    }
                }
        ) {
            Text(text = buttonTxtObserver.value.toString())
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
                                nsdShareViewModel.connectToResolvedServer(nsdShareViewModel, item)
                                if (isConnectedObserver.value == true) {
                                    log(Tag.INFO, "Connected")
                                    showDialog.value = false
                                }
                            }
                        ) {
                            Text(text = String(item.attributes.get("dName")!!, StandardCharsets.UTF_8))
                        }
                    }
                }
            }
        )
        SelectFile(selectFile = selectFile, nsdShareViewModel = nsdShareViewModel)
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

//@Composable
//fun CustomFloatingActionButton(
//    selectFile: MutableState<Boolean>,
//    isConnected: State<Boolean?>
//) {
//    if (isConnected.value == true) {
//        FloatingActionButton(
//            onClick = {
//                selectFile.value = true
//            },
//        ) {
//            Icon(Icons.Default.KeyboardArrowUp, "Upload File")
//        }
//    }
//}

@Composable
fun SelectFile(
    selectFile: MutableState<Boolean>,
    nsdShareViewModel: NsdShareViewModel
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                uri?.let {
                    nsdShareViewModel.selectedFile = getFileFromUri(context, uri)
                    log(Tag.INFO, "AB = ${nsdShareViewModel.selectedFile.absolutePath}")
                }
            }
        }
    )
    if (selectFile.value) {
        launcher.launch("*/*")
        selectFile.value = false
//        nsdShareViewModel.selectedFile = file
//        nsdShareViewModel.nsdHelper.sendFile(nsdShareViewModel = nsdShareViewModel, file)
        Toast.makeText(LocalContext.current, "File : ${nsdShareViewModel.selectedFile}", Toast.LENGTH_SHORT).show()
    }
}
