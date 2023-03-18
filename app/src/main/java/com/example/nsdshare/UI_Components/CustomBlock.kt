package com.example.nsdshare.UI_Components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsdshare.NsdShareViewModel
import com.example.nsdshare.ShareUnit

@Composable
fun CustomBlock(
    shareUnit: ShareUnit,
    deleteCallback: () -> Unit
): CustomBlockResult {
    val scrollableState = rememberScrollState()
    val showProgressBar = remember { shareUnit.progress }
    val progressAmountObserver = remember { shareUnit.progressAmount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(9.dp)
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp)
                .heightIn(max = 40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shareUnit.file.name,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .weight(8f)
                    .horizontalScroll(state = scrollableState),
                textAlign = TextAlign.Left
            )
            IconButton(
                modifier = Modifier.weight(2f),
                onClick = {
                    deleteCallback()
                }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete From List")
            }
        }
        if (showProgressBar.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(9.dp)
                    .heightIn(max = 40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val animatedProgress = animateFloatAsState(
                    targetValue = progressAmountObserver.observeAsState().value!!.toFloat()/100,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                ).value

                LinearProgressIndicator(
                    progress = animatedProgress,
                    color = MaterialTheme.colorScheme.background,
                    trackColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .width(200.dp)
                        .padding(top = 9.dp, start = 4.dp, end = 4.dp)
                )
                Text(
                    text = progressAmountObserver.observeAsState().value.toString() + "%",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier
                        .weight(1f)
                        .padding(10.dp)
                        .horizontalScroll(state = scrollableState),
                    textAlign = TextAlign.Left
                )
            }
        }
    }
    return CustomBlockResult(showProgressBar, progressAmountObserver)
}