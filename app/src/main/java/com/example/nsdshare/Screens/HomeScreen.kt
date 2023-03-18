package com.example.nsdshare.Screens

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavHostController
import com.ethan.nsdshare.Tag
import com.ethan.nsdshare.log
import com.example.nsdshare.AsyncFileSender
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.ShareUnit
import com.example.nsdshare.UI_Components.AcceptDownload
import com.example.nsdshare.UI_Components.CustomBlock
import com.example.nsdshare.UI_Components.CustomDeviceDiscoveryDialog
import com.example.nsdshare.UI_Components.CustomFileInfoDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import androidx.compose.ui.text.TextStyle

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun HomeScreen(
    nsdShareViewModel: NsdShareViewModel,
    navHostController: NavHostController
) {
    val historyObs = nsdShareViewModel.history.observeAsState()
    val isConnectedObserver = nsdShareViewModel.nsdHelper.isConnected.observeAsState()
    val discoverDeviceListObserver = nsdShareViewModel.nsdHelper.ls.observeAsState()
    var showDeviceDiscoveryDialog = remember { mutableStateOf(false) }
    var showFileInfoDialog = remember { mutableStateOf(false) }
    var selectFile = remember { mutableStateOf(false) }
    var _toastTxt = MutableLiveData("")
    var toastTxtObserver = _toastTxt.observeAsState("")
    val context = LocalContext.current

    val fileSelectedToGetInfo = MutableLiveData<File>()


    val showAskForPermissionDialogObserver = nsdShareViewModel.showAskForPermissionDialog.observeAsState()

    // Problem was when app starts it gave a blank toast message, this solved it, now it only toast when the _toastTxt changes
    if (toastTxtObserver.value.toString() != "")
        ToastAnywhere(msg = toastTxtObserver.value.toString())

    // Main Column
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // * Top NsdSwitch Composable which displays a switch to on and off the nsd service
        //                 with functionality of displaying ShowDeviceDiscoveryDialog box
        NsdSwitch(showDialog = showDeviceDiscoveryDialog, nsdShareViewModel = nsdShareViewModel)

        // * _history is the mutableLiveData list which stores the list of selected files
        //              and receives files of the respective sender and receiver's end
        // * historyObs is the observer of _history live data
        // * This lazy column displays that list
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
                        showFileInfoDialog.value = true
                        fileSelectedToGetInfo.value = item.file
                    },
                    modifier = Modifier
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    // * The CustomBlock is the composable which give the box of the files in the
                    //          lazycolumn, it returns the mutableState of progress bar component
                    //          of the CustomBlock to modify it from outside
                    val result = CustomBlock(
                        shareUnit = item,
                    ) {
                        nsdShareViewModel._history.setValue(nsdShareViewModel._history.value!!.minus(item))
                    }

                    item.progress = result.progress
                    item.progressAmount = result.progressAmount
                }
            }
        }

        // Last Column which displays the PICK FILE Button
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
                    // Sets selectFile MutableState to true, which inturn triggers SelectFile
                    //          Composable which has this variable as observer
                    selectFile.value = true
                    log(Tag.INFO, "isConnected = ${isConnectedObserver.value}")
                }
            ) {
                Text(
                    text = "PICK FILE",
//                    style = TextStyle(fontWeight = FontWeight.Bold)
                style = TextStyle()
                )
            }
        }


        // Calling CustomFileInfoDialog with specifications, so we can show it with 'showFileInfoDialog'
        //          It can be called by changing showFileInfoDialog MutableState
        CustomFileInfoDialog(
            showFileInfoDialog = showFileInfoDialog,
            title = "FILE INFO",
        ) {
            val fileSizeInBytes = fileSelectedToGetInfo.value!!.length()

            val fileSizeInKilobytes = fileSizeInBytes / 1024.0
            val fileSizeInMegabytes = fileSizeInKilobytes / 1024.0
            val fileSizeInGigabytes = fileSizeInMegabytes / 1024.0
            val fileSizeMBformatted = String.format("%.2f", fileSizeInMegabytes)
            val fileSizeGBformatted = String.format("%.2f", fileSizeInGigabytes)
            Text(text = "Name\t:\t${fileSelectedToGetInfo.value!!.name}", modifier = Modifier.padding(vertical = 5.dp))
            Text(text = "Type\t:\t${ if (fileSelectedToGetInfo.value!!.isFile) "FILE" else "DIRECTORY"}", modifier = Modifier.padding(vertical = 5.dp))
            Text(text = "Size\t:\t${if (fileSizeInMegabytes > 999) "$fileSizeGBformatted GB" else "$fileSizeMBformatted MB"}", modifier = Modifier.padding(vertical = 5.dp))
        }

        // Calling CustomDeviceDiscoveryDialog with specifications, so we can show it with 'showDeviceDiscoveryDialog'
        //          It can be called by changing showDeviceDiscoveryDialog MutableState
        CustomDeviceDiscoveryDialog(
            showDeviceDiscoveryDialog = showDeviceDiscoveryDialog,
            title = "Devices On Network",
            composable = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(400.dp)
                ) {
                    items(discoverDeviceListObserver.value!!.toList()) { item->
                        var _showProgressIndicator = remember { MutableLiveData(false) }
                        val showProgressIndicatorObserver = _showProgressIndicator.observeAsState()
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(5.dp),
                            onClick = {
                                // If List Not Empty, then only it is meaningful to select a device from list to send files
                                if (nsdShareViewModel.history.value!!.isNotEmpty()) {
                                    val asyncFileSender = AsyncFileSender(item.host, item.port)
                                    // Start showing progress bar beside the Device Name
                                    _showProgressIndicator.value = true
                                    CoroutineScope(Dispatchers.IO).launch {
                                        for (file in nsdShareViewModel.history.value!!.toList()) {
                                            // As well change file progress bar value accordingly
                                            file.progress.value = true
                                            log(Tag.INFO, "Files in history ${file.file.name}")
                                            asyncFileSender.sendFile(file)
                                            file.progress.value = false
                                        }
                                        // Turn progress bar off when file transfer is complete
                                        _showProgressIndicator.postValue(false)
                                    }
                                } else {
                                    _toastTxt.value = "Please Select A File First"
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.height(28.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    // "dName" is the attribute which is send along with register request which
                                    // can be used to show the device name of the Device
                                    text = String(item.attributes.get("dName")!!, StandardCharsets.UTF_8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                if (showProgressIndicatorObserver.value == true) {
                                    Spacer(modifier = Modifier.width(16.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .width(19.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.background
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        // SelectFile Composable to Select File from File Manager
        SelectFile(context = context,selectFile = selectFile, nsdShareViewModel = nsdShareViewModel)

        // Ask for download permission dialog
        AcceptDownload(
            acceptDownloadResponse = nsdShareViewModel.askForDownloadResponse,
            showAcceptDownloadDialog = showAskForPermissionDialogObserver as MutableState<Boolean>,
            filename = nsdShareViewModel.incomingFileName.observeAsState()
        )
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

                    // As file is received now update the _history list with the same file
                    nsdShareViewModel._history.setValue(nsdShareViewModel._history.value?.plus(listOf(
                        ShareUnit(file.absolutePath.toHashSet().toString(),file, mutableStateOf(false), MutableLiveData(0))
                    )))
                    log(Tag.INFO, "AB = ${file.path}")
                    // Turn off selectFile
                    selectFile.value = false
                }
            }
        }
    )
    if (selectFile.value) {
        DisposableEffect(Unit) {
            launcher.launch("*/*")
            onDispose {
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
