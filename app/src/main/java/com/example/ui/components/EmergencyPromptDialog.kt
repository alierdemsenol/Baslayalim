package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.TranslationHelper

@Composable
fun EmergencyPromptDialog(
    lang: String = "TR",
    onConfirmEmergency: () -> Unit,
    onConfirmNormal: () -> Unit,
    onAbandonFirar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = TranslationHelper.getTranslation("emergency_title", lang),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = TranslationHelper.getTranslation("emergency_body", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TranslationHelper.getTranslation("emergency_warn", lang),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onConfirmNormal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_continue_btn"),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(TranslationHelper.getTranslation("emergency_return_btn", lang))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onConfirmEmergency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_yes_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(TranslationHelper.getTranslation("emergency_yes_btn", lang))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onAbandonFirar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_firar_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(TranslationHelper.getTranslation("emergency_firar_btn", lang))
                }
            }
        },
        dismissButton = null
    )
}
