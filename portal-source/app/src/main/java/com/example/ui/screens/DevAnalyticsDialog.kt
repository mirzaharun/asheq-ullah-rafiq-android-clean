package com.example.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalBlue800

data class StatMetric(
    val titleBn: String,
    val titleEn: String,
    val valueBn: String,
    val valueEn: String,
    val subtitleBn: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val color: Color
)

data class SectorProgress(
    val sectorBn: String,
    val sectorEn: String,
    val percentage: Int,
    val detailBn: String,
    val detailEn: String,
    val color: Color
)

@Composable
fun DevAnalyticsDialog(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val keyStats = remember {
        listOf(
            StatMetric(
                titleBn = "মোট সরকারি বিনিয়োগ",
                titleEn = "Total Govt Investment",
                valueBn = "৬০,০০০+ কোটি",
                valueEn = "BDT 60,000+ Cr",
                subtitleBn = "মাতারবাড়ি ও উপকূলীয় উন্নয়ন",
                subtitleEn = "Matarbari & coastal dev",
                icon = Icons.Default.MonetizationOn,
                color = Color(0xFF0284C7)
            ),
            StatMetric(
                titleBn = "শতভাগ বিদ্যুৎ কভারেজ",
                titleEn = "100% Electrification",
                valueBn = "১০০%",
                valueEn = "100%",
                subtitleBn = "কুতুবদিয়া সাবমেরিন ক্যাবল ও গ্রিড",
                subtitleEn = "Kutubdia submarine cable grid",
                icon = Icons.Default.Bolt,
                color = Color(0xFFD97706)
            ),
            StatMetric(
                titleBn = "পাকা সড়ক ও সংযোগ সেতু",
                titleEn = "Paved Roads & Bridges",
                valueBn = "৩৫০+ কিমি",
                valueEn = "350+ km",
                subtitleBn = "দ্বীপের প্রধান ও গ্রামীণ নেটওয়ার্ক",
                subtitleEn = "Island arterial & rural roads",
                icon = Icons.Default.DirectionsCar,
                color = ForestGreen
            ),
            StatMetric(
                titleBn = "সিসি ব্লক বেড়িবাঁধ",
                titleEn = "CC Block Embankments",
                valueBn = "৪৫+ কিমি",
                valueEn = "45+ km",
                subtitleBn = "জলোচ্ছ্বাস ও বন্যা সুরক্ষা",
                subtitleEn = "Tidal surge protection",
                icon = Icons.Default.Shield,
                color = Color(0xFF7C3AED)
            ),
            StatMetric(
                titleBn = "শিক্ষা ও স্বাস্থ্য প্রতিষ্ঠান",
                titleEn = "Schools & Hospitals",
                valueBn = "১২০+ ভবন",
                valueEn = "120+ buildings",
                subtitleBn = "নতুন আধুনিকায়িত অবকাঠামো",
                subtitleEn = "Modernized infrastructure",
                icon = Icons.Default.AccountBalance,
                color = Color(0xFFE11D48)
            ),
            StatMetric(
                titleBn = "সরাসরি কর্মসংস্থান সৃষ্টি",
                titleEn = "Direct Employment",
                valueBn = "৫০,০০০+",
                valueEn = "50,000+",
                subtitleBn = "মেগা প্রকল্প, লবণ ও মৎস্য খাত",
                subtitleEn = "Mega port, salt & fishing",
                icon = Icons.Default.Groups,
                color = Color(0xFF0F766E)
            )
        )
    }

    val sectors = remember {
        listOf(
            SectorProgress(
                sectorBn = "বিদ্যুৎ ও জ্বালানি খাত (মাতারবাড়ি আল্ট্রা সুপার ক্রিটিক্যাল)",
                sectorEn = "Power & Energy Sector (Matarbari Thermal Hub)",
                percentage = 95,
                detailBn = "১২০০ মেগাওয়াট বিদ্যুৎ গ্রিডে যুক্ত ও কুতুবদিয়া সাবমেরিন বিদ্যুৎ লাইন সম্পূর্ণ।",
                detailEn = "1200 MW national grid generation and submarine cable to Kutubdia operational.",
                color = Color(0xFFD97706)
            ),
            SectorProgress(
                sectorBn = "মেগা প্রকল্প ও গভীর সমুদ্রবন্দর চ্যানেল",
                sectorEn = "Mega Infrastructure & Deep Sea Port",
                percentage = 88,
                detailBn = "১৮.৫ মিটার ড্রাফটের বাণিজ্যিক জাহাজ ভেড়ার জেটি ও সংযোগ চ্যানেল সমাপ্তির পথে।",
                detailEn = "18.5m draft commercial vessel channel nearing full milestone completion.",
                color = Color(0xFF0284C7)
            ),
            SectorProgress(
                sectorBn = "সড়ক, সেতু ও যোগাযোগ আধুনিকায়ন",
                sectorEn = "Roads, Bridges & Connectivity",
                percentage = 92,
                detailBn = "মহেশখালী প্রধান সড়ক, চকরিয়া সংযোগ ও কুতুবদিয়া রিং রোড কার্পেটিং।",
                detailEn = "Moheshkhali main artery, Chakaria connector and Kutubdia ring road paved.",
                color = ForestGreen
            ),
            SectorProgress(
                sectorBn = "উপকূলীয় স্থায়ী সিসি ব্লক বেড়িবাঁধ সুরক্ষা",
                sectorEn = "Coastal Sustainable Embankment Protection",
                percentage = 85,
                detailBn = "ধলঘাটা, মাতারবাড়ি ও কুতুবদিয়া আলী আকবর ডেইল সমুদ্র প্রতিরক্ষা বাঁধ।",
                detailEn = "Dhalghata, Matarbari & Ali Akbar Deil sea-wall defense shielding.",
                color = Color(0xFF7C3AED)
            ),
            SectorProgress(
                sectorBn = "শিক্ষা ও স্বাস্থ্য খাতের সংস্কার",
                sectorEn = "Education & Healthcare Modernization",
                percentage = 90,
                detailBn = "উপজেলা ৫০ শয্যা স্বাস্থ্য কমপ্লেক্স, আইসিটি ল্যাব ও নতুন কলেজ ভবন।",
                detailEn = "50-bed Upazila health complexes, ICT laboratories and degree college upgrades.",
                color = Color(0xFFE11D48)
            )
        )
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
                .testTag("dev_analytics_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = ForestGreen,
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
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "উন্নয়ন পরিসংখ্যান ও অগ্রগতি" else "Development Analytics & Metrics",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "২০১৪-২০২৪: মহেশখালী ও কুতুবদিয়ার এক দশক" else "2014-2024: A Decade of Transformation",
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

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Overall Progress Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalBlue800,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = if (language == AppLanguage.BN) "সামগ্রিক উন্নয়ন সম্পন্নতা" else "Overall Development Progress",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (language == AppLanguage.BN) "পরিকল্পিত সকল প্রকল্প বাস্তবায়নের গড় হার" else "Average implementation rate across all sectors",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "৮৮%",
                                        color = GoldAccent,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { 0.88f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = GoldAccent,
                                    trackColor = Color.White.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }

                    // 2x3 Metric Cards Grid
                    item {
                        Text(
                            text = if (language == AppLanguage.BN) "প্রধান উন্নয়ন সূচকসমূহ (Key Indicators)" else "Key Development Indicators",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    val metricRows = keyStats.chunked(2)
                    metricRows.forEach { rowItems ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { metric ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(metric.color.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = metric.icon,
                                                        contentDescription = null,
                                                        tint = metric.color,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (language == AppLanguage.BN) metric.titleBn else metric.titleEn,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.DarkGray,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (language == AppLanguage.BN) metric.valueBn else metric.valueEn,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = metric.color
                                            )
                                            Text(
                                                text = if (language == AppLanguage.BN) metric.subtitleBn else metric.subtitleEn,
                                                fontSize = 10.sp,
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sector Progress Section
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "খাতভিত্তিক অগ্রগতি (Sectoral Progress)" else "Sectoral Implementation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    items(sectors) { sec ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BN) sec.sectorBn else sec.sectorEn,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${sec.percentage}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = sec.color
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) sec.detailBn else sec.detailEn,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { sec.percentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = sec.color,
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
