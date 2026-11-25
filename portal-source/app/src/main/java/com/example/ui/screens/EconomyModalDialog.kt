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
import com.example.data.model.EconomyOpportunityItem
import com.example.data.repository.DefaultPortalContent

@Composable
fun EconomyModalDialog(
    language: AppLanguage,
    opportunities: List<EconomyOpportunityItem> = DefaultPortalContent.defaultEconomyOpportunities,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("ALL") }

    val tabs = listOf(
        "ALL" to (if (language == AppLanguage.BN) "সকল খাত" else "All"),
        "MEGA" to (if (language == AppLanguage.BN) "মেগা প্রকল্প ও শিল্প" else "Mega Projects"),
        "COASTAL" to (if (language == AppLanguage.BN) "উপকূলীয় লবণ ও শুঁটকি" else "Salt & Paan"),
        "JOBS" to (if (language == AppLanguage.BN) "চাকরি ও যুব প্রশিক্ষণ" else "Jobs & Training")
    )

    val filteredItems = remember(selectedTab, opportunities) {
        val sorted = opportunities.filter { it.isPublished }.sortedBy { it.order }
        when (selectedTab) {
            "MEGA", "COASTAL", "JOBS" -> sorted.filter { it.tabKey.equals(selectedTab, ignoreCase = true) }
            else -> sorted
        }
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
                .testTag("economy_modal_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Color(0xFF7B1FA2),
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
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "কর্মসংস্থান ও অর্থনীতি" else "Employment & Local Economy",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "উপকূলীয় সম্পদ, মেগা প্রকল্প ও যুব প্রশিক্ষণ" else "Coastal resources, mega projects & career training",
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
                                selectedContainerColor = Color(0xFF7B1FA2),
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
                    if (filteredItems.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "এই খাতে কোনো তথ্য পাওয়া যায়নি।" else "No opportunities found in this category.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            val badgeColor = Color(item.badgeColorHex)
                            val targetUrl = item.url?.takeIf { it.isNotBlank() } ?: item.circularPdfUrl.takeIf { it.isNotBlank() }
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
                                                imageVector = Icons.Default.TrendingUp,
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
                                                text = if (language == AppLanguage.BN) item.sectorBn else item.sectorEn.ifBlank { item.sectorBn },
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

                                    if (targetUrl != null) {
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
                                                    text = if (language == AppLanguage.BN) item.actionTextBn.ifBlank { "বিস্তারিত দেখুন" } else item.actionTextEn.ifBlank { item.actionTextBn.ifBlank { "View Details" } },
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
