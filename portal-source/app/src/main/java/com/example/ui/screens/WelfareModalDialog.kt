package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.WelfareProgramItem
import com.example.data.repository.DefaultPortalContent
import com.example.ui.theme.ForestGreen

@Composable
fun WelfareModalDialog(
    language: AppLanguage,
    programs: List<WelfareProgramItem> = DefaultPortalContent.defaultWelfarePrograms,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val visiblePrograms = remember(programs) {
        programs.filter { it.isPublished }.sortedBy { it.order }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("welfare_modal_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Color(0xFFC2185B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "মানবিক ও সামাজিক সহায়তা" else "Welfare & Social Support",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "সমাজসেবা অধিদপ্তর ভাতা ও আবেদন নির্দেশিকা" else "Dept of Social Services allowances & guidelines",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Official Portal Banner
                Surface(
                    color = Color(0xFFFCE4EC),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFF48FB1))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFC2185B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BN) "অনলাইন সামাজিক নিরাপত্তা ভাতা পোর্টাল" else "Online Social Safety Net Application",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF880E4F)
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "উপজেলা সমাজসেবা কার্যালয়ে না গিয়েও ঘরে বসে সরাসরি আবেদন করা যায়।" else "Apply directly from home without visiting local offices.",
                                fontSize = 11.sp,
                                color = Color(0xFF4A148C)
                            )
                        }
                        Button(
                            onClick = {
                                com.example.util.IntentUtils.safeOpenUrl(context, "https://mis.bhata.gov.bd/onlineApplication", language)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "আবেদন" else "Apply",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Program List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (visiblePrograms.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "বর্তমানে কোনো সামাজিক সহায়তা কর্মসূচি পাওয়া যায়নি।" else "No welfare programs available right now.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(visiblePrograms, key = { it.id }) { prog ->
                            val targetUrl = prog.applyUrl.ifBlank { prog.documentUrl }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFC2185B).copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color(0xFFC2185B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (language == AppLanguage.BN) prog.titleBn else prog.titleEn.ifBlank { prog.titleBn },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) prog.targetBn else prog.targetEn.ifBlank { prog.targetBn },
                                                fontSize = 11.sp,
                                                color = Color(0xFFC2185B),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = if (language == AppLanguage.BN) "📌 যোগ্যতা ও শর্ত:" else "📌 Eligibility:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) prog.eligibilityBn else prog.eligibilityEn.ifBlank { prog.eligibilityBn },
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            val rateText = if (language == AppLanguage.BN) prog.rateBn else prog.rateEn.ifBlank { prog.rateBn }
                                            Text(
                                                text = if (language == AppLanguage.BN) "💰 ভাতার হার: $rateText" else "💰 Allowance: $rateText",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreen
                                            )
                                        }
                                    }

                                    if (targetUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    com.example.util.IntentUtils.safeOpenUrl(context, targetUrl, language)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInNew,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (language == AppLanguage.BN) "আবেদন করুন" else "Apply Online",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
