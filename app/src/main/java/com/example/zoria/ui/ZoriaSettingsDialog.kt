package com.example.zoria.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StateErrorColor
import com.example.ui.theme.StateListeningColor
import com.example.ui.theme.ZoriaCardBorder
import com.example.ui.theme.ZoriaPrimary
import com.example.ui.theme.ZoriaSecondary
import com.example.ui.theme.ZoriaSurface
import com.example.ui.theme.ZoriaSurfaceVariant
import com.example.ui.theme.ZoriaTertiary
import com.example.ui.theme.ZoriaTextMuted
import com.example.ui.theme.ZoriaTextPrimary
import com.example.ui.theme.ZoriaTextSecondary
import com.example.zoria.data.local.MemoryEntity
import com.example.zoria.integration.CompanionMode
import com.example.zoria.model.ZoriaVoice
import com.example.zoria.service.ZoriaNotificationListenerService

@Composable
fun ZoriaSettingsDialog(
    selectedVoice: ZoriaVoice,
    onVoiceSelected: (ZoriaVoice) -> Unit,
    selectedMode: CompanionMode,
    onModeSelected: (CompanionMode) -> Unit,
    isContinuousEnabled: Boolean,
    onToggleContinuous: () -> Unit,
    isBackgroundActive: Boolean,
    onToggleBackground: () -> Unit,
    isNotificationAccessGranted: Boolean,
    memories: List<MemoryEntity>,
    onDeleteMemory: (String) -> Unit,
    onClearAllMemories: () -> Unit,
    hasApiKey: Boolean,
    onSaveApiKey: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("عام (General)", "موڈ (Modes)", "یادداشت (Memory)")

    var keyInput by remember { mutableStateOf("") }
    var showKeyEditor by remember { mutableStateOf(!hasApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("zoria_settings_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = ZoriaSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ZORIA ترتیبات اور موڈ",
                    color = ZoriaTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بند کریں", tint = ZoriaTextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ZoriaSurfaceVariant,
                    contentColor = ZoriaSecondary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ZoriaSecondary
                        )
                    }
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> GeneralSettingsTab(
                        hasApiKey = hasApiKey,
                        showKeyEditor = showKeyEditor,
                        keyInput = keyInput,
                        onKeyInputChange = { keyInput = it },
                        onSaveKey = {
                            onSaveApiKey(keyInput)
                            showKeyEditor = false
                        },
                        onShowKeyEditor = { showKeyEditor = true },
                        selectedVoice = selectedVoice,
                        onVoiceSelected = onVoiceSelected,
                        isContinuousEnabled = isContinuousEnabled,
                        onToggleContinuous = onToggleContinuous,
                        isBackgroundActive = isBackgroundActive,
                        onToggleBackground = onToggleBackground,
                        isNotificationAccessGranted = isNotificationAccessGranted,
                        onOpenNotificationSettings = {
                            context.startActivity(ZoriaNotificationListenerService.getNotificationSettingsIntent())
                        },
                        onClearHistory = onClearHistory
                    )

                    1 -> PersonalityModesTab(
                        selectedMode = selectedMode,
                        onModeSelected = onModeSelected
                    )

                    2 -> MemoryManagerTab(
                        memories = memories,
                        onDeleteMemory = onDeleteMemory,
                        onClearAllMemories = onClearAllMemories
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ZoriaPrimary),
                modifier = Modifier.testTag("settings_done_btn")
            ) {
                Text("مکمل (Done)")
            }
        }
    )
}

@Composable
fun GeneralSettingsTab(
    hasApiKey: Boolean,
    showKeyEditor: Boolean,
    keyInput: String,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onShowKeyEditor: () -> Unit,
    selectedVoice: ZoriaVoice,
    onVoiceSelected: (ZoriaVoice) -> Unit,
    isContinuousEnabled: Boolean,
    onToggleContinuous: () -> Unit,
    isBackgroundActive: Boolean,
    onToggleBackground: () -> Unit,
    isNotificationAccessGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column {
        // API Key Section
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (hasApiKey) StateListeningColor.copy(alpha = 0.12f) else StateErrorColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth().border(1.dp, if (hasApiKey) StateListeningColor else StateErrorColor, RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasApiKey) Icons.Default.Check else Icons.Default.Key,
                    contentDescription = null,
                    tint = if (hasApiKey) StateListeningColor else StateErrorColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasApiKey) "Gemini API Key فعال ہے" else "Gemini API Key درکار ہے",
                        color = if (hasApiKey) StateListeningColor else StateErrorColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onShowKeyEditor) {
                    Text("ترمیم", color = ZoriaSecondary, fontSize = 11.sp)
                }
            }
        }

        if (showKeyEditor) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = keyInput,
                onValueChange = onKeyInputChange,
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZoriaPrimary,
                    unfocusedBorderColor = ZoriaCardBorder,
                    focusedTextColor = ZoriaTextPrimary,
                    unfocusedTextColor = ZoriaTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onSaveKey,
                colors = ButtonDefaults.buttonColors(containerColor = ZoriaPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("محفوظ کریں")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Continuous Conversation Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("مسلسل گفتگو (Continuous Flow)", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("بولنے کے بعد خودکار سننا (Speak → Listen)", color = ZoriaTextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = isContinuousEnabled,
                onCheckedChange = { onToggleContinuous() },
                colors = SwitchDefaults.colors(checkedThumbColor = ZoriaSecondary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Background Foreground Service Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("پس منظر معاون (Background Service)", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("بیک گراؤنڈ میں دستیاب رہنا", color = ZoriaTextMuted, fontSize = 10.sp)
            }
            Switch(
                checked = isBackgroundActive,
                onCheckedChange = { onToggleBackground() },
                colors = SwitchDefaults.colors(checkedThumbColor = ZoriaPrimary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notification Listener Assistant Permission
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ZoriaSurfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable { onOpenNotificationSettings() }
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = ZoriaTertiary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("نوٹیفکیشن اسسٹنٹ (Notification Access)", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        text = if (isNotificationAccessGranted) "اجازت فعال ہے (WhatsApp وغیرہ کا اعلان)" else "اجازت درکار ہے - سیٹنگز کھولنے کے لیے ٹیپ کریں",
                        color = if (isNotificationAccessGranted) StateListeningColor else ZoriaTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Voice Selection
        Text("Gemini آواز کا انتخاب:", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        ZoriaVoice.values().forEach { voice ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onVoiceSelected(voice) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selectedVoice == voice, onClick = { onVoiceSelected(voice) }, colors = RadioButtonDefaults.colors(selectedColor = ZoriaPrimary))
                Text(voice.displayNameUrdu, color = ZoriaTextPrimary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onClearHistory) {
            Text("گفتگو صاف کریں (Clear History)", color = StateErrorColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun PersonalityModesTab(
    selectedMode: CompanionMode,
    onModeSelected: (CompanionMode) -> Unit
) {
    Column {
        Text("زوریا کا موڈ منتخب کریں (ZORIA Persona):", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        CompanionMode.values().forEach { mode ->
            val isSelected = selectedMode == mode
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ZoriaSurfaceVariant else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, if (isSelected) ZoriaTertiary else ZoriaCardBorder, RoundedCornerShape(12.dp))
                    .clickable { onModeSelected(mode) }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = ZoriaTertiary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${mode.titleUrdu} (${mode.titleEnglish})",
                            color = if (isSelected) ZoriaTertiary else ZoriaTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(text = mode.iconDesc, color = ZoriaTextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryManagerTab(
    memories: List<MemoryEntity>,
    onDeleteMemory: (String) -> Unit,
    onClearAllMemories: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Boss کی یادداشتیں (Memories):", color = ZoriaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (memories.isNotEmpty()) {
                TextButton(onClick = onClearAllMemories) {
                    Text("سب صاف کریں", color = StateErrorColor, fontSize = 11.sp)
                }
            }
        }

        if (memories.isEmpty()) {
            Text(
                "ابھی کوئی یادداشت محفوظ نہیں ہوئی۔ جب آپ گفتگو میں اپنی پسند یا نام بتائیں گے تو زوریا اسے یاد رکھے گی۔",
                color = ZoriaTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(memories, key = { it.id }) { mem ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ZoriaSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = ZoriaSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.value, color = ZoriaTextPrimary, fontSize = 12.sp)
                                Text(mem.key, color = ZoriaTextMuted, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onDeleteMemory(mem.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف کریں", tint = StateErrorColor, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
