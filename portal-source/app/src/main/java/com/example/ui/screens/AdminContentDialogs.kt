package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.RoyalBlue900

@Composable
fun DigitalServiceEditDialog(
    item: DigitalServiceItem?,
    isNew: Boolean,
    onSave: (DigitalServiceItem) -> Unit,
    onDismiss: () -> Unit,
    language: AppLanguage
) {
    var titleBn by remember { mutableStateOf(item?.titleBn ?: "") }
    var titleEn by remember { mutableStateOf(item?.titleEn ?: "") }
    var subTextBn by remember { mutableStateOf(item?.subTextBn ?: "") }
    var subTextEn by remember { mutableStateOf(item?.subTextEn ?: "") }
    var linkUrl by remember { mutableStateOf(item?.linkUrl ?: "") }
    var actionType by remember { mutableStateOf(item?.actionType ?: "WEB_BROWSER") }
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight().testTag("service_edit_dialog")) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = if (language == AppLanguage.BN) "সেবা তথ্য ও পোর্টাল লিঙ্ক সম্পাদনা" else "Edit Service & Portal Link", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)
                
                OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("সেবার নাম (বাংলা)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Service Title (English)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subTextBn, onValueChange = { subTextBn = it }, label = { Text("সংক্ষিপ্ত বিবরণ (বাংলা)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("ওয়েবসাইট লিঙ্ক (যেমন: https://services.nidw.gov.bd)") }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = {
                        onSave(item?.copy(titleBn = titleBn, titleEn = titleEn, subTextBn = subTextBn, subTextEn = subTextEn, linkUrl = linkUrl, actionType = actionType) 
                            ?: DigitalServiceItem(id = "S_${System.currentTimeMillis()}", titleBn = titleBn, titleEn = titleEn, subTextBn = subTextBn, subTextEn = subTextEn, linkUrl = linkUrl, actionType = actionType))
                        onDismiss()
                    }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun DailyScheduleEditDialog(
    item: DailyScheduleItem?,
    isNew: Boolean,
    onSave: (DailyScheduleItem) -> Unit,
    onDismiss: () -> Unit,
    language: AppLanguage
) {
    var titleBn by remember { mutableStateOf(item?.eventTitleBn ?: "") }
    var titleEn by remember { mutableStateOf(item?.eventTitleEn ?: "") }
    var categoryBn by remember { mutableStateOf(item?.categoryBn?.ifEmpty { "উন্নয়ন পরিদর্শন" } ?: "উন্নয়ন পরিদর্শন") }
    var categoryEn by remember { mutableStateOf(item?.categoryEn?.ifEmpty { "Development Inspection" } ?: "Development Inspection") }
    var dateBn by remember { mutableStateOf(item?.eventDateBn ?: "") }
    var dateEn by remember { mutableStateOf(item?.eventDateEn ?: "") }
    var timeBn by remember { mutableStateOf(item?.timeBn ?: "সকাল ১০:০০ মি.") }
    var timeEn by remember { mutableStateOf(item?.timeEn ?: "10:00 AM") }
    var locationBn by remember { mutableStateOf(item?.locationBn ?: "") }
    var locationEn by remember { mutableStateOf(item?.locationEn ?: "") }
    var statusBn by remember { mutableStateOf(item?.statusBn?.ifEmpty { "নির্ধারিত" } ?: "নির্ধারিত") }
    var statusEn by remember { mutableStateOf(item?.statusEn?.ifEmpty { "Scheduled" } ?: "Scheduled") }
    
    val categoryOptions = listOf(
        Pair("উন্নয়ন পরিদর্শন", "Development Inspection"),
        Pair("গণসংযোগ সভা", "Public Meeting"),
        Pair("বিশেষ অনুষ্ঠান", "Special Event"),
        Pair("প্রশাসনিক বৈঠক", "Administrative Meeting")
    )
    
    val statusOptions = listOf(
        Pair("নির্ধারিত", "Scheduled"),
        Pair("চলমান", "In Progress"),
        Pair("সম্পন্ন", "Completed")
    )

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("schedule_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            if (isNew) "নতুন কর্মসূচি যোগ করুন" else "কর্মসূচি সম্পাদনা"
                        } else {
                            if (isNew) "Add New Schedule" else "Edit Schedule"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                // Scrollable form body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Selection
                    Text(
                        text = if (language == AppLanguage.BN) "ক্যাটাগরি নির্বাচন করুন" else "Select Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.take(2).forEach { (bn, en) ->
                            val isSelected = categoryBn == bn
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryBn = bn
                                    categoryEn = en
                                },
                                label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.drop(2).forEach { (bn, en) ->
                            val isSelected = categoryBn == bn
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryBn = bn
                                    categoryEn = en
                                },
                                label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text("কর্মসূচির শিরোনাম (বাংলা) *") },
                        modifier = Modifier.fillMaxWidth().testTag("schedule_title_bn_input"),
                        singleLine = false,
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text("Event Title (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    // Date & Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = dateBn,
                            onValueChange = { dateBn = it },
                            label = { Text("তারিখ (যেমন: ১০ নভেম্বর, ২০২৬)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = timeBn,
                            onValueChange = { timeBn = it },
                            label = { Text("সময় (যেমন: সকাল ১০:৩০ মি.)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Location / Union
                    OutlinedTextField(
                        value = locationBn,
                        onValueChange = { locationBn = it },
                        label = { Text("স্থান ও ইউনিয়ন (যেমন: মাতারবাড়ি, মহেশখালী)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status Selection
                    Text(
                        text = if (language == AppLanguage.BN) "কর্মসূচির স্ট্যাটাস" else "Event Status",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statusOptions.forEach { (bn, en) ->
                            val isSelected = statusBn == bn
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    statusBn = bn
                                    statusEn = en
                                },
                                label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            if (titleBn.isNotBlank()) {
                                val finalId = item?.id?.ifEmpty { "sched_${System.currentTimeMillis()}" }
                                    ?: "sched_${System.currentTimeMillis()}"
                                onSave(
                                    DailyScheduleItem(
                                        id = finalId,
                                        eventTitleBn = titleBn.trim(),
                                        eventTitleEn = titleEn.trim().ifEmpty { titleBn.trim() },
                                        categoryBn = categoryBn,
                                        categoryEn = categoryEn,
                                        eventDateBn = dateBn.trim().ifEmpty { "আসন্ন" },
                                        eventDateEn = dateEn.trim().ifEmpty { "Upcoming" },
                                        timeBn = timeBn.trim(),
                                        timeEn = timeEn.trim(),
                                        locationBn = locationBn.trim().ifEmpty { "মহেশখালী-কুতুবদিয়া" },
                                        locationEn = locationEn.trim().ifEmpty { "Moheshkhali-Kutubdia" },
                                        statusBn = statusBn,
                                        statusEn = statusEn,
                                        timestamp = item?.timestamp?.takeIf { it != 0L } ?: System.currentTimeMillis()
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = titleBn.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E)),
                        modifier = Modifier.weight(1f).testTag("save_schedule_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun SocialLinkEditDialog(
    link: SocialMediaLink?,
    isNew: Boolean,
    onSave: (SocialMediaLink) -> Unit,
    onDismiss: () -> Unit,
    language: AppLanguage
) {
    var platformNameBn by remember { mutableStateOf(link?.platformNameBn ?: "") }
    var platformNameEn by remember { mutableStateOf(link?.platformNameEn ?: "") }
    var url by remember { mutableStateOf(link?.url ?: "") }
    var iconType by remember { mutableStateOf(link?.iconType ?: "FACEBOOK") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = if (isNew) "Add Social Link" else "Edit Social Link", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)
                
                OutlinedTextField(value = platformNameBn, onValueChange = { platformNameBn = it }, label = { Text("Name (BN)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = platformNameEn, onValueChange = { platformNameEn = it }, label = { Text("Name (EN)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = {
                        onSave(link?.copy(platformNameBn = platformNameBn, platformNameEn = platformNameEn, url = url, iconType = iconType)
                            ?: SocialMediaLink(id = "SM_${System.currentTimeMillis()}", platformNameBn = platformNameBn, platformNameEn = platformNameEn, url = url, iconType = iconType))
                        onDismiss()
                    }, modifier = Modifier.weight(1f)) { Text("Save") }
                }
            }
        }
    }
}
