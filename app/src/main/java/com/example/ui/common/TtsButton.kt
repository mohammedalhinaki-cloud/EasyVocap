package com.example.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TtsButton(
    textToSpeak: String,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    FilledTonalIconButton(
        onClick = { onSpeak(textToSpeak) },
        modifier = modifier
            .size(size)
            .testTag("btn_speak_${textToSpeak.take(10)}"),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint
        )
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "استماع للنطق الأمريكي",
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
