package de.hipp.app.taskcards.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.BrandPurple
import de.hipp.app.taskcards.ui.theme.Dimensions

/**
 * Dialog for sharing a list via QR code or deep link URL.
 * Shows QR code and provides options to copy URL or share via Android share sheet.
 */
@Composable
fun ShareDialog(
    shareUrl: String?,
    qrCodeBitmap: Bitmap?,
    isGeneratingQR: Boolean,
    onGenerateQR: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.share_task_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // QR Code Section
                if (qrCodeBitmap != null) {
                    Surface(
                        modifier = Modifier.size(250.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrCodeBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_code_description),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.qr_code_scan_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isGeneratingQR) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.qr_code_generating),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Show button to generate QR
                    Button(
                        onClick = onGenerateQR,
                        modifier = Modifier
                            .fillMaxWidth()
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            )
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = stringResource(R.string.generate_qr_code))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.generate_qr_code))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy URL button
                    Button(
                        onClick = {
                            shareUrl?.let { url ->
                                copyToClipboard(context, url)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            ),
                        enabled = shareUrl != null
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.share_copy_link), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.share_copy_link))
                    }

                    // Share via Android share sheet
                    Button(
                        onClick = {
                            shareUrl?.let { url ->
                                shareViaAndroid(context, url)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            ),
                        enabled = shareUrl != null
                    ) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_button), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.share_button))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.sizeIn(
                    minWidth = Dimensions.MinTouchTarget,
                    minHeight = Dimensions.MinTouchTarget
                )
            ) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Copy text to clipboard and show a toast notification.
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Task List Share URL", text)
    clipboard.setPrimaryClip(clip)

    android.widget.Toast.makeText(
        context,
        context.getString(R.string.share_link_copied),
        android.widget.Toast.LENGTH_SHORT
    ).show()
}

/**
 * Share text via Android share sheet.
 */
private fun shareViaAndroid(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_TITLE, context.getString(R.string.share_task_title))
    }

    val chooser = Intent.createChooser(intent, context.getString(R.string.share_via_system))
    context.startActivity(chooser)
}
