package com.example.zoria.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.StateErrorColor
import com.example.ui.theme.StateIdleColor
import com.example.ui.theme.StateListeningColor
import com.example.ui.theme.StateProcessingColor
import com.example.ui.theme.StateSpeakingColor
import com.example.ui.theme.ZoriaBackground
import com.example.ui.theme.ZoriaCardBorder
import com.example.ui.theme.ZoriaPrimary
import com.example.ui.theme.ZoriaSecondary
import com.example.ui.theme.ZoriaSurface
import com.example.ui.theme.ZoriaSurfaceVariant
import com.example.ui.theme.ZoriaTertiary
import com.example.ui.theme.ZoriaTextMuted
import com.example.ui.theme.ZoriaTextPrimary
import com.example.ui.theme.ZoriaTextSecondary
import com.example.zoria.integration.CompanionMode
import com.example.zoria.model.AssistantState
import com.example.zoria.model.ZoriaVoice

@Composable
fun ZoriaScreen(
    viewModel: ZoriaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assistantState by viewModel.assistantState.collectAsState()
    val rmsDecibels by viewModel.rmsDecibels.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isContinuous by viewModel.isContinuousConversationEnabled.collectAsState()
    val isBackgroundActive by viewModel.isBackgroundServiceActive.collectAsState()
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasApiKey by viewModel.hasConfiguredApiKey.collectAsState()
    val isMicGranted by viewModel.isMicPermissionGranted.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Permission launcher for microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setMicPermissionGranted(granted)
        if (granted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicPermissionGranted(hasMic)
        viewModel.checkPermissions()
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ZoriaBackground)
            .imePadding(),
        containerColor = ZoriaBackground,
        topBar = {
            ZoriaTopBar(
                assistantState = assistantState,
                selectedVoice = selectedVoice,
                selectedMode = selectedMode,
                hasApiKey = hasApiKey,
                onSettingsClick = { showSettingsDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 680.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Central Orb & State Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ZoriaOrbVisualizer(
                        state = assistantState,
                        rmsDecibels = rmsDecibels,
                        modifier = Modifier.padding(4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistantStateBadge(state = assistantState)
                        Spacer(modifier = Modifier.width(8.dp))
                        ModeBadgePill(mode = selectedMode)
                    }
                }
            }

            // Error banner if any
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StateErrorColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, StateErrorColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .testTag("error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = StateErrorColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = StateErrorColor,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Conversation Messages Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ConversationMessageItem(message = message)
                }
            }

            // Quick Action Prompt Chips (Repeat, WhatsApp, Camera, YouTube, Flashlight)
            QuickActionsCarousel(
                onRepeatClick = { viewModel.requestRepeatOrSimplify() },
                onWhatsAppClick = { viewModel.sendTextMessage("ZORIA WhatsApp kholo") },
                onCameraClick = { viewModel.sendTextMessage("ZORIA camera kholo") },
                onTorchClick = { viewModel.sendTextMessage("ZORIA torch on karo") },
                onYouTubeClick = { viewModel.sendTextMessage("ZORIA YouTube kholo") }
            )

            // Primary Voice & Stop Control Bar
            ZoriaVoiceControls(
                state = assistantState,
                onMicClick = {
                    if (isMicGranted) {
                        if (assistantState == AssistantState.SPEAKING) {
                            // Barge-in: cut off speech and start listening
                            viewModel.startListeningWithBargeIn()
                        } else if (assistantState == AssistantState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            viewModel.startListening()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopClick = { viewModel.stopAll() },
                textInput = textInput,
                onTextInputChange = { textInput = it },
                onSendText = {
                    viewModel.sendTextMessage(textInput)
                    textInput = ""
                }
            )
        }
    }

    if (showSettingsDialog) {
        ZoriaSettingsDialog(
            selectedVoice = selectedVoice,
            onVoiceSelected = { viewModel.setVoice(it) },
            selectedMode = selectedMode,
            onModeSelected = { viewModel.setMode(it) },
            isContinuousEnabled = isContinuous,
            onToggleContinuous = { viewModel.toggleContinuousConversation() },
            isBackgroundActive = isBackgroundActive,
            onToggleBackground = { viewModel.toggleBackgroundService() },
            isNotificationAccessGranted = isNotificationAccessGranted,
            memories = memories,
            onDeleteMemory = { viewModel.deleteMemory(it) },
            onClearAllMemories = { viewModel.clearAllMemories() },
            hasApiKey = hasApiKey,
            onSaveApiKey = { viewModel.saveCustomApiKey(it) },
            onClearHistory = {
                viewModel.clearHistory()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun ZoriaTopBar(
    assistantState: AssistantState,
    selectedVoice: ZoriaVoice,
    selectedMode: CompanionMode,
    hasApiKey: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ZoriaSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(0.5.dp, ZoriaCardBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ZORIA Brand Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ZoriaPrimary, ZoriaSecondary)
                            )
                        )
                        .testTag("app_brand_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ZORIA",
                        color = ZoriaTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = selectedMode.titleUrdu,
                        color = ZoriaTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Voice pill & Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ZoriaSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = ZoriaSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedVoice.voiceName,
                            color = ZoriaSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "ترتیبات",
                        tint = if (hasApiKey) ZoriaTextSecondary else StateErrorColor
                    )
                }
            }
        }
    }
}

@Composable
fun ModeBadgePill(mode: CompanionMode) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ZoriaTertiary.copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, ZoriaTertiary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
    ) {
        Text(
            text = mode.titleUrdu,
            color = ZoriaTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun QuickActionsCarousel(
    onRepeatClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onCameraClick: () -> Unit,
    onTorchClick: () -> Unit,
    onYouTubeClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            QuickChip(label = "دوبارہ بتاؤ (Repeat)", icon = Icons.Default.Refresh, onClick = onRepeatClick)
        }
        item {
            QuickChip(label = "WhatsApp کھولو", icon = Icons.Default.PlayArrow, onClick = onWhatsAppClick)
        }
        item {
            QuickChip(label = "کیمرہ کھولو", icon = Icons.Default.CameraAlt, onClick = onCameraClick)
        }
        item {
            QuickChip(label = "ٹارچ جلاؤ", icon = Icons.Default.FlashlightOn, onClick = onTorchClick)
        }
        item {
            QuickChip(label = "یوٹیوب کھولو", icon = Icons.Default.PlayArrow, onClick = onYouTubeClick)
        }
    }
}

@Composable
fun QuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ZoriaSurfaceVariant,
        modifier = Modifier
            .border(1.dp, ZoriaCardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = ZoriaSecondary, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = label, color = ZoriaTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
