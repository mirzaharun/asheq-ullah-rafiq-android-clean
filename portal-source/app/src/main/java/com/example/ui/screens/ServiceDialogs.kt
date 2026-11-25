package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import com.example.data.model.*
import com.example.data.repository.DefaultPortalContent
import com.example.ui.theme.*
import com.example.util.AuthManager
import com.example.util.IntentUtils

// --- 1. Weather & Maritime Signals Dialog ---
@Composable
fun WeatherDetailDialog(
    weather: WeatherData,
    language: AppLanguage,
    onRefreshWeather: ((onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentWeatherData = weather
    var isRefreshing by remember { mutableStateOf(false) }
    var isJustUpdated by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("weather_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "উপকূলীয় আবহাওয়া ও সতর্ক সংকেত" else "Coastal Weather & Maritime Alerts",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Text(
                                text = if (isJustUpdated) {
                                    if (language == AppLanguage.BN) "হালনাগাদ: সার্ভার থেকে সিঙ্ককৃত (${currentWeatherData.lastUpdatedBn})" else "Synced: ${currentWeatherData.lastUpdatedEn}"
                                } else {
                                    if (language == AppLanguage.BN) "হালনাগাদ: ${currentWeatherData.lastUpdatedBn}" else "Updated: ${currentWeatherData.lastUpdatedEn}"
                                },
                                fontSize = 11.sp,
                                color = if (isJustUpdated) ForestGreen else TextMuted,
                                fontWeight = if (isJustUpdated) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prominent Live Refresh Bar (Read-only Firestore sync)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0F9FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "সার্ভার আবহাওয়া সিঙ্ক" else "Cloud Weather Sync",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0369A1)
                            )
                        }

                        Button(
                            onClick = {
                                if (!isRefreshing) {
                                    if (onRefreshWeather != null) {
                                        isRefreshing = true
                                        onRefreshWeather.invoke(
                                            {
                                                isRefreshing = false
                                                isJustUpdated = true
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (language == AppLanguage.BN) "সার্ভার থেকে সর্বশেষ আবহাওয়া তথ্য সিঙ্ক হয়েছে" else "Latest weather synced from server",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            { err ->
                                                isRefreshing = false
                                                android.widget.Toast.makeText(
                                                    context,
                                                    if (language == AppLanguage.BN) "সিঙ্ক করা যায়নি: $err" else "Sync failed: $err",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    } else {
                                        isJustUpdated = true
                                    }
                                }
                            },
                            enabled = !isRefreshing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("weather_refresh_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "অপেক্ষা করুন..." else "Syncing...",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "রিফ্রেশ করুন" else "Refresh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Maritime Signal Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (currentWeatherData.maritimeSignal > 2) Color(0xFFFFEBEE) else Color(0xFFFFF8E1),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentWeatherData.maritimeSignal > 2) RedDanger else AmberWarning
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (currentWeatherData.maritimeSignal > 2) RedDanger else AmberWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${currentWeatherData.maritimeSignal}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) currentWeatherData.signalDescBn else currentWeatherData.signalDescEn,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentWeatherData.maritimeSignal > 2) RedDanger else Color(0xFF795548)
                            )
                            Text(
                                text = if (currentWeatherData.maritimeSignal == 1) {
                                    if (language == AppLanguage.BN) "সাগরে জাহাজ ও ট্রলার স্বাভাবিকভাবে চলতে পারে" else "Vessels may operate normally"
                                } else {
                                    if (language == AppLanguage.BN) "উপকূলের কাছাকাছি নিরাপদ আশ্রয়ে থাকুন" else "Take shelter near safe coastlines"
                                },
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weather metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = if (language == AppLanguage.BN) "বাতাসের গতি" else "Wind Speed",
                        value = "${currentWeatherData.windSpeedKmH} km/h",
                        icon = Icons.Default.Air,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = if (language == AppLanguage.BN) "সাগরের অবস্থা" else "Sea State",
                        value = if (language == AppLanguage.BN) "শান্ত" else "Calm",
                        icon = Icons.Default.Waves,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = if (language == AppLanguage.BN) "জোয়ার" else "High Tide",
                        value = if (language == AppLanguage.BN) currentWeatherData.highTideBn else currentWeatherData.highTideEn,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Advisory box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "💡 নৌ-যাত্রীদের জন্য পরামর্শ" else "💡 Passenger Advisory",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue800
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == AppLanguage.BN) currentWeatherData.advisoryBn else currentWeatherData.advisoryEn,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (language == AppLanguage.BN) "ঠিক আছে" else "Close")
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = RoyalBlue700, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 9.sp, color = TextMuted, textAlign = TextAlign.Center)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        }
    }
}

// --- 2. Sea-Truck Schedules & Updates Dialog ---
@Composable
fun SeaTruckDetailDialog(
    seaTruckUpdate: SeaTruckUpdate,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("seatruck_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBoat,
                                contentDescription = null,
                                tint = OceanTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "সী-ট্রাক ও নৌযান সময়সূচী" else "Sea-Truck & Ferry Schedule",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Text(
                                text = if (language == AppLanguage.BN) seaTruckUpdate.generalStatusBn else seaTruckUpdate.generalStatusEn,
                                fontSize = 11.sp,
                                color = GreenSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Safety notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📢 " + if (language == AppLanguage.BN) seaTruckUpdate.noticeBn else seaTruckUpdate.noticeEn,
                        fontSize = 11.sp,
                        color = RoyalBlue800,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(seaTruckUpdate.routes, key = { it.id }) { route ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "${route.fromBn} ➔ ${route.toBn}" else "${route.fromEn} ➔ ${route.toEn}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue900
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.BN) route.statusBn else route.statusEn,
                                            color = GreenSuccess,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (language == AppLanguage.BN) "নৌযান: ${route.vesselNameBn}" else "Vessel: ${route.vesselNameEn}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "⏰ " + if (language == AppLanguage.BN) route.departureTimeBn else route.departureTimeEn,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "ভাড়া: ৳${route.fareBdt}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00695C)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close")
                }
            }
        }
    }
}

// --- 3. Emergency Hotlines Dialog ---
@Composable
fun EmergencyContactsDialog(
    contacts: List<EmergencyContact>,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("emergency_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = RedDanger,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "দুর্যোগ ও জরুরি সহায়তা ডিরেক্টরি" else "Disaster & Emergency Relief",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedDanger
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "মহেশখালী ও কুতুবদিয়া ২৪ ঘণ্টা জরুরি সেবা" else "24/7 Island Emergency & Cyclone Directory",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Emergency Hotline Pills (999, 1090, 333)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pills = listOf(
                        Triple("999", if (language == AppLanguage.BN) "জরুরি সেবা" else "Emergency", Color(0xFFD32F2F)),
                        Triple("1090", if (language == AppLanguage.BN) "দুর্যোগ বার্তা" else "Cyclone/Tide", Color(0xFFE65100)),
                        Triple("333", if (language == AppLanguage.BN) "নাগরিক সেবা" else "Govt Info", Color(0xFF0284C7))
                    )
                    pills.forEach { (num, label, col) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = col.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    IntentUtils.safeDial(context, num, language)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(num, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = col)
                                Text(label, fontSize = 9.5.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF5F5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) contact.titleBn else contact.titleEn,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) contact.addressBn else contact.addressEn,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📞 ${contact.phone}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedDanger
                                    )
                                }

                                Button(
                                    onClick = {
                                        IntentUtils.safeDial(context, contact.phone, language)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (language == AppLanguage.BN) "কল করুন" else "Call", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close")
                }
            }
        }
    }
}

// --- 4. Blood Donors Directory Dialog ---
@Composable
fun BloodDonorsDialog(
    donors: List<BloodDonor>,
    selectedGroup: String?,
    selectedUpazila: String?,
    language: AppLanguage,
    onGroupSelected: (String?) -> Unit,
    onUpazilaSelected: (String?) -> Unit,
    onRegisterClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("blood_donors_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFCE4EC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bloodtype,
                                contentDescription = null,
                                tint = Color(0xFFC2185B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "রক্তদাতা ডিরেক্টরি" else "Blood Donors Registry",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "মহেশখালী ও কুতুবদিয়া ব্লাড ব্যাংক" else "Moheshkhali & Kutubdia Registry",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Blood Group Filter Chips
                val bloodGroups = listOf(null, "A+", "B+", "O+", "AB+", "A-", "B-", "O-", "AB-")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(bloodGroups) { group ->
                        val isSelected = selectedGroup == group
                        FilterChip(
                            selected = isSelected,
                            onClick = { onGroupSelected(group) },
                            label = {
                                Text(
                                    text = group ?: if (language == AppLanguage.BN) "সকল রক্ত" else "All",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFC2185B),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Upazila Filter Chips
                val upazilas = listOf(
                    null to (if (language == AppLanguage.BN) "সকল উপজেলা" else "All"),
                    "Moheshkhali" to (if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali"),
                    "Kutubdia" to (if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia")
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(upazilas) { (up, label) ->
                        val isSelected = selectedUpazila == up
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpazilaSelected(up) },
                            label = { Text(text = label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RoyalBlue800,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Donor count & register action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "উপলব্ধ দাতা: ${donors.size} জন" else "Available: ${donors.size} donors",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    TextButton(onClick = onRegisterClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "দাতা হিসেবে নাম লেখান" else "Register as Donor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2185B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Donors List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(donors, key = { it.id }) { donor ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFAFAFA),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Blood group badge
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC2185B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = donor.bloodGroup,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) donor.nameBn else donor.nameEn,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "📍 " + (if (language == AppLanguage.BN) "${donor.areaBn}, ${donor.upazila}" else "${donor.areaEn}, ${donor.upazila}"),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "সর্বশেষ রক্তদান: " + (if (language == AppLanguage.BN) donor.lastDonatedBn else donor.lastDonatedEn),
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        IntentUtils.safeDial(context, donor.phone, language)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE8F5E9), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call Donor",
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(18.dp)
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

// --- 5. Register Donor Dialog ---
@Composable
fun RegisterDonorDialog(
    language: AppLanguage,
    currentUserId: String? = null,
    onSave: (name: String, group: String, upazila: String, area: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("O+") }
    var upazila by remember { mutableStateOf("Moheshkhali") }
    var isSigningIn by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "রক্তদাতা নিবন্ধন" else "Register as Blood Donor",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                if (currentUserId.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN)
                                    "রক্তদাতা হিসেবে নিবন্ধন করতে আপনার Google অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
                                else
                                    "Sign in with Google to register as a verified blood donor.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF1E3A8A)
                            )
                            OutlinedButton(
                                onClick = {
                                    if (!isSigningIn) {
                                        isSigningIn = true
                                        AuthManager.signInWithGoogle(
                                            context = context,
                                            credentialManager = credentialManager,
                                            scope = scope,
                                            onAuthSuccess = { isSigningIn = false },
                                            onAuthError = { err ->
                                                isSigningIn = false
                                                android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                            },
                                            onAuthCancelled = { isSigningIn = false }
                                        )
                                    }
                                },
                                enabled = !isSigningIn,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("donor_google_signin_button")
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        if (language == AppLanguage.BN) "Google দিয়ে সাইন-ইন করুন" else "Sign in with Google",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RoyalBlue800
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == AppLanguage.BN) "আপনার পূর্ণ নাম" else "Full Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == AppLanguage.BN) "মোবাইল নম্বর" else "Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text(if (language == AppLanguage.BN) "গ্রাম / এলাকা" else "Area / Union") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Blood Group Selection
                Text(text = if (language == AppLanguage.BN) "রক্তের গ্রুপ" else "Blood Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val groups = listOf("A+", "B+", "O+", "AB+", "A-", "B-", "O-", "AB-")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(groups) { g ->
                        FilterChip(
                            selected = group == g,
                            onClick = { group = g },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }

                // Upazila Selection
                Text(text = if (language == AppLanguage.BN) "উপজেলা" else "Upazila", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = upazila == "Moheshkhali",
                        onClick = { upazila = "Moheshkhali" },
                        label = { Text(if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = upazila == "Kutubdia",
                        onClick = { upazila = "Kutubdia" },
                        label = { Text(if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = {
                            if (currentUserId.isNullOrBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "নিবন্ধন করতে প্রথমে Google দিয়ে সাইন-ইন করুন" else "Please sign in with Google first",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            val trimmedName = name.trim()
                            val trimmedPhone = phone.trim()
                            if (trimmedName.isBlank() || trimmedPhone.length < 6) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "পূর্ণ নাম এবং কমপক্ষে ৬ অঙ্কের মোবাইল নম্বর দিন" else "Please enter your name and a valid phone number",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            onSave(trimmedName, group, upazila, area.trim(), trimmedPhone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                        modifier = Modifier.weight(1f).testTag("submit_donor_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "নিবন্ধন সম্পন্ন" else "Submit")
                    }
                }
            }
        }
    }
}

// --- 6. Admin Panel API Architecture Simulator Dialog ---
@Composable
fun AdminSimulatorDialog(
    currentSignal: Int,
    language: AppLanguage,
    onUpdateSignal: (Int) -> Unit,
    onPublishNotice: (titleBn: String, titleEn: String, detailsBn: String, detailsEn: String, isUrgent: Boolean) -> Unit,
    onUpdateSeaTruck: (isNormal: Boolean, customNoticeBn: String, customNoticeEn: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSignal by remember { mutableStateOf(currentSignal) }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeDetails by remember { mutableStateOf("") }
    var isUrgentNotice by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("admin_simulator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.BN) "এডমিন প্যানেল সিমুলেটর" else "Admin API Integration Console",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "রিয়েলটাইম ডেটা মডেল ও স্টেট টেস্ট" else "Real-time JSON Data State Controller",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Open Web Admin Panel Link
                    item {
                        val context = LocalContext.current
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = RoyalBlue800,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "ওয়েব এডমিন প্যানেল (Web Admin Panel)" else "Web Admin Panel CMS",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue900
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "ফায়ারস্টোর ভিত্তিক ওয়েব এডমিন প্যানেল (HTML5 + Tailwind CSS + JS) যার মাধ্যমে ৫টি পেজের সমস্ত কন্টেন্ট ও ব্যানার নিয়ন্ত্রণ করা যায়।" else "Firestore-powered Web Admin CMS controlling all 5 screens and live portal content.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        IntentUtils.safeOpenUrl(context, "https://ais-dev-hfcrcrxgf2elpsphnvf353-978198531074.asia-southeast1.run.app", language)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "ব্রাউজারে এডমিন প্যানেল খুলুন" else "Open Web Admin CMS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Signal update
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "১. উপকূলীয় সমুদ্র সতর্ক সংকেত পরিবর্তন" else "1. Modify Maritime Warning Signal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue900
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(1, 2, 3, 4).forEach { s ->
                                        FilterChip(
                                            selected = selectedSignal == s,
                                            onClick = {
                                                selectedSignal = s
                                                onUpdateSignal(s)
                                            },
                                            label = { Text("সংকেত $s", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sea-truck control
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "২. সী-ট্রাক চলাচল সতর্কতা জারি" else "2. Sea-Truck Fleet Alert Notice",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue900
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onUpdateSeaTruck(
                                                true,
                                                "চলাচল স্বাভাবিক ও পর্যাপ্ত স্পিডবোট প্রস্তুত রয়েছে।",
                                                "Normal operations and ample speedboats ready at jetty."
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("স্বাভাবিক", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateSeaTruck(
                                                false,
                                                "ভাটার কারণে গোরকঘাটা জেটিতে চলাচল সাময়িক স্থগিত।",
                                                "Service temporarily suspended due to extreme low tide."
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("স্থগিত / সতর্ক", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Publish Notice
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "৩. তাৎক্ষণিক জরুরি নোটিশ প্রকাশ" else "3. Publish Urgent Notice to Feed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue900
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = noticeTitle,
                                    onValueChange = { noticeTitle = it },
                                    label = { Text("বিজ্ঞপ্তির শিরোনাম") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = noticeDetails,
                                    onValueChange = { noticeDetails = it },
                                    label = { Text("বিস্তারিত বিবরণ") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isUrgentNotice, onCheckedChange = { isUrgentNotice = it })
                                    Text("জরুরি লাল সংকেত ব্যাজ যুক্ত করুন", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        if (noticeTitle.isNotBlank()) {
                                            onPublishNotice(noticeTitle, noticeTitle, noticeDetails, noticeDetails, isUrgentNotice)
                                            noticeTitle = ""
                                            noticeDetails = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("বিজ্ঞপ্তি সম্প্রচার করুন")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900)
                ) {
                    Text(text = if (language == AppLanguage.BN) "সম্পন্ন" else "Done")
                }
            }
        }
    }
}

// --- 7. Development Project Detail Dialog ---
@Composable
fun ProjectDetailDialog(
    project: DevelopmentProject,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("project_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    com.example.ui.components.AsyncOrLocalImage(
                        imageUrl = project.imageUrl,
                        contentDescription = project.titleBn,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF00796B),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) project.timelineBn else project.timelineEn,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Details body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = if (language == AppLanguage.BN) project.titleBn else project.titleEn,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "অগ্রগতি: ${project.completionPercentage}%" else "Progress: ${project.completionPercentage}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "বাজেট: ${project.budgetBn}" else "Budget: ${project.budgetEn}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue800,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { project.completionPercentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = OceanTeal,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "প্রকল্প বিবরণ" else "Project Details",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue800
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) project.descriptionBn else project.descriptionEn,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "সামাজিক ও অর্থনৈতিক প্রভাব" else "Socio-Economic Impact",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) project.impactBn else project.impactEn,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Footer buttons
                val context = androidx.compose.ui.platform.LocalContext.current
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.util.PosterGenerator.generateAndSharePoster(context, project)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (language == AppLanguage.BN) "পোস্টার তৈরি ও শেয়ার" else "Share Poster")
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnionHubDialog(
    language: AppLanguage,
    unions: List<UnionServiceHub> = DefaultPortalContent.defaultUnionServices,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedUpazila by remember { mutableStateOf("ALL") }

    val publishedUnions = remember(unions) {
        unions.filter { it.isPublished }.sortedBy { it.order }
    }

    val moheshkhaliCount = remember(publishedUnions) {
        publishedUnions.count {
            it.upazilaEn.equals("Moheshkhali", ignoreCase = true) ||
                it.upazilaBn.contains("মহেশখালী")
        }
    }

    val kutubdiaCount = remember(publishedUnions) {
        publishedUnions.count {
            it.upazilaEn.equals("Kutubdia", ignoreCase = true) ||
                it.upazilaBn.contains("কুতুবদিয়া")
        }
    }

    val filteredUnions = remember(selectedUpazila, publishedUnions) {
        when (selectedUpazila) {
            "MOHESHKHALI" -> publishedUnions.filter {
                it.upazilaEn.equals("Moheshkhali", ignoreCase = true) ||
                    it.upazilaBn.contains("মহেশখালী")
            }
            "KUTUBDIA" -> publishedUnions.filter {
                it.upazilaEn.equals("Kutubdia", ignoreCase = true) ||
                    it.upazilaBn.contains("কুতুবদিয়া")
            }
            else -> publishedUnions
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
                .testTag("union_hub_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = Color(0xFF00796B),
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
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "ইউনিয়ন তথ্য ও সেবা কেন্দ্র" else "Union Digital Centers",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "মহেশখালী ও কুতুবদিয়ার সকল ইউনিয়ন ডিজিটাল সেবা" else "Local citizen portals & direct contacts",
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

                // Upazila Filter Chips
                val upazilaFilters = listOf(
                    "ALL" to (if (language == AppLanguage.BN) "সকল ইউনিয়ন (${publishedUnions.size}টি)" else "All Unions (${publishedUnions.size})"),
                    "MOHESHKHALI" to (if (language == AppLanguage.BN) "মহেশখালী উপজেলা (${moheshkhaliCount}টি)" else "Moheshkhali ($moheshkhaliCount)"),
                    "KUTUBDIA" to (if (language == AppLanguage.BN) "কুতুবদিয়া উপজেলা (${kutubdiaCount}টি)" else "Kutubdia ($kutubdiaCount)")
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(upazilaFilters) { (code, label) ->
                        val isSelected = selectedUpazila == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedUpazila = code },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00796B),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Unions List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    if (filteredUnions.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) {
                                        "এই মুহূর্তে কোনো ইউনিয়ন ডিজিটাল সেন্টারের তথ্য প্রকাশিত নেই।"
                                    } else {
                                        "No published union digital centers available right now."
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(filteredUnions, key = { it.id.ifBlank { it.nameBn } }) { union ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (language == AppLanguage.BN) union.nameBn else union.nameEn.ifBlank { union.nameBn },
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFE0F2F1),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) "উপজেলা: ${union.upazilaBn}" else "Upazila: ${union.upazilaEn.ifBlank { union.upazilaBn }}",
                                                    fontSize = 10.5.sp,
                                                    color = Color(0xFF004D40),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    val servicesText = if (language == AppLanguage.BN) {
                                        union.servicesBn
                                    } else {
                                        union.servicesEn.ifBlank { union.servicesBn }
                                    }
                                    if (servicesText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "💡 $servicesText",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF475569),
                                            lineHeight = 16.sp
                                        )
                                    }

                                    if (union.udcNameBn.isNotBlank() || union.udcPhone.isNotBlank() || union.chairmanPhone.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                if (union.udcNameBn.isNotBlank() || union.udcPhone.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "👤 ${union.udcNameBn.ifBlank { union.chairmanBn }}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF334155),
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        if (union.udcPhone.isNotBlank()) {
                                                            TextButton(
                                                                onClick = {
                                                                    IntentUtils.safeDial(context, union.udcPhone, language)
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                            ) {
                                                                Icon(Icons.Default.Call, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text(text = union.udcPhone, fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                                if (union.chairmanPhone.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "🏛️ ${union.chairmanBn.ifBlank { if (language == AppLanguage.BN) "ইউপি কার্যালয়" else "UP Office" }}",
                                                            fontSize = 10.5.sp,
                                                            color = Color(0xFF64748B),
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        TextButton(
                                                            onClick = {
                                                                IntentUtils.safeDial(context, union.chairmanPhone, language)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(Icons.Default.Call, contentDescription = null, tint = RoyalBlue800, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(text = union.chairmanPhone, fontSize = 10.5.sp, color = RoyalBlue800, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (union.webUrl.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    IntentUtils.safeOpenUrl(context, union.webUrl, language)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (language == AppLanguage.BN) "ইউনিয়ন ওয়েব পোর্টাল" else "Union Web Portal",
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

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val time: String = "এখন"
)

@Composable
fun AIAssistantDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    val initialMessages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = if (language == AppLanguage.BN)
                    "আসসালামু আলাইকুম! আমি মহেশখালী-কুতুবদিয়া পোর্টালের এআই স্মার্ট সহকারী।\n\nমাতারবাড়ি গভীর সমুদ্রবন্দর, কয়লা বিদ্যুৎ কেন্দ্র, সি-ট্রাক সময়সূচী, সরকারি ভাতা, জরুরী হেল্পলাইন বা এলাকার যেকোনো তথ্য জানতে নিচের টপিক চাপুন অথবা নিচে লিখে জিজ্ঞাসা করুন।"
                else
                    "Hello! I am the AI Smart Assistant for Moheshkhali & Kutubdia portal.\n\nAsk me about Matarbari deep sea port, power plant, sea truck schedules, govt welfare allowances, or emergency helplines!"
            )
        )
    }

    val quickQuestions = listOf(
        "মাতারবাড়ি গভীর সমুদ্রবন্দর প্রকল্প",
        "কুতুবদিয়া সি-ট্রাক সময় ও ভাড়া",
        "বয়স্ক ও বিধবা ভাতার নিয়মাবলী",
        "জরুরি স্বাস্থ্য ও অ্যাম্বুলেন্স নম্বর",
        "আদিনাথ মন্দির ভ্রমণের উপায়",
        "লবণ ও মিষ্টি পান চাষীদের সহায়তা"
    )

    fun generateAnswer(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("বন্দর") || q.contains("port") || q.contains("মাতারবাড়ি") && q.contains("প্রকল্প") -> {
                if (language == AppLanguage.BN)
                    "মাতারবাড়ি গভীর সমুদ্রবন্দর বাংলাদেশের প্রথম গভীর সমুদ্র বন্দর। এর ড্রাফট ১৮.৫ মিটার, যা সুবিশাল বাণিজ্যিক মাদার ভেসেল ভেড়ার সক্ষমতা রাখে। প্রকল্পটি জাপান সরকারের জাইকার সহযোগিতায় প্রায় ১৭,৭৭৭ কোটি টাকা ব্যয়ে বাস্তবায়িত হচ্ছে। এটি সম্পন্ন হলে বাংলাদেশের অর্থনীতি ও ব্লু-ইকোনমিতে বৈপ্লবিক পরিবর্তন আনবে।"
                else
                    "Matarbari Deep Sea Port is Bangladesh's premier deep sea facility with an 18.5m draft capability, berthing large container vessels. Built with JICA cooperation at BDT 17,777 Crore."
            }
            q.contains("সি-ট্রাক") || q.contains("sea-truck") || q.contains("ভাড়া") || q.contains("সময়") || q.contains("ঘাট") -> {
                if (language == AppLanguage.BN)
                    "কুতুবদিয়া-মগনামা ও কক্সবাজার-মহেশখালী রুটে নিয়মিত সী-ট্রাক ও স্পিডবোট চলাচল করে।\n\n১. কুতুবদিয়া থেকে মগনামা: সকাল ৭:৩০, দুপুর ১২:০০, বিকাল ৫:০০ (ভাড়া ৫০ টাকা)।\n২. মগনামা থেকে কুতুবদিয়া: সকাল ৯:০০, দুপুর ২:০০, সন্ধ্যা ৭:০০ (ভাড়া ৫০ টাকা)।\n৩. কক্সবাজার ৬নং ঘাট থেকে মহেশখালী গোরকঘাটা: স্পিডবোটে ১১০ টাকা (প্রতি ১৫ মিনিট অন্তর)।"
                else
                    "Sea-Truck & ferry operates daily between Kutubdia (Darbar Ghat) and Magnama (Pekua) at 07:30 AM, 12:00 PM, 05:00 PM (Fare BDT 50). Speedboats run every 15 minutes between Cox's Bazar Ghat 6 and Gorakghata (Fare BDT 110)."
            }
            q.contains("ভাতা") || q.contains("বয়স্ক") || q.contains("বিধবা") || q.contains("প্রতিবন্ধী") || q.contains("allowance") -> {
                if (language == AppLanguage.BN)
                    "সমাজসেবা অধিদপ্তরের ভাতা পেতে অনলাইনে mis.bhata.gov.bd/onlineApplication পোর্টালে আবেদন করা যায়।\n\n১. বয়স্ক ভাতা: পুরুষ সর্বনিম্ন ৬৫ বছর, নারী সর্বনিম্ন ৬২ বছর (মাসিক ৬০০ টাকা)।\n২. বিধবা ভাতা: মাসিক ৫৫০ টাকা।\n৩. অসচ্ছল প্রতিবন্ধী ভাতা: সুবর্ণ নাগরিক কার্ডধারীদের মাসিক ৮৫০ টাকা।\nটাকা সরাসরি নগদ/বিকাশ মোবাইল অ্যাকাউন্টে জমা হয়।"
                else
                    "Apply online at mis.bhata.gov.bd/onlineApplication. Old Age Allowance: Men 65+, Women 62+ (BDT 600/month). Widow Allowance: BDT 550/month. Disability Allowance: BDT 850/month directly disbursed to mobile wallet."
            }
            q.contains("জরুরি") || q.contains("হাসপাতাল") || q.contains("অ্যাম্বুলেন্স") || q.contains("পুলিশ") || q.contains("কোস্টগার্ড") || q.contains("emergency") -> {
                if (language == AppLanguage.BN)
                    "জরুরি হটলাইন নম্বরসমূহ:\n• জাতীয় জরুরি সেবা: ৯৯৯ (পুলিশ, অ্যাম্বুলেন্স, ফায়ার সার্ভিস)\n• নাগরিক সেবা হেল্পলাইন: ৩৩৩\n• দুর্যোগের আগাম সতর্কবার্তা: ১০৯০ (টোল ফ্রি)\n• মহেশখালী কোস্টগার্ড স্টেশন: ০১৭৬৯-৪৪৪০২০\n• মহেশখালী স্বাস্থ্য কমপ্লেক্স: ০১৮১৯-৫৫৬৬৭৭\n• কুতুবদিয়া স্বাস্থ্য কমপ্লেক্স: ০১৮১৮-৯৯০০১১"
                else
                    "Emergency Hotlines:\n• National Emergency: 999\n• Citizen Info: 333\n• Coastal Disaster Warning: 1090\n• Moheshkhali Coast Guard: 01769-444020\n• Moheshkhali Hospital: 01819-556677\n• Kutubdia Hospital: 01818-990011"
            }
            q.contains("আদিনাথ") || q.contains("মন্দির") || q.contains("সোনাদিয়া") || q.contains("বাতিঘর") || q.contains("পর্যটন") || q.contains("tourism") -> {
                if (language == AppLanguage.BN)
                    "মহেশখালী-কুতুবদিয়ার প্রধান দর্শনীয় স্থানসমূহ:\n১. আদিনাথ মন্দির (মৈনাক পাহাড়ের চূড়ায় সনাতন ধর্মাবলম্বীদের তীর্থস্থান)।\n২. সোনাদিয়া দ্বীপ (লাল কাঁকড়া ও অনাবিল সমুদ্রসৈকত)।\n৩. কুতুবদিয়া বাতিঘর (১৮৪৬ সালে ব্রিটিশ আমলে প্রতিষ্ঠিত ঐতিহাসিক বাতিঘর)।\n৪. কুতুবদিয়া সমুদ্রসৈকত ও বায়ুবিদ্যুৎ কেন্দ্র (উইন্ডমিল)।\nকক্সবাজার ৬নং ঘাট বা মগনামা ঘাট দিয়ে সহজে যাতায়াত করা যায়।"
                else
                    "Top Tourism Attractions: Adinath Temple atop Mainak Hill, Sonadia Island with virgin sands and red crabs, Historical Kutubdia Lighthouse (1846), and Kutubdia Windmill Beach."
            }
            q.contains("লবণ") || q.contains("পান") || q.contains("শুঁটকি") || q.contains("salt") -> {
                if (language == AppLanguage.BN)
                    "মহেশখালী ও কুতুবদিয়া দেশের লবণ, মিষ্টি পান ও শুঁটকির রাজধানী। বিসিকের মাধ্যমে আধুনিক পলিথিন পদ্ধতিতে চাষীদের ঋণ ও কারিগরি সহায়তা দেওয়া হচ্ছে। মিষ্টি পানের জন্য মহেশখালী জিআই স্বীকৃতি পেয়েছে এবং সোনাদিয়ায় কীটনাশকমুক্ত অর্গানিক শুঁটকি প্রস্তুত হয়।"
                else
                    "Moheshkhali and Kutubdia are the primary hubs for national salt production, famous sweet betel leaf (GI certified), and pesticide-free dry fish export processing."
            }
            q.contains("হ্যালো") || q.contains("হাই") || q.contains("কেমন") || q.contains("hello") || q.contains("hi") -> {
                if (language == AppLanguage.BN)
                    "হ্যালো! আমি আপনার সেবায় নিয়োজিত। মহেশখালী ও কুতুবদিয়ার মেগা উন্নয়ন, যোগাযোগ ব্যবস্থা বা সরকারি নাগরিক সেবা সংক্রান্ত যেকোনো তথ্য জানতে প্রশ্ন করুন।"
                else
                    "Hello! I am at your service. Please ask any question about development, travel, or services in Moheshkhali & Kutubdia."
            }
            else -> {
                if (language == AppLanguage.BN)
                    "আপনার প্রশ্নের জন্য ধন্যবাদ। মহেশখালী-কুতুবদিয়া পোর্টালে এই বিষয়ে বিস্তারিত তথ্য সংরক্ষিত রয়েছে। আপনি পোর্টালের 'উন্নয়ন', 'নাগরিক সেবা', 'যোগাযোগ' বা 'পর্যটন' সেকশন থেকেও তথ্য পেতে পারেন অথবা নির্দিষ্ট কোনো প্রকল্প বা সেবা সম্পর্কে বিস্তারিত লিখুন।"
                else
                    "Thank you for asking. You can explore relevant information in the Development, Public Services, Tourism or Contact sections of this portal."
            }
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_assistant_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Color(0xFF4F46E5),
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
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "এআই স্মার্ট সহকারী" else "AI Smart Assistant",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "মহেশখালী-কুতুবদিয়ার সকল তথ্য তাৎক্ষণিক জানুন" else "Instant constituency intelligence & civic help",
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

                // Quick Prompt Chips
                Surface(
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "💡 দ্রুত জানতে যেকোনো একটিতে চাপুন:" else "💡 Suggested Inquiries:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4338CA),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            items(quickQuestions) { question ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                    modifier = Modifier.clickable {
                                        initialMessages.add(ChatMessage(isUser = true, text = question))
                                        val ans = generateAnswer(question)
                                        initialMessages.add(ChatMessage(isUser = false, text = ans))
                                    }
                                ) {
                                    Text(
                                        text = question,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF3730A3),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(initialMessages) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!msg.isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4F46E5).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.SmartToy,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (msg.isUser) 14.dp else 2.dp,
                                    bottomEnd = if (msg.isUser) 2.dp else 14.dp
                                ),
                                color = if (msg.isUser) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (msg.isUser) Color(0xFF4338CA) else Color(0xFFE2E8F0)),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 12.5.sp,
                                    color = if (msg.isUser) Color.White else Color(0xFF1E293B),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Input Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (language == AppLanguage.BN) "আপনার প্রশ্ন লিখুন..." else "Ask a question...",
                                    fontSize = 12.5.sp
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val q = inputText.trim()
                                    initialMessages.add(ChatMessage(isUser = true, text = q))
                                    inputText = ""
                                    val ans = generateAnswer(q)
                                    initialMessages.add(ChatMessage(isUser = false, text = ans))
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4F46E5))
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 11. About App & Purpose Dialog ---
@Composable
fun AboutAppDialog(
    language: AppLanguage,
    appConfig: AppConfig = AppConfig(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.84f)
                .testTag("about_app_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue900, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "অ্যাপ সম্পর্কে" else "About This App",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN)
                                        "${appConfig.appNameBn} — ${appConfig.subTitleBn}"
                                    else
                                        "${appConfig.appNameEn} — ${appConfig.subTitleEn}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) appConfig.constituencyBn else appConfig.constituencyEn,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RoyalBlue800
                                )
                            }
                        }
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "১. অ্যাপের উদ্দেশ্য" else "1. App Purpose",
                            body = if (language == AppLanguage.BN) {
                                "কক্সবাজার-২ (মহেশখালী ও কুতুবদিয়া) আসনের নাগরিকদের কাছে উন্নয়ন কর্মকাণ্ডের তথ্য, উপকূলীয় জনগুরুত্বপূর্ণ সেবা এবং জনপ্রতিনিধির কার্যালয়ের সাথে সরাসরি ডিজিটাল যোগাযোগ সহজতর করার লক্ষ্যে এই পোর্টালটি নির্মিত।"
                            } else {
                                "This portal provides citizens of Cox's Bazar-2 (Moheshkhali & Kutubdia) with transparent access to regional development updates, essential coastal services, and direct digital communication with the public service office."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "২. অ্যাপের মূল সেবাসমূহ" else "2. Core Portal Services",
                            body = if (language == AppLanguage.BN) {
                                "• উন্নয়ন প্রকল্প ট্র্যাকিং: মহেশখালী ও কুতুবদিয়া উপজেলার ১১টি খাতের প্রকল্প ও প্রতিবেদন।\n" +
                                "• উপকূলীয় ও জরুরি তথ্য: আবহাওয়া ও সমুদ্র সতর্ক সংকেত, সি-ট্রাক ও স্পিডবোট সময়সূচি, জোয়ার-ভাটার সময়, জরুরি হেল্পলাইন ও রক্তদাতা ডিরেক্টরি।\n" +
                                "• নাগরিক আবেদন ও যোগাযোগ: সাক্ষাৎ/আবেদন ও অভিযোগ জমা, ট্র্যাকিং আইডি দিয়ে অবস্থা যাচাই, সাধারণ জিজ্ঞাসা (FAQ) এবং মতামত/জরিপ।\n" +
                                "• পর্যটন, মিডিয়া ও পরিচিতি: স্থানীয় পর্যটন তথ্য, ছবি ও ভিডিও গ্যালারি, সংবাদ আর্কাইভ এবং যাচাইকৃত জীবনী।"
                            } else {
                                "• Development Tracking: Projects and reports organized across 11 sectors for Moheshkhali and Kutubdia.\n" +
                                "• Coastal & Emergency Info: Weather and maritime alerts, sea-truck & speedboat schedules, tide timings, emergency helplines, and blood donor directory.\n" +
                                "• Citizen Services & Contact: Submit appointments/grievances, track status by tracking ID, view FAQs, and share feedback or vote in citizen polls.\n" +
                                "• Tourism, Media & Profile: Coastal tourism guide, photo & video gallery, news archive, and verified biography."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৩. অফিসিয়াল পরিচিতি ও যোগাযোগ" else "3. Official Identity & Contact",
                            body = if (language == AppLanguage.BN) {
                                "দায়িত্বপ্রাপ্ত পোর্টাল: আশেক উল্লাহ রফিক ডিজিটাল পোর্টাল\n" +
                                "কার্যালয়: ${appConfig.officeAddressCoxsBazarBn}\n" +
                                "হেল্পলাইন: ${appConfig.hotlinePhone}\n" +
                                "ইমেইল: ${appConfig.contactEmail}"
                            } else {
                                "Portal: Asheq Ullah Rafiq Digital Portal\n" +
                                "Main Office: ${appConfig.officeAddressCoxsBazarEn}\n" +
                                "Helpline: ${appConfig.hotlinePhone}\n" +
                                "Email: ${appConfig.contactEmail}"
                            }
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp)
                            .testTag("about_app_close_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800)
                    ) {
                        Text(if (language == AppLanguage.BN) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 12. Privacy Policy Dialog ---
@Composable
fun PrivacyPolicyDialog(
    language: AppLanguage,
    appConfig: AppConfig = AppConfig(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .testTag("privacy_policy_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue900, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.PrivacyTip,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "গোপনীয়তা নীতি" else "Privacy Policy",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "১. তথ্য সংগ্রহ" else "1. Information Collection",
                            body = if (language == AppLanguage.BN) {
                                "• সাধারণ ব্রাউজিং: উন্নয়ন প্রকল্প, নোটিশ, গ্যালারি, সি-ট্রাক ও আবহাওয়া তথ্য, পর্যটন ও পরিচিতি দেখতে কোনো লগইন প্রয়োজন হয় না।\n" +
                                "• নাগরিক ফর্ম ও সেবা: আবেদন/সাক্ষাৎ, সাধারণ মতামত, রক্তদাতা নিবন্ধন বা জনমত জরিপে অংশগ্রহণের সময় আপনি স্বেচ্ছায় নাম, মোবাইল নম্বর, উপজেলা/ইউনিয়ন, মতামতের বিবরণ ও ঐচ্ছিক সংযুক্তি প্রদান করেন। এই সেবাগুলোর জন্য Google Sign-In এর মাধ্যমে Firebase Authentication থেকে আপনার অনন্য ইউজার আইডি (UID) যুক্ত করা হয়।\n" +
                                "• ডিভাইস অনুমতি: অ্যাপে ইন্টারনেট ও নেটওয়ার্ক অবস্থা যাচাইয়ের অনুমতি (INTERNET, ACCESS_NETWORK_STATE) এবং ঐচ্ছিক বিজ্ঞপ্তির জন্য Android 13+ ডিভাইসে POST_NOTIFICATIONS অনুমতি ব্যবহৃত হয়। কোনো ব্যাকগ্রাউন্ড লোকেশন (GPS), কন্টাক্ট, এসএমএস বা কল-লগ সংগ্রহ করা হয় না। ছবি বা নথি যুক্ত করতে অ্যান্ড্রয়েডের নিরাপদ সিস্টেম পিকার ব্যবহৃত হয়।"
                            } else {
                                "• Public Browsing: No login is required to browse development projects, notices, gallery, coastal schedules, tourism info, or biography.\n" +
                                "• Citizen Submissions: When submitting applications/appointments, feedback, blood donor registrations, or poll votes, you voluntarily provide fields such as name, phone number, upazila/union, message, and optional attachments. These submissions require Google Sign-In via Firebase Authentication to associate your unique User ID (UID).\n" +
                                "• Device Permissions: The app uses INTERNET and ACCESS_NETWORK_STATE permissions, plus optional POST_NOTIFICATIONS permission on Android 13+ for notice alerts. No background GPS location, contacts, SMS, or call logs are collected. Optional file/photo attachments use Android's standard system picker."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "২. তথ্যের ব্যবহার" else "2. How We Use Information",
                            body = if (language == AppLanguage.BN) {
                                "সংগৃহীত তথ্য শুধুমাত্র আপনার নাগরিক আবেদন বা অভিযোগ নিষ্পত্তি, ট্র্যাকিং আইডির মাধ্যমে আবেদনের অবস্থান প্রদর্শন, মতামত পর্যালোচনা এবং জরুরি রক্তদাতা সমন্বয়ে ব্যবহৃত হয়। কোনো বিজ্ঞাপন প্রদর্শন বা বাণিজ্যিক উদ্দেশ্যে আপনার তথ্য ব্যবহার বা বিক্রি করা হয় না।"
                            } else {
                                "Collected information is used strictly to process citizen applications or grievances, provide application status tracking, review public feedback, and coordinate blood donor requests. Data is never sold or used for advertising."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৩. তথ্য সংরক্ষণ ও নিরাপত্তা" else "3. Data Storage & Security",
                            body = if (language == AppLanguage.BN) {
                                "আপনার জমাকৃত তথ্য এনক্রিপ্টেড HTTPS/TLS সংযোগের মাধ্যমে Google Cloud Firestore এবং প্রযোজ্য ক্ষেত্রে Firebase Storage-এ সংরক্ষিত হয়। Firestore Security Rules দ্বারা প্রতিটি নাগরিক আবেদন কেবল সংশ্লিষ্ট আবেদনকারী (UID) এবং যাচাইকৃত অনুমোদিত এডমিনের প্রবেশাধিকারে সুরক্ষিত থাকে। অ্যাপের অভ্যন্তরীণ মিডিয়া ফাইল ডিভাইসের প্রাইভেট অ্যাপ স্টোরেজে রাখা হয়।"
                            } else {
                                "Submitted records are transmitted over encrypted HTTPS/TLS and stored in Google Cloud Firestore (and Firebase Storage when files are uploaded). Access is enforced by Firestore Security Rules restricting citizen submissions to their authenticated UID owner and verified authorized administrators. Cached local media files remain inside the app's private storage directory."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৪. তৃতীয় পক্ষের সেবা" else "4. Third-Party Services",
                            body = if (language == AppLanguage.BN) {
                                "তথ্য সংরক্ষণ, প্রমাণীকরণ ও সাইন-ইনের জন্য অ্যাপটি Google Firebase (Authentication, Cloud Firestore, Storage) এবং Android Jetpack Credential Manager (Google Sign-In) ব্যবহার করে। এছাড়া আপনি গ্যালারি বা সামাজিক মাধ্যমে কোনো বাহ্যিক লিংকে (যেমন YouTube, Google Maps বা সংবাদ লিংক) ট্যাপ করলে আপনার ডিভাইসের ব্রাউজার বা সংশ্লিষ্ট তৃতীয় পক্ষের অ্যাপ খুলতে পারে। অ্যাপে কোনো থার্ড-পার্টি অ্যাড এসডিকে (Ads SDK) নেই।"
                            } else {
                                "The app uses Google Firebase (Authentication, Cloud Firestore, Storage) and Android Credential Manager (Google Sign-In) for authentication and cloud data handling. Tapping external links in the gallery, social section, or map buttons opens your device browser or external apps governed by their own policies. No third-party ad SDKs are included."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৫. তথ্য সংশোধন ও মুছে ফেলার নীতি" else "5. Data Correction & Deletion Policy",
                            body = if (language == AppLanguage.BN) {
                                "আপনি যেকোনো সময় অ্যাপ থেকে আপনার Google সাইন-ইন সেশন লগআউট করতে পারবেন। পূর্বে জমা দেওয়া কোনো আবেদন, মতামত, রক্তদাতা তথ্য বা ব্যক্তিগত রেকর্ড সংশোধন বা স্থায়ীভাবে মুছে ফেলতে চাইলে আপনার নাম, ট্র্যাকিং আইডি/মোবাইল নম্বর উল্লেখ করে আমাদের অফিসিয়াল ইমেইল (${appConfig.contactEmail}) অথবা হেল্পলাইন নম্বরে (${appConfig.hotlinePhone}) যোগাযোগ করুন। পরিচয় যাচাইয়ের পর অনুমোদিত এডমিন সংশ্লিষ্ট রেকর্ড ডেটাবেজ থেকে স্থায়ীভাবে মুছে দেবেন।"
                            } else {
                                "You can sign out of your Google session anytime in the app. To request correction or permanent deletion of a submitted application, feedback entry, or blood donor profile, please contact our office via email (${appConfig.contactEmail}) or helpline (${appConfig.hotlinePhone}) with your name and tracking ID/phone number for verification. Verified deletion requests are permanently removed from Firestore by authorized administrators."
                            }
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp)
                            .testTag("privacy_policy_close_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800)
                    ) {
                        Text(if (language == AppLanguage.BN) "বুঝেছি ও সম্মত" else "Understood & Agree", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 12B. Dedicated Terms of Service Dialog ---
@Composable
fun TermsOfServiceDialog(
    language: AppLanguage,
    appConfig: AppConfig = AppConfig(),
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.86f)
                .testTag("terms_of_service_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue900, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "ব্যবহারের শর্তাবলী" else "Terms of Service",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "১. ব্যবহারকারীর দায়িত্ব" else "1. User Responsibilities",
                            body = if (language == AppLanguage.BN) {
                                "• নাগরিক আবেদন, সাক্ষাৎ, অভিযোগ, মতামত কিংবা রক্তদাতা ফর্মে সর্বদা সঠিক, সত্য ও হালনাগাদ তথ্য প্রদান করতে হবে।\n" +
                                "• আবেদন জমা দেওয়ার পর প্রাপ্ত ট্র্যাকিং আইডি এবং আপনার Google অ্যাকাউন্টের নিরাপত্তা আপনাকেই রক্ষা করতে হবে।"
                            } else {
                                "• You agree to provide accurate, truthful, and up-to-date details when submitting applications, grievances, feedback, or blood donor registrations.\n" +
                                "• You are responsible for safeguarding your Google Sign-In session and application tracking numbers."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "২. সেবার সীমাবদ্ধতা ও তথ্যের প্রকৃতি" else "2. Service & Information Limitations",
                            body = if (language == AppLanguage.BN) {
                                "• এই অ্যাপটি একটি ডিজিটাল তথ্য ও নাগরিক যোগাযোগ পোর্টাল।\n" +
                                "• প্রদর্শিত আবহাওয়া, সমুদ্র সতর্ক সংকেত, জোয়ার-ভাটা এবং সি-ট্রাক/স্পিডবোটের সময়সূচি স্থানীয় কর্তৃপক্ষের সিদ্ধান্ত ও প্রাকৃতিক অবস্থার ওপর নির্ভরশীল এবং তা পূর্ব ঘোষণা ছাড়াই পরিবর্তিত হতে পারে।\n" +
                                "• জরুরি হেল্পলাইন, জনপ্রতিনিধি ডিরেক্টরি ও রক্তদাতা তথ্য জনস্বার্থে প্রদত্ত; বাস্তব জরুরি পরিস্থিতিতে সরাসরি দায়িত্বপ্রাপ্ত প্রতিষ্ঠানের সাথে যোগাযোগ নিশ্চিত করুন।"
                            } else {
                                "• This application operates as an informational and citizen communication portal.\n" +
                                "• Displayed weather, maritime signals, tide timings, and sea-truck/speedboat schedules depend on local authority advisories and sea conditions and may change without prior notice.\n" +
                                "• Emergency helplines, local representative contacts, and blood donor listings are provided as a public convenience; always verify directly with the relevant authorities during emergencies."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৩. নিষিদ্ধ কার্যক্রম" else "3. Prohibited Conduct",
                            body = if (language == AppLanguage.BN) {
                                "অ্যাপের মাধ্যমে ভুয়া আবেদন বা অভিযোগ পাঠানো, স্প্যামিং, অশালীন/উসকানিমূলক বা মানহানিকর বক্তব্য প্রকাশ, অন্য ব্যক্তির পরিচয় ব্যবহার করা কিংবা অ্যাপ ও ডেটাবেজের স্বাভাবিক কার্যক্রমে বিঘ্ন ঘটানোর চেষ্টা সম্পূর্ণ নিষিদ্ধ। শর্ত ভঙ্গ করলে সংশ্লিষ্ট এন্ট্রি মুছে ফেলা বা সেবা ব্যবহারে সীমাবদ্ধতা আরোপ করা হতে পারে।"
                            } else {
                                "Submitting fabricated complaints or applications, spamming, posting abusive or defamatory content, impersonating another person, or attempting to disrupt app or backend security is strictly prohibited. Violations may result in removal of submissions or restricted access."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৪. বাহ্যিক ওয়েবসাইট ও লিংক" else "4. External Links",
                            body = if (language == AppLanguage.BN) {
                                "গ্যালারি, সংবাদ আর্কাইভ ও সামাজিক মাধ্যম অংশে যুক্ত তৃতীয় পক্ষের ওয়েবসাইট বা ভিডিও লিংকের কনটেন্ট এবং প্রাপ্যতার জন্য এই পোর্টাল দায়ী নয়।"
                            } else {
                                "This portal is not responsible for the availability or content of third-party websites, media sources, or map services opened via external links."
                            }
                        )
                    }

                    item {
                        FooterInfoSectionCard(
                            title = if (language == AppLanguage.BN) "৫. শর্তাবলী পরিবর্তন ও যোগাযোগ" else "5. Updates & Contact",
                            body = if (language == AppLanguage.BN) {
                                "জনসেবার মানোন্নয়ন ও নীতিগত প্রয়োজনে কর্তৃপক্ষ এই শর্তাবলী হালনাগাদ করতে পারে। শর্তাবলী সংক্রান্ত যেকোনো প্রশ্নে যোগাযোগ করুন: ${appConfig.contactEmail} অথবা ${appConfig.hotlinePhone}।"
                            } else {
                                "These terms may be updated to reflect portal improvements or policy needs. For questions regarding these terms, contact ${appConfig.contactEmail} or ${appConfig.hotlinePhone}."
                            }
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(48.dp)
                            .testTag("terms_close_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800)
                    ) {
                        Text(if (language == AppLanguage.BN) "সম্মত ও বন্ধ করুন" else "I Agree & Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterInfoSectionCard(
    title: String,
    body: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = RoyalBlue900
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = body,
                fontSize = 13.sp,
                color = Color(0xFF334155),
                lineHeight = 20.sp
            )
        }
    }
}

// --- 13. General Feedback Form Dialog ---
@Composable
fun GeneralFeedbackDialog(
    language: AppLanguage,
    currentUserId: String? = null,
    isSubmitting: Boolean = false,
    onSubmit: (name: String, phone: String, union: String, category: String, message: String, attachmentUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var union by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var attachmentUrl by remember { mutableStateOf("") }
    var isSigningIn by remember { mutableStateOf(false) }

    val unionOptions = listOf(
        "মাতারবাড়ি" to "Matarbari",
        "ধলঘাটা" to "Dhalghata",
        "কালারমারছড়া" to "Kalarmarchhara",
        "শাপলাপুর" to "Shaplapur",
        "হোয়ানক" to "Hoanak",
        "বড় মহেশখালী" to "Boro Moheshkhali",
        "ছোট মহেশখালী" to "Choto Moheshkhali",
        "কুতুবজোম" to "Kutubjom",
        "বড়ঘোপ" to "Boroghop",
        "আলী আকবর ডেইল" to "Ali Akbar Deil",
        "কৈয়ারবিল" to "Kaiyarbil",
        "লেমশীখালী" to "Lemshikhali",
        "উত্তর ধুরুং" to "Uttar Dhurung",
        "দক্ষিণ ধুরুং" to "Dakshin Dhurung"
    )
    val categoryOptions = listOf(
        "উন্নয়ন পরামর্শ" to "Development Suggestion",
        "অভিযোগ" to "Complaint",
        "প্রশংসা" to "Appreciation",
        "প্রযুক্তিগত সমস্যা" to "Technical Issue",
        "অন্যান্য" to "Other"
    )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f).testTag("feedback_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue800, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "মতামত ও পরামর্শ" else "Feedback & Suggestions",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentUserId.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BN)
                                            "মতামত পাঠাতে অনুগ্রহ করে Google অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
                                        else
                                            "Please sign in with Google to submit feedback.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            if (!isSigningIn) {
                                                isSigningIn = true
                                                AuthManager.signInWithGoogle(
                                                    context = context,
                                                    credentialManager = credentialManager,
                                                    scope = scope,
                                                    onAuthSuccess = { isSigningIn = false },
                                                    onAuthError = { err ->
                                                        isSigningIn = false
                                                        android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                                    },
                                                    onAuthCancelled = { isSigningIn = false }
                                                )
                                            }
                                        },
                                        enabled = !isSigningIn,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("feedback_google_signin_button")
                                    ) {
                                        Text(
                                            if (language == AppLanguage.BN) "Google দিয়ে সাইন-ইন করুন" else "Sign in with Google",
                                            fontWeight = FontWeight.SemiBold,
                                            color = RoyalBlue800
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = name, onValueChange = { name = it },
                            label = { Text(if (language == AppLanguage.BN) "আপনার নাম" else "Your Name") },
                            modifier = Modifier.fillMaxWidth().testTag("feedback_name_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it },
                            label = { Text(if (language == AppLanguage.BN) "মোবাইল নম্বর" else "Phone Number") },
                            modifier = Modifier.fillMaxWidth().testTag("feedback_phone_input")
                        )
                    }
                    item {
                        Text(if (language == AppLanguage.BN) "আপনার ইউনিয়ন" else "Your Union", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(unionOptions) { (uBn, uEn) ->
                                val label = if (language == AppLanguage.BN) uBn else uEn
                                FilterChip(
                                    selected = union == uBn,
                                    onClick = { union = uBn },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    item {
                        Text(if (language == AppLanguage.BN) "মতামতের ধরন" else "Feedback Category", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categoryOptions) { (cBn, cEn) ->
                                val label = if (language == AppLanguage.BN) cBn else cEn
                                FilterChip(
                                    selected = category == cBn,
                                    onClick = { category = cBn },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = message, onValueChange = { message = it },
                            label = { Text(if (language == AppLanguage.BN) "বিস্তারিত মতামত" else "Detailed Feedback") },
                            modifier = Modifier.fillMaxWidth().height(120.dp).testTag("feedback_message_input"),
                            maxLines = 5
                        )
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = {
                            if (currentUserId.isNullOrBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "মতামত পাঠাতে প্রথমে Google দিয়ে সাইন-ইন করুন" else "Please sign in with Google first",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            val trimmedName = name.trim()
                            val trimmedPhone = phone.trim()
                            val trimmedMessage = message.trim()
                            if (trimmedName.isBlank() || trimmedPhone.length < 6 || trimmedMessage.isBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "দয়া করে নাম, সঠিক মোবাইল নম্বর (কমপক্ষে ৬ অঙ্ক) এবং মতামত দিন" else "Please enter your name, valid phone (at least 6 chars), and feedback",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            onSubmit(trimmedName, trimmedPhone, union, category, trimmedMessage, attachmentUrl)
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_feedback_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (language == AppLanguage.BN) "মতামত পাঠান" else "Submit Feedback", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
