package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.*
import com.example.data.repository.DefaultPortalContent
import com.example.ui.theme.*
import com.example.util.MediaStorageManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun HeroBanner(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    customVideoSource: String? = null,
    customPosterSource: String? = null,
    isMuted: Boolean = true,
    onToggleMute: () -> Unit = {}
) {
    val context = LocalContext.current
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isMuted) {
        mediaPlayer?.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .testTag("hero_banner")
    ) {
        // 1. Poster Image Background
        val posterSource = customPosterSource?.takeIf { it.isNotBlank() } ?: "drawable:banner_video_poster"
        AsyncOrLocalImage(
            imageUrl = posterSource,
            contentDescription = "Moheshkhali-Kutubdia Coast & Infrastructure",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Video Player for uploaded banner
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    videoViewRef = this
                    val videoUri = MediaStorageManager.resolveVideoUri(ctx, customVideoSource)
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mediaPlayer = mp
                        mp.isLooping = true
                        mp.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                }
            }
        )

        // Sound Toggle Button
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White
            )
        }

        // 3. Cinematic gradient overlay for optimal readability of white text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )

        // 4. Clean Typography and Badges Overlay (Seamless without player overlay or buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Main Bold Headline
            Text(
                text = if (language == AppLanguage.BN) "মহেশখালী-কুতুবদিয়ার উন্নয়ন ও অগ্রযাত্রা" else "Moheshkhali-Kutubdia Development & Progress",
                color = Color.White,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Rounded Decade Pill Badge: "২০১৪ – ২০২৪"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00796B),
                    border = BorderStroke(0.8.dp, Color(0xFF80CBC4).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "২০১৪ – ২০২৪" else "2014 – 2024",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Subtitle
                Text(
                    text = if (language == AppLanguage.BN) "সামাজিক, অর্থনৈতিক ও অবকাঠামোগত উন্নয়নের এক দশক" else "A decade of social, economic & infrastructure progress",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Coast Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalBlue900.copy(alpha = 0.75f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "মহেশখালী – কুতুবদিয়া • বঙ্গোপসাগর উপকূল" else "Moheshkhali – Kutubdia • Coastal Bay of Bengal",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherAndTransportCards(
    weatherData: WeatherData,
    language: AppLanguage,
    onWeatherClick: () -> Unit,
    onSeaTruckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Weather (আবহাওয়া)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFE8F4FD), // Soft pastel blue
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .testTag("card_weather")
                .clickable(onClick = onWeatherClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weather Icon inside circular badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1976D2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Weather Icon",
                        tint = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "আবহাওয়া" else "Weather",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "সর্বশেষ তথ্য দেখুন" else "View Updates",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = RoyalBlue800,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Card 2: Sea-Truck Updates (সী-ট্রাক চলাচল আপডেট)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFE6F7F2), // Soft pastel mint/cyan
            shadowElevation = 1.dp,
            modifier = Modifier
                .weight(1f)
                .height(86.dp)
                .testTag("card_seatruck")
                .clickable(onClick = onSeaTruckClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sea-truck / ship icon inside circular badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00796B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBoat,
                        contentDescription = "Sea-Truck Icon",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "সি-ট্রাক চলাচল" else "Sea-Truck Schedule",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004D40),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "সময়সূচি ও তথ্য দেখুন" else "View Schedule",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Details",
                    tint = Color(0xFF00796B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PublicServicesSection(
    services: List<PublicServiceItem>,
    language: AppLanguage,
    onServiceClick: (String) -> Unit,
    onTourismClick: () -> Unit,
    modifier: Modifier = Modifier,
    homepageCategories: List<HomepageCategoryItem> = DefaultPortalContent.defaultHomepageCategories
) {
    val primaryGreen = Color(0xFF006A4E)
    val gridCategories = remember(homepageCategories) {
        homepageCategories
            .filter { it.isPublished && !it.placement.equals("FEATURED_BANNER", ignoreCase = true) }
            .sortedBy { it.order }
    }
    val featuredBanners = remember(homepageCategories) {
        homepageCategories
            .filter { it.isPublished && it.placement.equals("FEATURED_BANNER", ignoreCase = true) }
            .sortedBy { it.order }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // --- Special Introduction / Slogan Bar ---
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            shadowElevation = 3.dp,
            border = BorderStroke(1.5.dp, Color(0xFF006A4E).copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("special_introduction_banner")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF006A4E), Color(0xFF004D38))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFFFEF08A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "উন্নয়ন ও জনসেবা পোর্টাল" else "Development & Public Service Portal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (language == AppLanguage.BN) "মহেশখালী-কুতুবদিয়ার তথ্য, সেবা ও সম্ভাবনার ডিজিটাল ঠিকানা" else "Digital Hub for Information, Services & Possibilities of Moheshkhali-Kutubdia",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feature Highlights Grid (4 Key Value Badges)
                    val featureBadges = if (language == AppLanguage.BN) {
                        listOf(
                            "ℹ️ সকল তথ্য এক জায়গায়",
                            "❤️ জনগণের পাশে",
                            "📍 দ্বীপাঞ্চলের সেবা",
                            "📈 উন্নয়নের তথ্যভাণ্ডার"
                        )
                    } else {
                        listOf(
                            "ℹ️ All Info in One",
                            "❤️ Beside Citizens",
                            "📍 Island Services",
                            "📈 Dev Analytics"
                        )
                    }

                    // 2x2 grid for 4 badges
                    val badgeRows = featureBadges.chunked(2)
                    badgeRows.forEach { rowBadges ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowBadges.forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = badge,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Header with lines and center icon & title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFF006A4E).copy(alpha = 0.3f),
                thickness = 1.dp
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = primaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.BN) "জনসেবা ও তথ্যকেন্দ্র" else "Public Service Hub",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryGreen
                )
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = Color(0xFF006A4E).copy(alpha = 0.3f),
                thickness = 1.dp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // --- Dynamic Essential Categories Grid ---
        val rows = gridCategories.chunked(3)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val targetId = item.actionTarget.ifBlank { item.id }
                    val title = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
                    val icon = PortalIconMapper.resolveIcon(item.iconKey, Icons.Default.Category)
                    val tint = Color(item.colorHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.02f)
                            .testTag("service_card_${item.id}")
                            .clickable { onServiceClick(targetId) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = tint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
                // Fill space if row has fewer than 3 items
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (featuredBanners.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            featuredBanners.forEach { banner ->
                val bannerTarget = banner.actionTarget.ifBlank { banner.id }
                val bannerTint = Color(banner.colorHex)
                val bannerIcon = PortalIconMapper.resolveIcon(banner.iconKey, Icons.Default.TravelExplore)
                val bannerTitle = if (language == AppLanguage.BN) banner.titleBn else banner.titleEn.ifBlank { banner.titleBn }
                val bannerSubtitle = if (language == AppLanguage.BN) banner.subtitleBn else banner.subtitleEn.ifBlank { banner.subtitleBn }
                val bannerBadge = if (language == AppLanguage.BN) banner.badgeBn else banner.badgeEn.ifBlank { banner.badgeBn }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF8E1),
                    shadowElevation = 1.5.dp,
                    border = BorderStroke(1.dp, Color(0xFFFFE082)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("service_card_${banner.id}")
                        .clickable {
                            if (bannerTarget == "tourism") onTourismClick() else onServiceClick(bannerTarget)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(bannerTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = bannerIcon,
                                contentDescription = bannerTitle,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bannerTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                if (bannerBadge.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFFB300)
                                    ) {
                                        Text(
                                            text = bannerBadge,
                                            color = Color(0xFF3E2723),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (bannerSubtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bannerSubtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open $bannerTitle",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecadeHighlightBanner(
    language: AppLanguage,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFEFF6FF), // Soft pastel sky blue
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("decade_highlight_banner")
            .clickable(onClick = onDetailClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail image with rounded corners
            Box(
                modifier = Modifier
                    .size(width = 115.dp, height = 95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.LightGray)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.moheshkhali_banner),
                    contentDescription = "Decade of Development",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "উন্নয়নের এক দশক" else "A Decade of Development",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalBlue900
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Pill badge for 2014 - 2024
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00695C)
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "২০১৪ – ২০২৪" else "2014 – 2024",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (language == AppLanguage.BN) "মহেশখালী–কুতুবদিয়ার উন্নয়ন কার্যক্রমের তথ্যচিত্র" else "Development Milestones in Moheshkhali & Kutubdia",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 14.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("decade_detail_button")
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "বিস্তারিত দেখুন" else "View Details",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue800
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = RoyalBlue800,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoticeBoardWidget(
    language: AppLanguage,
    onNoticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("notice_board_widget")
            .clickable(onClick = onNoticeClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Megaphone icon in circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Notices",
                    tint = RoyalBlue800,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN) "সর্বশেষ সংবাদ ও বিজ্ঞপ্তি" else "Latest News & Notices",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (language == AppLanguage.BN) "সাম্প্রতিক খবর ও গুরুত্বপূর্ণ ঘোষণা" else "Recent updates & official notices",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open notices",
                tint = RoyalBlue800,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun MarqueeNoticeBar(
    appConfig: AppConfig,
    language: AppLanguage,
    isAdminAuthenticated: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noticeText = if (language == AppLanguage.BN) appConfig.noticeMarqueeBn else appConfig.noticeMarqueeEn

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB), // Soft yellow/amber warm pastel
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("marquee_notice_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFD97706).copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Notice Ticker",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN) "জরুরি বিজ্ঞপ্তি / নোটিশ বোর্ড" else "Official Notice Ticker",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = noticeText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF78350F),
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE
                    )
                )
            }

            if (isAdminAuthenticated) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp).testTag("edit_notice_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Notice",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminNoticeEditDialog(
    appConfig: AppConfig,
    language: AppLanguage,
    onSave: (AppConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var noticeBn by remember { mutableStateOf(appConfig.noticeMarqueeBn) }
    var noticeEn by remember { mutableStateOf(appConfig.noticeMarqueeEn) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight().testTag("admin_notice_edit_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "নোটিশ বোর্ড / মারকিউ সম্পাদনা" else "Edit Notice Board Ticker",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                OutlinedTextField(
                    value = noticeBn,
                    onValueChange = { noticeBn = it },
                    label = { Text(if (language == AppLanguage.BN) "নোটিশ ঘোষণা (বাংলা)" else "Notice Text (Bengali)") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = noticeEn,
                    onValueChange = { noticeEn = it },
                    label = { Text(if (language == AppLanguage.BN) "নোটিশ ঘোষণা (ইংরেজি)" else "Notice Text (English)") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    maxLines = 4
                )

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
                            val updated = appConfig.copy(
                                noticeMarqueeBn = noticeBn,
                                noticeMarqueeEn = noticeEn
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900),
                        modifier = Modifier.weight(1f).testTag("save_notice_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save Changes")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenServicesSection(
    services: List<DigitalServiceItem>,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (DigitalServiceItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        // Master Card on Home Screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true }
                .testTag("citizen_services_master_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle top gradient accent bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0284C7), Color(0xFF10B981), Color(0xFFF59E0B))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 18.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading Icon Badge with multi-color styling
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Title & Subtitle
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.BN) "নাগরিক সেবা ও সহায়িকা" else "Citizen E-Services & Helpdesk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (language == AppLanguage.BN) 
                                "এনআইডি, জন্ম নিবন্ধন, সনদ ও ভাতার সরকারি অনলাইন পোর্টাল" 
                            else 
                                "NID, Birth Reg, Certificates & Govt Allowances",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "সকল সেবা দেখুন ও আবেদন করুন" else "View all services & apply",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0284C7)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Forward Arrow Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Citizen Services",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet displaying all individual citizen services
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Bottom Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == AppLanguage.BN) "নাগরিক সেবা ও সহায়িকা" else "Citizen E-Services & Helpdesk",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (language == AppLanguage.BN) 
                                "সরাসরি সরকারি অফিশিয়াল পোর্টালে আবেদন করুন" 
                            else 
                                "Apply directly at official government portals",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (isAdmin) {
                        IconButton(
                            onClick = onAddClick,
                            modifier = Modifier
                                .background(Color(0xFFE2E8F0), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Service", tint = ForestGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2x2 Grid of Individual Services
                val rows = services.chunked(2)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEachIndexed { index, item ->
                            ServiceCard(
                                item = item,
                                index = index,
                                language = language,
                                isAdmin = isAdmin,
                                onEditClick = onEditClick,
                                onDeleteClick = onDeleteClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// Deprecated alias to maintain backward compatibility if called elsewhere
@Composable
fun DigitalServicesSection(
    services: List<DigitalServiceItem>,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (DigitalServiceItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    CitizenServicesSection(
        services = services,
        language = language,
        isAdmin = isAdmin,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier
    )
}

@Composable
fun ServiceCard(
    item: DigitalServiceItem, 
    index: Int,
    language: AppLanguage, 
    isAdmin: Boolean, 
    onEditClick: (DigitalServiceItem) -> Unit, 
    onDeleteClick: (String) -> Unit, 
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Resolve destination URL with reliable fallback
    val targetUrl = remember(item) {
        when {
            item.linkUrl.isNotBlank() -> item.linkUrl
            item.id == "1" || item.titleBn.contains("এনআইডি") || item.titleEn.contains("NID") -> "https://services.nidw.gov.bd"
            item.id == "2" || item.titleBn.contains("জন্ম") || item.titleEn.contains("Birth") -> "https://bdris.gov.bd"
            item.id == "3" || item.titleBn.contains("সার্টিফিকেট") || item.titleBn.contains("প্রত্যয়ন") -> "https://prottoyon.gov.bd"
            item.id == "4" || item.titleBn.contains("ভাতা") || item.titleEn.contains("Allowance") -> "https://mis.bhata.gov.bd/onlineApplication"
            else -> "https://bangladesh.gov.bd"
        }
    }
    
    val openServiceUrl = {
        com.example.util.IntentUtils.safeOpenUrl(context, targetUrl, language)
    }
    
    // Soft color palette for services
    val bgColors = listOf(Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF3E0), Color(0xFFF3E5F5))
    val iconColors = listOf(Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00), Color(0xFF7B1FA2))
    
    val bgColor = bgColors[index % bgColors.size]
    val iconColor = iconColors[index % iconColors.size]

    val serviceIcon = remember(item) {
        when {
            item.titleBn.contains("এনআইডি") || item.titleEn.contains("NID") -> Icons.Default.Badge
            item.titleBn.contains("জন্ম") || item.titleEn.contains("Birth") -> Icons.Default.ChildCare
            item.titleBn.contains("সার্টিফিকেট") || item.titleBn.contains("সনদ") -> Icons.Default.Description
            item.titleBn.contains("ভাতা") || item.titleEn.contains("Allowance") -> Icons.Default.VolunteerActivism
            else -> Icons.Default.Public
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { openServiceUrl() }
            .testTag("service_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(serviceIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.subTextBn.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) item.subTextBn else item.subTextEn,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openServiceUrl() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "আবেদন করুন" else "Apply Now",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = iconColor
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            
            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onEditClick(item) }, modifier = Modifier.size(32.dp)) { 
                        Icon(Icons.Default.Edit, contentDescription = "Edit Service", modifier = Modifier.size(16.dp), tint = ForestGreen) 
                    }
                    IconButton(onClick = { onDeleteClick(item.id) }, modifier = Modifier.size(32.dp)) { 
                        Icon(Icons.Default.Delete, contentDescription = "Delete Service", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) 
                    }
                }
            }
        }
    }
}

@Composable
fun DailyScheduleSection(
    schedules: List<DailyScheduleItem>,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (DailyScheduleItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryGreen = Color(0xFF006A4E)
    val deepNavy = Color(0xFF0F172A)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("daily_schedule_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = primaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = if (language == AppLanguage.BN) "দৈনন্দিন কার্যক্রম ও হালনাগাদ" else "Daily Activities & Updates",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = deepNavy
                    )
                    Text(
                        text = if (language == AppLanguage.BN) "উন্নয়ন পরিদর্শন, জনসভা ও গণসংযোগ এবং জনসেবা সম্পর্কিত হালনাগাদ" else "Development inspections, public meetings & service updates",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            if (isAdmin) {
                FilledTonalButton(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = primaryGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_schedule_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Schedule",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "নতুন যোগ" else "Add New",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (schedules.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "বর্তমানে কোনো কর্মসূচি নির্ধারিত নেই" else "No upcoming schedules listed",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            schedules.forEach { schedule ->
                ScheduleItemCard(
                    item = schedule,
                    language = language,
                    isAdmin = isAdmin,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ScheduleItemCard(
    item: DailyScheduleItem,
    language: AppLanguage,
    isAdmin: Boolean,
    onEditClick: (DailyScheduleItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val category = if (language == AppLanguage.BN) item.categoryBn.ifEmpty { "উন্নয়ন পরিদর্শন" } else item.categoryEn.ifEmpty { "Development Inspection" }
    val (catBgColor, catTextColor) = when {
        category.contains("উন্নয়ন") || category.contains("Development") -> Pair(Color(0xFFE8F5E9), Color(0xFF1B5E20))
        category.contains("সভা") || category.contains("Meeting") || category.contains("গণসংযোগ") -> Pair(Color(0xFFE0F2FE), Color(0xFF0369A1))
        category.contains("অনুষ্ঠান") || category.contains("Event") -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF334155))
    }

    val statusText = if (language == AppLanguage.BN) item.statusBn.ifEmpty { "নির্ধারিত" } else item.statusEn.ifEmpty { "Scheduled" }
    val (statusBgColor, statusTextColor) = when {
        statusText.contains("চলমান") || statusText.contains("Progress") -> Pair(Color(0xFFFFF7ED), Color(0xFFC2410C))
        statusText.contains("সম্পন্ন") || statusText.contains("Completed") -> Pair(Color(0xFFECFDF5), Color(0xFF047857))
        else -> Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("schedule_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Category Badge & Status Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = catBgColor
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = catTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBgColor
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusTextColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Event Title
            Text(
                text = if (language == AppLanguage.BN) item.eventTitleBn.ifEmpty { item.eventTitleEn } else item.eventTitleEn.ifEmpty { item.eventTitleBn },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF0F172A),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tags row (Date, Time, Location)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Color(0xFF006A4E),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            val d = if (language == AppLanguage.BN) item.eventDateBn else item.eventDateEn
                            val t = if (language == AppLanguage.BN) item.timeBn else item.timeEn
                            append(d)
                            if (t.isNotBlank()) append(" • ").append(t)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val location = if (language == AppLanguage.BN) item.locationBn else item.locationEn
                if (location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Admin Actions Row (সংশোধন ও বিয়োজন)
            if (isAdmin) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { onEditClick(item) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFF006A4E).copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp).testTag("edit_schedule_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF006A4E),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "সংশোধন" else "Edit",
                            fontSize = 12.sp,
                            color = Color(0xFF006A4E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.height(34.dp).testTag("delete_schedule_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "বিয়োজন" else "Delete",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = if (language == AppLanguage.BN) "কর্মসূচি মুছে ফেলতে চান?" else "Delete Schedule?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.BN)
                        "আপনি কি নিশ্চিত যে '${if (language == AppLanguage.BN) item.eventTitleBn else item.eventTitleEn}' কর্মসূচিটি মুছে ফেলতে চান? এটি ক্লাউড ডাটাবেজ থেকেও মুছে যাবে।"
                    else
                        "Are you sure you want to delete this schedule? This will permanently remove it from Firestore."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick(item.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = if (language == AppLanguage.BN) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// --- Special Days & Events Ticker Widget ("দিনপঞ্জি ও বিশেষ দিবস") ---

data class EventThemeColors(
    val gradientColors: List<Color>,
    val badgeBg: Color,
    val badgeText: Color,
    val cardBorder: Color,
    val headerAccent: Color
)

fun getEventThemeColors(themeType: String): EventThemeColors {
    return when (themeType.uppercase()) {
        "MOURNING" -> EventThemeColors(
            gradientColors = listOf(Color(0xFF1F2937), Color(0xFF111827)),
            badgeBg = Color(0xFFF3F4F6),
            badgeText = Color(0xFF111827),
            cardBorder = Color(0xFF4B5563),
            headerAccent = Color(0xFF374151)
        )
        "FESTIVE" -> EventThemeColors(
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFC2410C)),
            badgeBg = Color(0xFFFEF08A),
            badgeText = Color(0xFF854D0E),
            cardBorder = Color(0xFFFDE68A),
            headerAccent = Color(0xFFD97706)
        )
        "WORKERS" -> EventThemeColors(
            gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF0284C7)),
            badgeBg = Color(0xFFE0F2FE),
            badgeText = Color(0xFF1E3A8A),
            cardBorder = Color(0xFFBFDBFE),
            headerAccent = Color(0xFF1E40AF)
        )
        "LOCAL_DEV" -> EventThemeColors(
            gradientColors = listOf(Color(0xFF006A4E), Color(0xFF047857)),
            badgeBg = Color(0xFFD1FAE5),
            badgeText = Color(0xFF065F46),
            cardBorder = Color(0xFFA7F3D0),
            headerAccent = Color(0xFF006A4E)
        )
        else -> EventThemeColors( // PATRIOTIC (Default - Red & Green)
            gradientColors = listOf(Color(0xFF006A4E), Color(0xFFB91C1C)),
            badgeBg = Color(0xFFFEF08A),
            badgeText = Color(0xFF854D0E),
            cardBorder = Color(0xFFFECACA),
            headerAccent = Color(0xFFB91C1C)
        )
    }
}

fun getDaysDiff(eventDateIso: String): Int {
    if (eventDateIso.isBlank()) return Int.MAX_VALUE
    try {
        val parts = eventDateIso.trim().split("-")
        if (parts.size == 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            val targetCal = java.util.Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
            return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        }
    } catch (_: Exception) {}
    return Int.MAX_VALUE
}

fun calculateCountdownStatus(targetIso: String, language: AppLanguage): String {
    val days = getDaysDiff(targetIso)
    if (days == Int.MAX_VALUE) return ""
    return if (language == AppLanguage.BN) {
        when {
            days == 0 -> "🎉 আজকে অনুষ্ঠিত হচ্ছে!"
            days == 1 -> "⏳ আর বাকি: ১ দিন"
            days > 1 -> "⏳ আর বাকি: ${toBanglaDigits(days)} দিন"
            days == -1 -> "গতকাল অতিক্রান্ত"
            days < -1 -> "অতিক্রান্ত (${toBanglaDigits(-days)} দিন পূর্বে)"
            else -> ""
        }
    } else {
        when {
            days == 0 -> "🎉 Happening Today!"
            days == 1 -> "⏳ 1 day left"
            days > 1 -> "⏳ $days days left"
            days < 0 -> "Completed (${-days} days ago)"
            else -> ""
        }
    }
}

private fun toBanglaDigits(number: Int): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val str = number.toString()
    val sb = StringBuilder()
    for (ch in str) {
        if (ch.isDigit()) {
            sb.append(bnDigits[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

@Composable
fun SpecialEventsSection(
    events: List<SpecialEventItem>,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (SpecialEventItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryGreen = Color(0xFF006A4E)
    var selectedCategoryTab by remember { mutableStateOf("সকল") }

    // Automatic Date Sorting: Upcoming closest events first, followed by past
    val sortedEvents = remember(events) {
        events.sortedWith(Comparator { a, b ->
            val diffA = getDaysDiff(a.eventDateIso)
            val diffB = getDaysDiff(b.eventDateIso)
            val isAUpcoming = diffA >= 0
            val isBUpcoming = diffB >= 0
            when {
                isAUpcoming && isBUpcoming -> diffA.compareTo(diffB)
                isAUpcoming && !isBUpcoming -> -1
                !isAUpcoming && isBUpcoming -> 1
                else -> diffB.compareTo(diffA)
            }
        })
    }

    val filteredEvents = remember(sortedEvents, selectedCategoryTab) {
        if (selectedCategoryTab == "সকল") sortedEvents
        else sortedEvents.filter { it.categoryBn == selectedCategoryTab }
    }

    // Dynamic Featured Hero Card: Nearest upcoming event or explicit featured tag
    val featuredEvent = remember(sortedEvents) {
        sortedEvents.firstOrNull { it.isFeaturedCountdown && getDaysDiff(it.eventDateIso) >= 0 }
            ?: sortedEvents.firstOrNull { getDaysDiff(it.eventDateIso) >= 0 }
            ?: sortedEvents.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .testTag("special_events_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = primaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (language == AppLanguage.BN) "দিনপঞ্জি ও বিশেষ দিবস" else "Special Days & Events",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (language == AppLanguage.BN) "জাতীয় দিবস ও মহেশখালী-কুতুবদিয়ার উন্নয়ন সময়সূচী" else "National Days & Maheshkhali-Kutubdia Milestones",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            if (isAdmin) {
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(if (language == AppLanguage.BN) "যুক্ত করুন" else "Add", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Featured Countdown Hero Card with Day-Specific Dynamic Theme
        if (featuredEvent != null) {
            val countdownText = calculateCountdownStatus(featuredEvent.eventDateIso, language)
            val themeColors = getEventThemeColors(featuredEvent.themeType)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth().testTag("featured_countdown_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = themeColors.gradientColors
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) "📌 আগামী বিশেষ দিবস" else "📌 UPCOMING EVENT",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            if (countdownText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = themeColors.badgeBg
                                ) {
                                    Text(
                                        text = countdownText,
                                        color = themeColors.badgeText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (language == AppLanguage.BN) featuredEvent.titleBn else featuredEvent.titleEn,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) featuredEvent.dateDisplayBn else featuredEvent.dateDisplayEn,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (featuredEvent.locationOrTagBn.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = featuredEvent.locationOrTagBn,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (featuredEvent.descriptionBn.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.BN) featuredEvent.descriptionBn else featuredEvent.descriptionEn,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onEditClick(featuredEvent) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.3f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (language == AppLanguage.BN) "সম্পাদন" else "Edit", fontSize = 11.sp, color = Color.White)
                                }
                                Button(
                                    onClick = { onDeleteClick(featuredEvent.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.85f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Tabs / Filter Chips: [সকল দিবস] [জাতীয় দিবস] [স্থানীয় উন্নয়ন দিবস]
        val categories = listOf("সকল", "জাতীয় দিবস", "স্থানীয় উন্নয়ন দিবস")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategoryTab == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryTab = cat },
                    label = {
                        Text(
                            text = if (cat == "সকল") (if (language == AppLanguage.BN) "সকল দিবস" else "All Events") else cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryGreen,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, if (isSelected) primaryGreen else Color(0xFFCBD5E1))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Dynamic Grid Cards of National & Local Events with Theme Accents
        if (filteredEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো বিশেষ দিবস পাওয়া যায়নি।" else "No events found.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredEvents, key = { it.id }) { item ->
                    val statusText = calculateCountdownStatus(item.eventDateIso, language)
                    val cardTheme = getEventThemeColors(item.themeType)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .width(265.dp)
                            .testTag("special_event_card_${item.id}")
                    ) {
                        Column {
                            // Header Accent Bar per Theme
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(cardTheme.headerAccent)
                            )

                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (item.categoryBn.contains("জাতীয়")) Color(0xFFE8F5E9) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = item.categoryBn,
                                            color = if (item.categoryBn.contains("জাতীয়")) primaryGreen else Color(0xFF0369A1),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (item.locationOrTagBn.isNotBlank()) {
                                        Text(
                                            text = item.locationOrTagBn,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 17.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, tint = cardTheme.headerAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) item.dateDisplayBn else item.dateDisplayEn,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = cardTheme.headerAccent
                                    )
                                }

                                if (statusText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = cardTheme.badgeBg
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cardTheme.badgeText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (isAdmin) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        IconButton(onClick = { onEditClick(item) }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = primaryGreen)
                                        }
                                        IconButton(onClick = { onDeleteClick(item.id) }, modifier = Modifier.size(26.dp)) {
                                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
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

// Dialog for Admin to add/edit Special Events
@Composable
fun SpecialEventFormDialog(
    item: SpecialEventItem,
    isNew: Boolean,
    language: AppLanguage,
    onSave: (SpecialEventItem) -> Unit,
    onDismiss: () -> Unit
) {
    var titleBn by remember { mutableStateOf(item.titleBn) }
    var titleEn by remember { mutableStateOf(item.titleEn) }
    var eventDateIso by remember { mutableStateOf(item.eventDateIso.ifBlank { "2026-12-16" }) }
    var dateDisplayBn by remember { mutableStateOf(item.dateDisplayBn) }
    var dateDisplayEn by remember { mutableStateOf(item.dateDisplayEn) }
    var categoryBn by remember { mutableStateOf(item.categoryBn.ifBlank { "জাতীয় দিবস" }) }
    var locationOrTagBn by remember { mutableStateOf(item.locationOrTagBn.ifBlank { "জাতীয়" }) }
    var themeType by remember { mutableStateOf(item.themeType.ifBlank { "PATRIOTIC" }) }
    var descriptionBn by remember { mutableStateOf(item.descriptionBn) }
    var isFeaturedCountdown by remember { mutableStateOf(item.isFeaturedCountdown) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew) "➕ নতুন বিশেষ দিবস যুক্ত করুন" else "✏️ বিশেষ দিবস সম্পাদন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF006A4E)
                )

                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("দিবসের নাম (বাংলা)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = eventDateIso,
                    onValueChange = { eventDateIso = it },
                    label = { Text("কাউন্টডাউন তারিখ (YYYY-MM-DD, যেমন: 2026-12-16)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateDisplayBn,
                    onValueChange = { dateDisplayBn = it },
                    label = { Text("তারিখ প্রদর্শন (যেমন: ১৬ ডিসেম্বর, ২০২৬)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("ক্যাটেগরি:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = categoryBn == "জাতীয় দিবস",
                        onClick = { categoryBn = "জাতীয় দিবস" },
                        label = { Text("জাতীয় দিবস", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = categoryBn == "স্থানীয় উন্নয়ন দিবস",
                        onClick = { categoryBn = "স্থানীয় উন্নয়ন দিবস" },
                        label = { Text("স্থানীয় উন্নয়ন দিবস", fontSize = 11.sp) }
                    )
                }

                Text("দিবস থিম ও কালার স্কিম:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val themes = listOf(
                        "PATRIOTIC" to "🔴🟢 বিজয়/স্বাধীনতা",
                        "MOURNING" to "🖤 শোক/একুশে",
                        "FESTIVE" to "🎆 বৈশাখ/শিশু দিবস",
                        "WORKERS" to "👷 মে দিবস/সশস্ত্র",
                        "LOCAL_DEV" to "🌊 উন্নয়ন মাইলফলক"
                    )
                    items(themes) { (type, labelText) ->
                        FilterChip(
                            selected = themeType == type,
                            onClick = { themeType = type },
                            label = { Text(labelText, fontSize = 10.5.sp) }
                        )
                    }
                }

                Text("স্থান / ট্যাগ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("জাতীয়", "মহেশখালী", "কুতুবদিয়া", "মহেশখালী-কুতুবদিয়া").forEach { tag ->
                        FilterChip(
                            selected = locationOrTagBn == tag,
                            onClick = { locationOrTagBn = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = descriptionBn,
                    onValueChange = { descriptionBn = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isFeaturedCountdown,
                        onCheckedChange = { isFeaturedCountdown = it }
                    )
                    Text("প্রধান কাউন্টডাউন হিসেবে দেখান", fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("বাতিল") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                item.copy(
                                    titleBn = titleBn.ifBlank { "বিশেষ দিবস" },
                                    titleEn = titleEn.ifBlank { titleBn },
                                    eventDateIso = eventDateIso,
                                    dateDisplayBn = dateDisplayBn.ifBlank { eventDateIso },
                                    categoryBn = categoryBn,
                                    locationOrTagBn = locationOrTagBn,
                                    themeType = themeType,
                                    descriptionBn = descriptionBn,
                                    isFeaturedCountdown = isFeaturedCountdown
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                }
            }
        }
    }
}

