package com.example.nsdshare.UI_Components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsdshare.ShareUnit

@Composable
fun CustomBlock(
    shareUnit: ShareUnit,
    deleteCallback: () -> Unit
): MutableState<Boolean> {
    val scrollableState = rememberScrollState()
    val showProgressBar = remember { shareUnit.progress }

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
        if (showProgressBar.value) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(top = 9.dp,start = 4.dp, end = 4.dp)
                    .width(19.dp)
                    .clickable {  }
                    .weight(1f),
                strokeWidth = 2.dp
            )
        }
        IconButton(
            modifier = Modifier.weight(1f),
            onClick = {
            deleteCallback()
        }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete From List")
        }
    }
    return showProgressBar
}