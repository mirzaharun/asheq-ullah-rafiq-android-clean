package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.EducationResourceItem
import com.example.data.repository.DefaultPortalContent
import com.example.ui.theme.ForestGreen

@Composable
fun EducationModalDialog(
    language: AppLanguage,
    resources: List<EducationResourceItem> = DefaultPortalContent.defaultEducationResources,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("ALL") }

    val displayItems = remember(selectedTab, resources) {
        val sorted = resources.filter { it.isPublished }.sortedBy { it.order }
        when (selectedTab) {
            "SCHOLARSHIPS" -> sorted.filter { it.type.equals("SCHOLARSHIP", ignoreCase = true) }
            "RESULTS" -> sorted.filter { it.type.equals("RESULTS", ignoreCase = true) }
            "BOARDS" -> sorted.filter { it.type.equals("BOARD", ignoreCase = true) }
            else -> sorted
        }
    }
    val boardCount = remember(resources) {
        resources.count { it.isPublished && it.type.equals("BOARD", ignoreCase = true) }
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
                .testTag("education_modal_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = Color(0xFF0284C7),
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
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "শিক্ষা ও শিক্ষার্থী সহায়তা" else "Education & Student Support",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "সরকারি বৃত্তি ও সকল শিক্ষা বোর্ডের রেজাল্ট" else "Govt scholarships & all education boards",
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

                // Filter Tabs
                val tabs = listOf(
                    "ALL" to (if (language == AppLanguage.BN) "সকল রিসোর্স" else "All"),
                    "SCHOLARSHIPS" to (if (language == AppLanguage.BN) "সরকারি বৃত্তি ও ট্রাস্ট" else "Scholarships"),
                    "RESULTS" to (if (language == AppLanguage.BN) "রেজাল্ট পোর্টাল" else "Results Portal"),
                    "BOARDS" to (if (language == AppLanguage.BN) "${boardCount}টি শিক্ষা বোর্ড" else "$boardCount Education Boards")
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs) { (code, title) ->
                        val isSelected = selectedTab == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = code },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (displayItems.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "এই বিভাগে কোনো রিসোর্স পাওয়া যায়নি।" else "No resources found in this category.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(displayItems, key = { it.id }) { item ->
                            val icon = when (item.type.uppercase()) {
                                "SCHOLARSHIP" -> Icons.Default.CardGiftcard
                                "RESULTS" -> Icons.Default.WorkspacePremium
                                else -> Icons.Default.AccountBalance
                            }
                            val badgeColor = when (item.type.uppercase()) {
                                "SCHOLARSHIP" -> ForestGreen
                                "RESULTS" -> Color(0xFFE11D48)
                                else -> Color(0xFF0284C7)
                            }
                            val targetUrl = item.url.ifBlank { item.documentUrl }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(badgeColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) item.categoryBn else item.categoryEn.ifBlank { item.categoryBn },
                                                fontSize = 11.sp,
                                                color = badgeColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (language == AppLanguage.BN) item.descriptionBn else item.descriptionEn.ifBlank { item.descriptionBn },
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 17.sp
                                    )

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
                                                colors = ButtonDefaults.buttonColors(containerColor = badgeColor),
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
                                                    text = if (language == AppLanguage.BN) "অফিসিয়াল পোর্টাল খুলুন" else "Open Official Portal",
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
