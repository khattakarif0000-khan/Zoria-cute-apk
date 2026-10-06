package com.example.zoria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZoriaCardBorder
import com.example.ui.theme.ZoriaPrimary
import com.example.ui.theme.ZoriaSecondary
import com.example.ui.theme.ZoriaSurfaceVariant
import com.example.ui.theme.ZoriaTertiary
import com.example.ui.theme.ZoriaTextMuted
import com.example.ui.theme.ZoriaTextPrimary
import com.example.zoria.model.ChatMessage
import com.example.zoria.model.MessageSender
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationMessageItem(
    message: ChatMessage,
    onPlayAudio: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) ZoriaSurfaceVariant else Color(0xFF141E33)
    val borderColor = if (isUser) ZoriaCardBorder else ZoriaPrimary.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 12.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                // ZORIA Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ZoriaPrimary)
                        .testTag("zoria_message_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "ZORIA",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Message Bubble
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = bubbleColor,
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                    .width(androidx.compose.ui.unit.Dp.Unspecified)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Header with name and tags
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isUser) "آپ (You)" else "زوریا (ZORIA)",
                            color = if (isUser) ZoriaSecondary else ZoriaTertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (message.isSimplifiedRepeat) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ZoriaSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "آسان وضاحت",
                                    color = ZoriaSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Main Text
                    Text(
                        text = message.text,
                        color = ZoriaTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Footer with timestamp & audio action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatTime(message.timestamp),
                            color = ZoriaTextMuted,
                            fontSize = 10.sp
                        )

                        if (!message.audioBase64.isNullOrEmpty() && onPlayAudio != null) {
                            IconButton(
                                onClick = { onPlayAudio(message.audioBase64) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("play_message_audio_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "آڈیو سنیں",
                                    tint = ZoriaSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ZoriaSurfaceVariant)
                        .testTag("user_message_avatar"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = ZoriaTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMillis: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}
