package com.example.nsdshare.UI_Components

import androidx.compose.animation.AnimatedContentScope.SlideDirection.Companion.Start
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File


@Composable
fun CustomFileInfoDialog(
    showFileInfoDialog: MutableState<Boolean>,
    title: String,
    composable: @Composable () -> Unit,
) {
    val scrollableState = rememberScrollState()
    if (showFileInfoDialog.value) {
        Dialog(
            onDismissRequest = { showFileInfoDialog.value = false }
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .wrapContentHeight()
                    .heightIn(min = 150.dp, max = 400.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .verticalScroll(scrollableState),
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = title,
                        modifier = Modifier
                            .padding(20.dp).fillMaxWidth().align(alignment = Alignment.CenterHorizontally),
                        style = TextStyle(
                            fontSize = 20.sp
                        )
                    )
                    composable()
                }
            }
        }
    }
}