package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.TourismSpot
import com.example.ui.components.AsyncOrLocalImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourismPortalSheet(
    spots: List<TourismSpot>,
    selectedCategory: String?,
    language: AppLanguage,
    onCategorySelected: (String?) -> Unit,
    onSpotSelected: (TourismSpot) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BackgroundLight,
        modifier = modifier
            .fillMaxSize()
            .testTag("tourism_portal_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                color = RoyalBlue800,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("tourism_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BN) "পর্যটন ও ভ্রমণ গাইড" else "Tourism & Travel Guide",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == AppLanguage.BN) "মহেশখালী ও কুতুবদিয়া দ্বীপপুঞ্জ" else "Moheshkhali & Kutubdia Archipelago",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldAccent.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "${spots.size} টি স্পট" else "${spots.size} Spots",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Category Filter Row (All, Moheshkhali, Kutubdia)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(
                    null to (if (language == AppLanguage.BN) "সকল স্থান" else "All Spots"),
                    "Moheshkhali" to (if (language == AppLanguage.BN) "মহেশখালী (আদিনাথ ও সোনাদিয়া)" else "Moheshkhali"),
                    "Kutubdia" to (if (language == AppLanguage.BN) "কুতুবদিয়া (বাতিঘর ও সৈকত)" else "Kutubdia")
                )

                items(categories) { (cat, label) ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(cat) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue800,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("tourism_chip_${cat ?: "all"}")
                    )
                }
            }

            // Spots List
            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 24.dp + navBarBottom)
            ) {
                if (spots.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "এই বিভাগে কোনো পর্যটন স্পট পাওয়া যায়নি।" else "No tourism spots available in this category.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(spots, key = { it.spotId }) { spot ->
                        TourismSpotCard(
                            spot = spot,
                            language = language,
                            onClick = { onSpotSelected(spot) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TourismSpotCard(
    spot: TourismSpot,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("tourism_spot_${spot.spotId}")
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image with Tag Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncOrLocalImage(
                    imageUrl = spot.coverImageUrl,
                    contentDescription = if (language == AppLanguage.BN) spot.titleBn else spot.titleEn,
                    modifier = Modifier.fillMaxSize()
                )

                // Category pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (spot.category == "Moheshkhali") RoyalBlue800 else Color(0xFF00695C),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (spot.category == "Moheshkhali") {
                            if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali"
                        } else {
                            if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Fare chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBoat,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = spot.approxFareBdt,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Info Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) spot.titleBn else spot.titleEn,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (language == AppLanguage.BN) spot.descriptionBn else spot.descriptionEn,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Transport Tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    spot.transportTypes.take(2).forEach { transport ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = transport,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "রুট ও বিস্তারিত তথ্য" else "View Route & Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800
                    )

                    Button(
                        onClick = onClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "বিস্তারিত" else "Explore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TourismSpotDetailDialog(
    spot: TourismSpot,
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
                .fillMaxHeight(0.9f)
                .testTag("tourism_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Image with close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    AsyncOrLocalImage(
                        imageUrl = spot.coverImageUrl,
                        contentDescription = if (language == AppLanguage.BN) spot.titleBn else spot.titleEn,
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
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RoyalBlue800,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (spot.category == "Moheshkhali") {
                                if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali"
                            } else {
                                if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Scrollable details body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = if (language == AppLanguage.BN) spot.titleBn else spot.titleEn,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBlue900
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (language == AppLanguage.BN) spot.descriptionBn else spot.descriptionEn,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    // Travel Route from Cox's Bazar
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBoat,
                                        contentDescription = null,
                                        tint = RoyalBlue800,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "কক্সবাজার থেকে যাতায়াতের রুট" else "Travel Route from Cox's Bazar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue800
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) spot.travelRouteDetailsBn else spot.travelRouteDetailsEn,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "আনুমানিক ভাড়া: ${spot.approxFareBdt}" else "Approx Fare: ${spot.approxFareBdt}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF00695C)
                                )
                            }
                        }
                    }

                    // Navigation Steps
                    item {
                        Text(
                            text = if (language == AppLanguage.BN) "ধাপে ধাপে দিকনির্দেশনা (Navigation)" else "Step-by-Step Navigation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val steps = if (language == AppLanguage.BN) spot.navigationStepsBn else spot.navigationStepsEn
                        steps.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OceanTeal,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .offset(y = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Recommended Hotels & Seafood
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Hotel,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "থাকা ও খাওয়ার ব্যবস্থা" else "Stay & Dining Options",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                val hotels = if (language == AppLanguage.BN) spot.recommendedHotelsBn else spot.recommendedHotelsEn
                                Text(
                                    text = if (language == AppLanguage.BN) "🏨 হোটেল / রেস্টহাউজ: " + hotels.joinToString(", ") else "🏨 Hotels: " + hotels.joinToString(", "),
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val seafood = if (language == AppLanguage.BN) spot.seafoodRestaurantsBn else spot.seafoodRestaurantsEn
                                Text(
                                    text = if (language == AppLanguage.BN) "🐟 সামুদ্রিক খাবারের রেস্তোরাঁ: " + seafood.joinToString(", ") else "🐟 Seafood Diners: " + seafood.joinToString(", "),
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Action Buttons Footer
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
                        OutlinedButton(
                            onClick = {
                                val title = if (language == AppLanguage.BN) spot.titleBn else spot.titleEn
                                com.example.util.IntentUtils.safeShareText(
                                    context = context,
                                    subject = title,
                                    text = "$title\n${spot.googleMapsUrl}",
                                    chooserTitle = if (language == AppLanguage.BN) "শেয়ার" else "Share",
                                    language = language
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (language == AppLanguage.BN) "শেয়ার" else "Share", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                com.example.util.IntentUtils.safeOpenUrl(context, spot.googleMapsUrl, language)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(46.dp)
                                .testTag("open_maps_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "গুগল ম্যাপে দেখুন" else "Google Maps",
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
