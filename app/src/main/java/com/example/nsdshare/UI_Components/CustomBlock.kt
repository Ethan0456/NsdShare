package com.example.nsdshare.UI_Components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CustomBlock(
    fileName: String,
//    fileStatus: String
) {
    val scrollableState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .heightIn(max = 40.dp)
            .verticalScroll(state = scrollableState)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)),
    ) {
        Text(
            text = fileName,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Left
        )
//        Text(text = fileStatus)
    }
}