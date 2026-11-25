package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Route filter selection states
 */
enum class RouteFilterState(val titleBn: String, val titleEn: String) {
    ALL("সকল রুট", "All Routes"),
    KUTUBDIA_MOGNAMA("কুতুবদিয়া - মগনামা", "Kutubdia - Mognama"),
    MAHESHKHALI_COXSBAZAR("মহেশখালী - কক্সবাজার", "Maheshkhali - Cox's Bazar")
}

/**
 * Dedicated Sea-Truck & Watercraft Schedule Screen for Maheshkhali and Kutubdia
 * Features:
 * - Clean top app bar titled 'সি-ট্রাক ও নৌযান সময়সূচি'
 * - Route Filter Tabs ('সকল রুট', 'কুতুবদিয়া - মগনামা', 'মহেশখালী - কক্সবাজার')
 * - Scrolling Safety Notice (Marquee Ticker Banner)
 * - Tide Schedule Card (জোয়ার-ভাটার সময়সূচি) with wave icon & High/Low tide indicators
 * - Kutubdia - Mognama Route Card with capsule-style time chips
 * - Maheshkhali - Cox's Bazar Route Card with capsule-style time chips
 * - Color-coded departure boxes (Blue for origin departure, Green for return)
 * - Italicized Footer Note ('প্রাথমিক প্রকাশিত সময়সূচি • সময়সূচি পরিবর্তন সাপেক্ষে')
 * - System BackHandler & top bar navigation back arrow
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatercraftScheduleScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    onRefreshTide: () -> Unit,
    modifier: Modifier = Modifier,
    weatherData: WeatherData = WeatherData(),
    dynamicSchedules: List<DynamicWatercraftSchedule> = emptyList(),
    dynamicTideSchedule: DynamicTideSchedule = DynamicTideSchedule(),
    onOpenAdmin: (() -> Unit)? = null
) {
    // Hardware/gesture back press handler
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(RouteFilterState.ALL) }
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("watercraft_schedule_screen"),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RoyalBlue800)
            ) {
                // Header Top Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "সি-ট্রাক ও নৌযান সময়সূচি" else "Sea-Truck Schedule",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "মহেশখালী ও কুতুবদিয়া উপকূলীয় নৌপথ" else "Maheshkhali & Kutubdia Routes",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (language == AppLanguage.BN) "ফিরে যান" else "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Styled Refresh Button with icon + label matching deep blue/teal design
                        Button(
                            onClick = {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    onRefreshTide()
                                    coroutineScope.launch {
                                        delay(750)
                                        isRefreshing = false
                                        Toast.makeText(
                                            context,
                                            if (language == AppLanguage.BN) "নৌযান ও জোয়ার সূচি সফলভাবে রিফ্রেশ হয়েছে" else "Schedules refreshed successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            enabled = !isRefreshing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OceanTeal,
                                disabledContainerColor = OceanTeal.copy(alpha = 0.7f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("refresh_tide_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = if (language == AppLanguage.BN) "রিফ্রেশ" else "Refresh",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRefreshing) {
                                    if (language == AppLanguage.BN) "রিফ্রেশ হচ্ছে..." else "Refreshing..."
                                } else {
                                    if (language == AppLanguage.BN) "রিফ্রেশ করুন" else "Refresh"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (onOpenAdmin != null) {
                            IconButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier.size(34.dp).testTag("admin_watercraft_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditCalendar,
                                    contentDescription = "Edit Schedule",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4ADE80))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "সক্রিয়" else "Live",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = RoyalBlue800,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )

                // Route Filter Tabs Bar ('সকল রুট', 'কুতুবদিয়া - মগনামা', 'মহেশখালী - কক্সবাজার')
                RouteFilterTabBar(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it },
                    language = language
                )
            }
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrolling Safety Notice (Marquee Ticker Banner) right below tabs
            SafetyMarqueeTickerBanner(
                language = language,
                customNotice = if (language == AppLanguage.BN) dynamicTideSchedule.safetyMarqueeBn else dynamicTideSchedule.safetyMarqueeEn
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Tide Schedule Card (জোয়ার-ভাটার সময়সূচি)
                TideScheduleCard(
                    weatherData = weatherData,
                    tideSchedule = dynamicTideSchedule,
                    language = language
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Route Cards based on Filter Selection
                AnimatedVisibility(
                    visible = selectedFilter == RouteFilterState.ALL || selectedFilter == RouteFilterState.KUTUBDIA_MOGNAMA,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        KutubdiaMognamaRouteCard(
                            language = language,
                            schedules = dynamicSchedules.filter { it.routeGroup == "KUTUBDIA_MOGNAMA" }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                AnimatedVisibility(
                    visible = selectedFilter == RouteFilterState.ALL || selectedFilter == RouteFilterState.MAHESHKHALI_COXSBAZAR,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        MaheshkhaliCoxsBazarRouteCard(
                            language = language,
                            schedules = dynamicSchedules.filter { it.routeGroup == "MAHESHKHALI_COXSBAZAR" }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Passenger Safety Advisory
                PassengerAdvisoryCard(language = language)

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Note with Italicized text: 'প্রাথমিক প্রকাশিত সময়সূচি • সময়সূচি পরিবর্তন সাপেক্ষে'
                FooterScheduleNote(language = language)

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Route Filter Tabs Bar ('সকল রুট', 'কুতুবদিয়া - মগনামা', 'মহেশখালী - কক্সবাজার')
 */
@Composable
private fun RouteFilterTabBar(
    selectedFilter: RouteFilterState,
    onFilterSelected: (RouteFilterState) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        color = RoyalBlue900,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RouteFilterState.values().forEach { filter ->
                val isSelected = (filter == selectedFilter)
                val label = if (language == AppLanguage.BN) filter.titleBn else filter.titleEn

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onFilterSelected(filter) }
                        .testTag("tab_filter_${filter.name.lowercase()}")
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalBlue900 else Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Scrolling Safety Notice (Marquee Ticker Banner)
 * '⚠️ বিশেষ সতর্কতা: ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা অবশ্যই মেনে চলুন।'
 */
@Composable
private fun SafetyMarqueeTickerBanner(
    language: AppLanguage,
    customNotice: String = "",
    modifier: Modifier = Modifier
) {
    val marqueeNotice = if (customNotice.isNotBlank()) {
        "$customNotice   •   $customNotice"
    } else if (language == AppLanguage.BN) {
        "⚠️ বিশেষ সতর্কতা: ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা অবশ্যই মেনে চলুন।   •   🌊 ভাটার সময় জেটিতে বাড়তি সতর্কতা অবলম্বন করুন।   •   ⚠️ বিশেষ সতর্কতা: ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা অবশ্যই মেনে চলুন।"
    } else {
        "⚠️ Safety Alert: Follow security guidelines strictly when using jetties during low tide.   •   🌊 Exercise caution at landing ghats during ebb tide."
    }

    Surface(
        color = Color(0xFFFEF3C7), // Light amber/orange warning background
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_marquee_ticker")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFD97706)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "জরুরি বার্তা" else "ALERT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = marqueeNotice,
                color = Color(0xFF92400E),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity = 45.dp
                    )
            )
        }
    }
}

/**
 * Tide Schedule Card (জোয়ার-ভাটার সময়সূচি)
 * Visual thumbnail/card layout with a water wave icon displaying daily High Tide and Low Tide status:
 * জোয়ার (High Tide): প্রথম জোয়ার • দ্বিতীয় জোয়ার
 * ভাটা (Low Tide): প্রথম ভাটা • দ্বিতীয় ভাটা
 */
@Composable
private fun TideScheduleCard(
    weatherData: WeatherData,
    tideSchedule: DynamicTideSchedule = DynamicTideSchedule(),
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val statusText = if (language == AppLanguage.BN) tideSchedule.statusBn else tideSchedule.statusEn

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tide_schedule_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header: Title with Wave Icon & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Wave Icon",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (language == AppLanguage.BN) "জোয়ার-ভাটার সময়সূচি" else "Daily Tide Schedule",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "উপকূলীয় জোয়ার ও ভাটা পর্যবেক্ষণ" else "Coastal High & Low Tide Monitoring",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two Visual Columns: High Tide (জোয়ার) & Low Tide (ভাটা)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. High Tide Card (জোয়ার)
                TidePhaseBox(
                    isHighTide = true,
                    title = if (language == AppLanguage.BN) "জোয়ার (High Tide)" else "High Tide (জোয়ার)",
                    badgeText = if (language == AppLanguage.BN) "পানি বৃদ্ধি" else "Rising Water",
                    firstPhaseLabel = if (language == AppLanguage.BN) "প্রথম জোয়ার" else "1st High Tide",
                    firstPhaseTime = if (language == AppLanguage.BN) tideSchedule.highTide1Bn else tideSchedule.highTide1En,
                    secondPhaseLabel = if (language == AppLanguage.BN) "দ্বিতীয় জোয়ার" else "2nd High Tide",
                    secondPhaseTime = if (language == AppLanguage.BN) tideSchedule.highTide2Bn else tideSchedule.highTide2En,
                    modifier = Modifier.weight(1f)
                )

                // 2. Low Tide Card (ভাটা)
                TidePhaseBox(
                    isHighTide = false,
                    title = if (language == AppLanguage.BN) "ভাটা (Low Tide)" else "Low Tide (ভাটা)",
                    badgeText = if (language == AppLanguage.BN) "পানি হ্রাস" else "Ebb Water",
                    firstPhaseLabel = if (language == AppLanguage.BN) "প্রথম ভাটা" else "1st Low Tide",
                    firstPhaseTime = if (language == AppLanguage.BN) tideSchedule.lowTide1Bn else tideSchedule.lowTide1En,
                    secondPhaseLabel = if (language == AppLanguage.BN) "দ্বিতীয় ভাটা" else "2nd Low Tide",
                    secondPhaseTime = if (language == AppLanguage.BN) tideSchedule.lowTide2Bn else tideSchedule.lowTide2En,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info hint
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = RoyalBlue700,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BN)
                            "জোয়ার ও ভাটার সময় সাগর ও খালের পানির উচ্চতা প্রায় ৩–৪ মিটার পরিবর্তিত হয়।"
                        else
                            "Water depth at coastal channels fluctuates approx 3–4 meters between tides.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Individual High/Low Tide Phase Box
 */
@Composable
private fun TidePhaseBox(
    isHighTide: Boolean,
    title: String,
    badgeText: String,
    firstPhaseLabel: String,
    firstPhaseTime: String,
    secondPhaseLabel: String,
    secondPhaseTime: String,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isHighTide) Color(0xFFF0F9FF) else Color(0xFFF0FDFA)
    val borderCol = if (isHighTide) Color(0xFFBAE6FD) else Color(0xFF99F6E4)
    val accentCol = if (isHighTide) Color(0xFF0284C7) else Color(0xFF0D9488)
    val badgeBg = if (isHighTide) Color(0xFF0369A1) else Color(0xFF0F766E)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentCol
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeBg,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1st Phase
            TideTimingRow(label = firstPhaseLabel, time = firstPhaseTime, accentColor = accentCol)

            Spacer(modifier = Modifier.height(6.dp))

            // 2nd Phase
            TideTimingRow(label = secondPhaseLabel, time = secondPhaseTime, accentColor = accentCol)
        }
    }
}

@Composable
private fun TideTimingRow(
    label: String,
    time: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.85f),
        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = time,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

/**
 * Kutubdia - Mognama Route Card
 */
/**
 * Kutubdia - Mognama Route Card
 */
@Composable
private fun KutubdiaMognamaRouteCard(
    language: AppLanguage,
    schedules: List<DynamicWatercraftSchedule> = emptyList(),
    modifier: Modifier = Modifier
) {
    val originSchedule = schedules.firstOrNull { it.isOriginDeparture }
    val returnSchedule = schedules.firstOrNull { !it.isOriginDeparture }

    val originTitle = if (originSchedule != null) {
        if (language == AppLanguage.BN) originSchedule.routeTitleBn else originSchedule.routeTitleEn
    } else {
        if (language == AppLanguage.BN) "কুতুবদিয়া থেকে মগনামা" else "Kutubdia to Mognama"
    }

    val originNotice = if (originSchedule != null) {
        if (language == AppLanguage.BN) originSchedule.officialNoticeTextBn else originSchedule.officialNoticeTextEn
    } else {
        "কুতুবদিয়া থেকে মগনামা: সকাল ৭:৩০ মিনিট, দুপুর ১২:০০ মিনিট, বিকাল ৫:০০ মিনিট।"
    }

    val originSlots = if (originSchedule != null && originSchedule.timeSlotsBn.isNotEmpty()) {
        if (language == AppLanguage.BN) originSchedule.timeSlotsBn else originSchedule.timeSlotsEn.ifEmpty { originSchedule.timeSlotsBn }
    } else {
        listOf("সকাল ৭:৩০ মিনিট", "দুপুর ১২:০০ মিনিট", "বিকাল ৫:০০ মিনিট")
    }

    val returnTitle = if (returnSchedule != null) {
        if (language == AppLanguage.BN) returnSchedule.routeTitleBn else returnSchedule.routeTitleEn
    } else {
        if (language == AppLanguage.BN) "মগনামা থেকে কুতুবদিয়া" else "Mognama to Kutubdia"
    }

    val returnNotice = if (returnSchedule != null) {
        if (language == AppLanguage.BN) returnSchedule.officialNoticeTextBn else returnSchedule.officialNoticeTextEn
    } else {
        "মগনামা থেকে কুতুবদিয়া: সকাল ৯:০০ মিনিট, দুপুর ২:০০ মিনিট, সন্ধ্যা ৭:০০ মিনিট।"
    }

    val returnSlots = if (returnSchedule != null && returnSchedule.timeSlotsBn.isNotEmpty()) {
        if (language == AppLanguage.BN) returnSchedule.timeSlotsBn else returnSchedule.timeSlotsEn.ifEmpty { returnSchedule.timeSlotsBn }
    } else {
        listOf("সকাল ৯:০০ মিনিট", "দুপুর ২:০০ মিনিট", "সন্ধ্যা ৭:০০ মিনিট")
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("kutubdia_mognama_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sailing,
                            contentDescription = null,
                            tint = RoyalBlue800,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.BN) "কুতুবদিয়া – মগনামা রুট" else "Kutubdia – Mognama Route",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "বিআইডব্লিউটিসি সি-ট্রাক ও ঘাট সার্ভিস" else "BIWTC Sea-Truck & Ghat Service",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlue50
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "সি-ট্রাক" else "Sea-Truck",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue800,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Origin Departure Box (Color-coded Blue)
            DepartureScheduleBox(
                isOriginDeparture = true,
                routeTitle = originTitle,
                officialNoticeText = originNotice,
                timeSlots = originSlots,
                testTag = "box_kutubdia_to_mognama"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Return Departure Box (Color-coded Green)
            DepartureScheduleBox(
                isOriginDeparture = false,
                routeTitle = returnTitle,
                officialNoticeText = returnNotice,
                timeSlots = returnSlots,
                testTag = "box_mognama_to_kutubdia"
            )
        }
    }
}

/**
 * Maheshkhali - Cox's Bazar Route Card
 */
@Composable
private fun MaheshkhaliCoxsBazarRouteCard(
    language: AppLanguage,
    schedules: List<DynamicWatercraftSchedule> = emptyList(),
    modifier: Modifier = Modifier
) {
    val originSchedule = schedules.firstOrNull { it.isOriginDeparture }
    val returnSchedule = schedules.firstOrNull { !it.isOriginDeparture }

    val originTitle = if (originSchedule != null) {
        if (language == AppLanguage.BN) originSchedule.routeTitleBn else originSchedule.routeTitleEn
    } else {
        if (language == AppLanguage.BN) "মহেশখালী হইতে কক্সবাজার" else "Maheshkhali to Cox's Bazar"
    }

    val originNotice = if (originSchedule != null) {
        if (language == AppLanguage.BN) originSchedule.officialNoticeTextBn else originSchedule.officialNoticeTextEn
    } else {
        "মহেশখালী হইতে কক্সবাজার: সকাল ৭:০০ মিনিট, সকাল ০৯:৪৫ মিনিট, সকাল ১১:০০ মিনিট, দুপুর ২:০০ মিনিট, বিকাল ৫:০০ টা, রাত ৭:০০ মিনিট।"
    }

    val originSlots = if (originSchedule != null && originSchedule.timeSlotsBn.isNotEmpty()) {
        if (language == AppLanguage.BN) originSchedule.timeSlotsBn else originSchedule.timeSlotsEn.ifEmpty { originSchedule.timeSlotsBn }
    } else {
        listOf(
            "সকাল ৭:০০ মিনিট",
            "সকাল ০৯:৪৫ মিনিট",
            "সকাল ১১:০০ মিনিট",
            "দুপুর ২:০০ মিনিট",
            "বিকাল ৫:০০ টা",
            "রাত ৭:০০ মিনিট"
        )
    }

    val returnTitle = if (returnSchedule != null) {
        if (language == AppLanguage.BN) returnSchedule.routeTitleBn else returnSchedule.routeTitleEn
    } else {
        if (language == AppLanguage.BN) "কক্সবাজার হইতে মহেশখালী" else "Cox's Bazar to Maheshkhali"
    }

    val returnNotice = if (returnSchedule != null) {
        if (language == AppLanguage.BN) returnSchedule.officialNoticeTextBn else returnSchedule.officialNoticeTextEn
    } else {
        "কক্সবাজার হইতে মহেশখালী: সকাল ৮:০০ মিনিট, সকাল ১০:০০ মিনিট, দুপুর ১২:০০ মিনিট, বিকাল ৩:০০ মিনিট, সন্ধ্যা ৬:০০ টা, রাত ৯:০০ মিনিট।"
    }

    val returnSlots = if (returnSchedule != null && returnSchedule.timeSlotsBn.isNotEmpty()) {
        if (language == AppLanguage.BN) returnSchedule.timeSlotsBn else returnSchedule.timeSlotsEn.ifEmpty { returnSchedule.timeSlotsBn }
    } else {
        listOf(
            "সকাল ৮:০০ মিনিট",
            "সকাল ১০:০০ মিনিট",
            "দুপুর ১২:০০ মিনিট",
            "বিকাল ৩:০০ মিনিট",
            "সন্ধ্যা ৬:০০ টা",
            "রাত ৯:০০ মিনিট"
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("maheshkhali_coxsbazar_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F7FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBoat,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.BN) "মহেশখালী – কক্সবাজার রুট" else "Maheshkhali – Cox's Bazar Route",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "গোরকঘাটা জেটি ঘাট ⇄ ৬নং নুনিয়াছড়া ঘাট" else "Gorokghata Jetty ⇄ 6 No. Nuniachhara",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE6F4EA)
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "স্পিডবোট" else "Speedboat",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenSuccess,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Origin Departure Box (Color-coded Blue)
            DepartureScheduleBox(
                isOriginDeparture = true,
                routeTitle = originTitle,
                officialNoticeText = originNotice,
                timeSlots = originSlots,
                testTag = "box_maheshkhali_to_coxsbazar"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Return Departure Box (Color-coded Green)
            DepartureScheduleBox(
                isOriginDeparture = false,
                routeTitle = returnTitle,
                officialNoticeText = returnNotice,
                timeSlots = returnSlots,
                testTag = "box_coxsbazar_to_maheshkhali"
            )
        }
    }
}

/**
 * Color-coded Departure Box (Blue for Origin Departure, Green for Return Departure)
 * Uses capsule-style chips with schedule icons for all departure times.
 */
@Composable
private fun DepartureScheduleBox(
    isOriginDeparture: Boolean,
    routeTitle: String,
    officialNoticeText: String,
    timeSlots: List<String>,
    testTag: String,
    modifier: Modifier = Modifier
) {
    // Blue tint for origin, Green tint for return
    val containerColor = if (isOriginDeparture) Color(0xFFEFF6FF) else Color(0xFFF0FDF4)
    val borderColor = if (isOriginDeparture) Color(0xFFBFDBFE) else Color(0xFFBBF7D0)
    val headerBadgeBg = if (isOriginDeparture) RoyalBlue800 else GreenSuccess
    val textColor = if (isOriginDeparture) RoyalBlue900 else Color(0xFF14532D)
    val pillBg = if (isOriginDeparture) Color(0xFFDBEAFE) else Color(0xFFDCFCE7)
    val pillBorder = if (isOriginDeparture) Color(0xFF93C5FD) else Color(0xFF86EFAC)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Direction Label Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(headerBadgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOriginDeparture)
                                Icons.AutoMirrored.Filled.TrendingFlat
                            else
                                Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = routeTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = headerBadgeBg.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isOriginDeparture) "যাত্রা শুরু" else "প্রত্যাবর্তন",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerBadgeBg,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Exact official schedule text
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.9f),
                border = BorderStroke(0.8.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = officialNoticeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Capsule-Style Time Chips Grid with Schedule Icons
            Text(
                text = "সময়সূচির তালিকা (ক্যাপসুল ভিউ):",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Render time slots in capsule-style chips
            val chunkedSlots = timeSlots.chunked(3)
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (chunk in chunkedSlots) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (slot in chunk) {
                            Surface(
                                shape = RoundedCornerShape(20.dp), // Capsule style
                                color = pillBg,
                                border = BorderStroke(1.dp, pillBorder),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = headerBadgeBg,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = slot,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        // Fill extra space if chunk is less than 3
                        for (i in chunk.size until 3) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Passenger Advisory and Guidelines
 */
@Composable
private fun PassengerAdvisoryCard(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.BN) "যাত্রী সাধারণের জন্য নিরাপত্তা নির্দেশনা" else "Passenger Safety Advisory",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AdvisoryBulletItem(
                text = if (language == AppLanguage.BN)
                    "নৌযানে ওঠার পর বাধ্যতামূলকভাবে লাইফ জ্যাকেট পরিধান করুন।"
                else
                    "Wearing a life jacket is mandatory while on board any watercraft."
            )
            AdvisoryBulletItem(
                text = if (language == AppLanguage.BN)
                    "সমুদ্র বন্দরে ৩ নম্বর বা তদূর্ধ্ব স্থানীয় সতর্ক সংকেত থাকলে সকল নৌযান চলাচল সাময়িক স্থগিত থাকবে।"
                else
                    "Vessel operations will be temporarily suspended if Local Warning Signal 3 or higher is issued."
            )
            AdvisoryBulletItem(
                text = if (language == AppLanguage.BN)
                    "নৌযানের ধারণক্ষমতার অতিরিক্ত যাত্রী বহন আইনত দণ্ডনীয় অপরাধ।"
                else
                    "Overloading vessels beyond certified passenger capacity is strictly prohibited."
            )
        }
    }
}

@Composable
private fun AdvisoryBulletItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB45309),
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = Color(0xFF78350F),
            lineHeight = 16.sp
        )
    }
}

/**
 * Footer Note with exact required italic text:
 * 'প্রাথমিক প্রকাশিত সময়সূচি • সময়সূচি পরিবর্তন সাপেক্ষে'
 */
@Composable
private fun FooterScheduleNote(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("footer_schedule_note")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = RoyalBlue700,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.BN)
                    "প্রাথমিক প্রকাশিত সময়সূচি • সময়সূচি পরিবর্তন সাপেক্ষে"
                else
                    "Initial Published Schedule • Subject to change based on weather and tides",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = RoyalBlue900,
                textAlign = TextAlign.Center
            )
        }
    }
}
