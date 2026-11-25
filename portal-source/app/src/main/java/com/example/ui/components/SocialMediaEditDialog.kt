package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.SocialMediaLink
import com.example.ui.theme.FlagGreen700
import com.example.ui.theme.RoyalBlue900

data class SocialPreset(
    val key: String,
    val nameBn: String,
    val nameEn: String,
    val defaultUrl: String,
    val iconType: String
)

@Composable
fun SocialMediaEditDialog(
    link: SocialMediaLink,
    isNew: Boolean,
    language: AppLanguage,
    onSave: (SocialMediaLink) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameBn by remember { mutableStateOf(link.platformNameBn) }
    var nameEn by remember { mutableStateOf(link.platformNameEn) }
    var url by remember { mutableStateOf(link.url) }
    var iconType by remember { mutableStateOf(link.iconType.ifBlank { "FACEBOOK" }) }
    var order by remember { mutableStateOf(link.order.toString()) }

    val presets = listOf(
        SocialPreset("FACEBOOK", "ফেসবুক পেজ", "Facebook Page", "https://facebook.com/asheq.ullah.rafiq.mp", "FACEBOOK"),
        SocialPreset("YOUTUBE", "ইউটিউব চ্যানেল", "YouTube Channel", "https://youtube.com/@ashequllahrafiq", "YOUTUBE"),
        SocialPreset("WHATSAPP", "হোয়াটসঅ্যাপ হেল্পলাইন", "WhatsApp Helpline", "https://wa.me/8801819000000", "WHATSAPP"),
        SocialPreset("TWITTER", "এক্স (টুইটার)", "X (Twitter)", "https://twitter.com/AsheqRafiqMP", "TWITTER"),
        SocialPreset("INSTAGRAM", "ইনস্টাগ্রাম", "Instagram", "https://instagram.com/asheq_rafiq_mp", "INSTAGRAM"),
        SocialPreset("LINKEDIN", "লিংকডইন", "LinkedIn", "https://linkedin.com/in/ashequllahrafiq", "LINKEDIN"),
        SocialPreset("WEBSITE", "অফিসিয়াল ওয়েবসাইট", "Official Website", "https://ashequllahrafiq.com", "WEBSITE")
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isNew) {
                        if (language == AppLanguage.BN) "নতুন সোশ্যাল মিডিয়া চ্যানেল যোগ করুন" else "Add New Social Channel"
                    } else {
                        if (language == AppLanguage.BN) "সোশ্যাল মিডিয়া চ্যানেল সম্পাদনা" else "Edit Social Channel"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                // Quick Presets
                Column {
                    Text(
                        text = if (language == AppLanguage.BN) "প্ল্যাটফর্ম প্রিসেট নির্বাচন করুন:" else "Select Platform Preset:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { preset ->
                            val isSelected = iconType == preset.key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    iconType = preset.key
                                    if (nameBn.isBlank() || presets.any { it.nameBn == nameBn }) nameBn = preset.nameBn
                                    if (nameEn.isBlank() || presets.any { it.nameEn == nameEn }) nameEn = preset.nameEn
                                    if (url.isBlank() || presets.any { it.defaultUrl == url }) url = preset.defaultUrl
                                },
                                label = {
                                    Text(
                                        text = if (language == AppLanguage.BN) preset.nameBn else preset.nameEn,
                                        fontSize = 11.5.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FlagGreen700,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text(if (language == AppLanguage.BN) "প্ল্যাটফর্মের নাম (বাংলা)" else "Platform Name (BN)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (language == AppLanguage.BN) "প্ল্যাটফর্মের নাম (ইংরেজি)" else "Platform Name (EN)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(if (language == AppLanguage.BN) "ওয়েব বা সোশ্যাল লিংক URL" else "Social / Web Link URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (language == AppLanguage.BN) "প্রদর্শনের ক্রম (Order)" else "Display Order (e.g. 1, 2, 3)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }

                    Button(
                        onClick = {
                            if (url.isBlank()) {
                                Toast.makeText(context, if (language == AppLanguage.BN) "লিংক URL পূরণ করুন" else "Please enter URL", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val finalNameBn = nameBn.ifBlank { nameEn.ifBlank { "সোশ্যাল লিংক" } }
                            val finalNameEn = nameEn.ifBlank { nameBn.ifBlank { "Social Link" } }
                            val finalOrder = order.toIntOrNull() ?: 0

                            val updated = link.copy(
                                platformNameBn = finalNameBn.trim(),
                                platformNameEn = finalNameEn.trim(),
                                url = url.trim(),
                                iconType = iconType,
                                order = finalOrder
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlagGreen700),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}
