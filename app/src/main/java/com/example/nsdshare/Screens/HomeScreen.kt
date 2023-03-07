package com.example.nsdshare.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.UI_Components.CustomDialog

@Composable
fun HomeScreen(
    nsdShareViewModel: NsdShareViewModel,
    navHostController: NavHostController
) {
    val connectionStatus: State<String> = nsdShareViewModel.connectionStatus.observeAsState("")
    val historyObs = nsdShareViewModel.history.observeAsState()
    val isResolvedObserver = nsdShareViewModel.nsdHelper.isResolved.observeAsState()
    val discoverDeviceListObserver = nsdShareViewModel.nsdHelper.ls.observeAsState()
    var isServiceRunning = nsdShareViewModel.nsdHelper.isServiceRunning.observeAsState()
    var deviceNameObserver = nsdShareViewModel.deviceName.observeAsState("")

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
            modifier = Modifier.weight(9f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(historyObs.value!!.size) { item->
                Button(onClick = {}) {
                    Text(text = item.toString())
                }
            }
        }

        var showDialog = remember { mutableStateOf<Boolean>(false) }
        Surface(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .weight(2f),
        ) {
            Column(
                modifier = Modifier.wrapContentHeight().fillMaxWidth()
            ) {
                TextField(
                    value = deviceNameObserver.value,
                    onValueChange = { nsdShareViewModel.onDeviceNameChange(it) }
                )

                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = {
                        nsdShareViewModel.changeConnectionStatus("Waiting...")
                        if (!isServiceRunning.value!!) {
                            nsdShareViewModel.startNsdService()
                        }
                        showDialog.value = true
                        if (isResolvedObserver.value == true) {
                            nsdShareViewModel.changeConnectionStatus("Connected")
                        }
                    }) {
                        Text(text = "CONNECT")
                    }
//                Button(onClick = {
//                    nsdShareViewModel.changeConnectionStatus("Discovering...")
//                    if (!isServiceRunning.value!!) {
//                        nsdShareViewModel.startNsdService()
//                    }
//                    showDialog.value = true
//                    if (isResolvedObserver.value == true) {
//                        nsdShareViewModel.changeConnectionStatus("Connected")
//                    }
//                }) {
//                    Text(text = "RECEIVE")
//                }
                }

            }
        }
        CustomDialog(
            showDialog = showDialog,
            title = "Devices On Network",
            composable = {
                LazyColumn(
                    modifier = Modifier
                        .height(500.dp)
                        .width(350.dp)
                ) {
//                    if (!discoverDeviceListObserver.value!!.isEmpty()) {
                        items(discoverDeviceListObserver.value!!.toList()) { item->
                            Button(onClick = {
//                                nsdShareViewModel.nsdHelper.onClickResolve(item)
                            }) {
//                                if (item.serviceName != null) {
                                    Text(text = item.serviceName)
//                                }
                            }
//                        }
                    }
                }
            }
        )
    }
}