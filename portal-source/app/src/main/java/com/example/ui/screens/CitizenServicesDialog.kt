package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.DigitalServiceItem
import com.example.data.repository.DefaultPortalContent
import com.example.ui.components.PortalIconMapper
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RoyalBlue800

@Composable
fun CitizenServicesDialog(
    language: AppLanguage,
    services: List<DigitalServiceItem> = DefaultPortalContent.defaultDigitalServices,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val filteredServices = remember(searchQuery, selectedCategory, services) {
        services
            .filter { it.isPublished }
            .sortedBy { it.order }
            .filter { item ->
                val matchesCategory = (selectedCategory == "ALL" || item.category.equals(selectedCategory, ignoreCase = true))
                val matchesSearch = searchQuery.isBlank() ||
                    item.titleBn.contains(searchQuery, ignoreCase = true) ||
                    item.titleEn.contains(searchQuery, ignoreCase = true) ||
                    item.authorityBn.contains(searchQuery, ignoreCase = true) ||
                    item.authorityEn.contains(searchQuery, ignoreCase = true) ||
                    item.descriptionBn.contains(searchQuery, ignoreCase = true) ||
                    item.descriptionEn.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesSearch
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
                .testTag("citizen_services_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = RoyalBlue800,
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
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "নাগরিক সেবা ও তথ্য ভাণ্ডার" else "Digital Citizen Services",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "সরকারি অফিসিয়াল পোর্টাল ও আবেদন লিঙ্ক" else "Official government portals & direct applications",
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

                // Search & Filter Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (language == AppLanguage.BN) "সেবা খুঁজুন (যেমন: এনআইডি, জন্ম নিবন্ধন, ভূমি)..." else "Search services (e.g., NID, Birth, Land)...",
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val categories = listOf(
                        "ALL" to (if (language == AppLanguage.BN) "সকল সেবা" else "All"),
                        "CITIZEN" to (if (language == AppLanguage.BN) "নাগরিকত্ব ও সনদ" else "Citizenship"),
                        "LAND" to (if (language == AppLanguage.BN) "ভূমি ও ব্যবসা" else "Land & Business"),
                        "PASSPORT" to (if (language == AppLanguage.BN) "পাসপোর্ট ও লাইসেন্স" else "Passport & Driving"),
                        "SECURITY" to (if (language == AppLanguage.BN) "আইন ও নিরাপত্তা" else "Security")
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (code, label) ->
                            val isSelected = selectedCategory == code
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = code },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForestGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Services List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredServices.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "কোনো নাগরিক সেবা পাওয়া যায়নি।" else "No matching citizen services found.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredServices, key = { it.id }) { service ->
                            val serviceColor = Color(service.colorHex)
                            val serviceIcon = PortalIconMapper.resolveIcon(service.iconKey, Icons.Default.Badge)
                            val targetUrl = service.portalUrl.ifBlank { service.linkUrl }
                            val authorityText = if (language == AppLanguage.BN) {
                                service.authorityBn.ifBlank { service.subTextBn }
                            } else {
                                service.authorityEn.ifBlank { service.subTextEn.ifBlank { service.authorityBn } }
                            }
                            val descText = if (language == AppLanguage.BN) {
                                service.descriptionBn.ifBlank { service.subTextBn }
                            } else {
                                service.descriptionEn.ifBlank { service.subTextEn.ifBlank { service.descriptionBn } }
                            }

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
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(serviceColor.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = serviceIcon,
                                                contentDescription = null,
                                                tint = serviceColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (language == AppLanguage.BN) service.titleBn else service.titleEn.ifBlank { service.titleBn },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            if (authorityText.isNotBlank()) {
                                                Text(
                                                    text = authorityText,
                                                    fontSize = 11.sp,
                                                    color = ForestGreen,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    if (descText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = descText,
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569),
                                            lineHeight = 17.sp
                                        )
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
                                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
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
                                                    text = if (language == AppLanguage.BN) "পোর্টালে যান" else "Visit Portal",
                                                    fontSize = 12.sp,
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
