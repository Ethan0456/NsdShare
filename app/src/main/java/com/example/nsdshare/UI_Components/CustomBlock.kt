package com.example.nsdshare.UI_Components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    shareUnit: ShareUnit
): MutableState<Boolean> {
    val scrollableState = rememberScrollState()
    val showProgressBar = remember { shareUnit.progress }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .heightIn(max = 40.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
    ) {
        Text(
            text = shareUnit.file.name,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.weight(9f).horizontalScroll(state = scrollableState),
            textAlign = TextAlign.Left
        )
        if (showProgressBar.value) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 5.dp, start = 10.dp)
                    .width(19.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.background
            )
        }
    }
    return showProgressBar
}