package com.example.nsdshare.UI_Components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.MutableLiveData

@Composable
fun AcceptDownload(
    acceptDownloadResponse: MutableLiveData<Int>,
    showAcceptDownloadDialog: MutableState<Boolean>,
    title: String = "Accept Download?",
    filename: State<String?>
) {
    val scrollState = rememberScrollState()

    if (showAcceptDownloadDialog.value) {
        Dialog(onDismissRequest = {
            showAcceptDownloadDialog.value = false
        }) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .wrapContentHeight()
                    .heightIn(min = 150.dp, max = 400.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth()
                            .align(alignment = Alignment.CenterHorizontally),
                        style = TextStyle(
                            fontSize = 20.sp
                        )
                    )
                    Text(
                        text = "File : ${filename.value}",
                        maxLines = 5,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.verticalScroll(scrollState)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    ) {
                        Button(onClick = {
                            acceptDownloadResponse.value = 0
                            showAcceptDownloadDialog.value = false
                        }, modifier = Modifier.weight(1f).padding(end = 5.dp)) {
                            Text(text = "Reject")
                        }
                        Button(onClick = {
                            acceptDownloadResponse.value = 1
                            showAcceptDownloadDialog.value = false
                        }, modifier = Modifier.weight(1f).padding(end = 5.dp)) {
                            Text(text = "Accept")
                        }
                    }
                }
            }
        }
    }
}