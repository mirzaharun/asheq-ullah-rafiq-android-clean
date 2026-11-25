package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppConfig
import com.example.data.model.AppLanguage
import com.example.data.model.ProfileInfo
import com.example.ui.components.AsyncOrLocalImage
import com.example.ui.components.SharedTextFooter
import com.example.ui.theme.*

@Composable
fun ProfileTabScreen(
    profileInfo: ProfileInfo,
    appConfig: AppConfig,
    language: AppLanguage,
    isAdminAuthenticated: Boolean = false,
    onAdminClick: () -> Unit,
    onLogoutAdmin: () -> Unit = {},
    onSaveProfile: (ProfileInfo) -> Unit = {},
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    var showFullBioDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Track expansion states for 4 interactive milestone cards
    var isEarlyLifeExpanded by remember { mutableStateOf(false) }
    var isStudentPoliticsExpanded by remember { mutableStateOf(false) }
    var isParliamentDutiesExpanded by remember { mutableStateOf(false) }
    var isPublicActivitiesExpanded by remember { mutableStateOf(false) }

    val name = if (language == AppLanguage.BN) profileInfo.nameBn else profileInfo.nameEn
    val title = if (language == AppLanguage.BN) profileInfo.titleBn else profileInfo.titleEn
    val birthDate = if (language == AppLanguage.BN) profileInfo.birthDateBn else profileInfo.birthDateEn
    val birthPlace = if (language == AppLanguage.BN) profileInfo.birthPlaceBn else profileInfo.birthPlaceEn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_tab_screen"),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Coastal Profile Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .testTag("profile_banner_section")
            ) {
                // Background Coastal Banner Image
                AsyncOrLocalImage(
                    imageUrl = profileInfo.bannerUrl.ifEmpty { appConfig.heroImageUrl },
                    contentDescription = "Moheshkhali Coastal Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    RoyalBlue900.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Header titles & branding inside banner
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "উন্নয়ন ও জনসেবা পরিচিতি" else "Development & Public Service",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (language == AppLanguage.BN) "কক্সবাজার-২ (মহেশখালী–কুতুবদিয়া)" else "Cox's Bazar-2 (Moheshkhali–Kutubdia)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Profile Avatar Card with Badges
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .offset(y = (-24).dp)
                    .testTag("profile_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo with Gold Ring Border
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(3.5.dp, GoldAccent, CircleShape)
                            .testTag("profile_avatar_image")
                    ) {
                        AsyncOrLocalImage(
                            imageUrl = profileInfo.photoUrl.ifEmpty { appConfig.avatarImageUrl },
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Badges for Birthdate & Birthplace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = RoyalBlue800,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = birthDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RoyalBlue900
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF166534),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = birthPlace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Brief Intro Section ("সংক্ষিপ্ত পরিচিতি")
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .offset(y = (-14).dp)
                    .testTag("brief_intro_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = RoyalBlue900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "সংক্ষিপ্ত পরিচিতি" else "Brief Introduction",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (language == AppLanguage.BN) profileInfo.briefIntroBn else profileInfo.briefIntroEn,
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }

        // Section Title: 4 Interactive Milestone Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .offset(y = (-10).dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "জীবন ও রাজনৈতিক মাইলফলক" else "Life & Political Milestones",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "বিস্তারিত জানতে যেকোনো কার্ডে ট্যাপ করুন" else "Tap any card to view detailed background",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // 4 Interactive Grid Cards
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .offset(y = (-6).dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: প্রারম্ভিক জীবন ও শিক্ষা
                MilestoneInteractiveCard(
                    title = if (language == AppLanguage.BN) profileInfo.earlyLifeTitleBn else profileInfo.earlyLifeTitleEn,
                    content = if (language == AppLanguage.BN) profileInfo.earlyLifeContentBn else profileInfo.earlyLifeContentEn,
                    icon = Icons.Default.School,
                    badgeText = if (language == AppLanguage.BN) "শিক্ষা ও প্রারম্ভিক" else "Education",
                    iconTint = Color(0xFF0284C7),
                    isExpanded = isEarlyLifeExpanded,
                    onToggle = { isEarlyLifeExpanded = !isEarlyLifeExpanded },
                    testTag = "milestone_card_early_life"
                )

                // Card 2: ছাত্ররাজনীতি ও সাংগঠনিক দায়িত্ব
                MilestoneInteractiveCard(
                    title = if (language == AppLanguage.BN) profileInfo.studentPoliticsTitleBn else profileInfo.studentPoliticsTitleEn,
                    content = if (language == AppLanguage.BN) profileInfo.studentPoliticsContentBn else profileInfo.studentPoliticsContentEn,
                    icon = Icons.Default.Groups,
                    badgeText = if (language == AppLanguage.BN) "ছাত্র সংসদ ভিপি ও জিএস" else "Student Politics",
                    iconTint = Color(0xFF7C3AED),
                    isExpanded = isStudentPoliticsExpanded,
                    onToggle = { isStudentPoliticsExpanded = !isStudentPoliticsExpanded },
                    testTag = "milestone_card_student_politics"
                )

                // Card 3: জাতীয় সংসদে দায়িত্ব পালন
                MilestoneInteractiveCard(
                    title = if (language == AppLanguage.BN) profileInfo.parliamentDutiesTitleBn else profileInfo.parliamentDutiesTitleEn,
                    content = if (language == AppLanguage.BN) profileInfo.parliamentDutiesContentBn else profileInfo.parliamentDutiesContentEn,
                    icon = Icons.Default.AccountBalance,
                    badgeText = if (language == AppLanguage.BN) "১০ম, ১১শ ও ১২শ সংসদ" else "Parliament (3 Terms)",
                    iconTint = Color(0xFFD97706),
                    isExpanded = isParliamentDutiesExpanded,
                    onToggle = { isParliamentDutiesExpanded = !isParliamentDutiesExpanded },
                    testTag = "milestone_card_parliament"
                )

                // Card 4: জনসংযোগতা ও প্রাতিষ্ঠানিক কর্মকাণ্ড
                MilestoneInteractiveCard(
                    title = if (language == AppLanguage.BN) profileInfo.publicActivitiesTitleBn else profileInfo.publicActivitiesTitleEn,
                    content = if (language == AppLanguage.BN) profileInfo.publicActivitiesContentBn else profileInfo.publicActivitiesContentEn,
                    icon = Icons.Default.Stars,
                    badgeText = if (language == AppLanguage.BN) "মেগা প্রকল্প ও প্রাতিষ্ঠানিক" else "Mega Projects",
                    iconTint = Color(0xFF059669),
                    isExpanded = isPublicActivitiesExpanded,
                    onToggle = { isPublicActivitiesExpanded = !isPublicActivitiesExpanded },
                    testTag = "milestone_card_public_activities"
                )
            }
        }

        // 5. Personal & Professional Info Table / Grid (ব্যক্তিগত ও পেশাগত তথ্য)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("personal_info_table_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "ব্যক্তিগত ও পেশাগত তথ্য" else "Personal & Professional Info",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "অফিসিয়াল তথ্য" else "Official",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RoyalBlue800,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Table rows
                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "পূর্ণ নাম" else "Full Name",
                        value = name,
                        icon = Icons.Default.Person,
                        isEven = false
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "জন্ম তারিখ" else "Date of Birth",
                        value = if (language == AppLanguage.BN) profileInfo.birthDateBn else profileInfo.birthDateEn,
                        icon = Icons.Default.CalendarToday,
                        isEven = true
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "জন্মস্থান" else "Birthplace",
                        value = if (language == AppLanguage.BN) profileInfo.birthPlaceBn else profileInfo.birthPlaceEn,
                        icon = Icons.Default.LocationOn,
                        isEven = false
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "শিক্ষাগত যোগ্যতা" else "Education",
                        value = if (language == AppLanguage.BN) profileInfo.educationBn else profileInfo.educationEn,
                        icon = Icons.Default.School,
                        isEven = true
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "পেশা" else "Profession",
                        value = if (language == AppLanguage.BN) profileInfo.professionBn else profileInfo.professionEn,
                        icon = Icons.Default.Business,
                        isEven = false
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "রাজনৈতিক দল" else "Political Party",
                        value = if (language == AppLanguage.BN) profileInfo.politicalPartyBn else profileInfo.politicalPartyEn,
                        icon = Icons.Default.Flag,
                        isEven = true
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "নির্বাচনী এলাকা" else "Constituency",
                        value = if (language == AppLanguage.BN) profileInfo.constituencyBn else profileInfo.constituencyEn,
                        icon = Icons.Default.HowToVote,
                        isEven = false
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "জাতীয় সংসদ" else "Parliament Terms",
                        value = if (language == AppLanguage.BN) profileInfo.parliamentTermsBn else profileInfo.parliamentTermsEn,
                        icon = Icons.Default.AccountBalance,
                        isEven = true
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "ছাত্র সংসদ ইতিহাস" else "Student Union",
                        value = if (language == AppLanguage.BN) profileInfo.collegeUnionBn else profileInfo.collegeUnionEn,
                        icon = Icons.Default.Groups,
                        isEven = false
                    )

                    ProfileTableRow(
                        label = if (language == AppLanguage.BN) "বিশ্ববিদ্যালয় সিনেট" else "University Senate",
                        value = if (language == AppLanguage.BN) profileInfo.universitySenateBn else profileInfo.universitySenateEn,
                        icon = Icons.Default.Stars,
                        isEven = true
                    )
                }
            }
        }

        // 6. "সম্পূর্ণ জীবনী পড়ুন" Action Button
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showFullBioDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("read_full_bio_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "সম্পূর্ণ জীবনী পড়ুন" else "Read Full Biography",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Admin Console Integration Button (Role-based)
                if (isAdminAuthenticated) {
                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("edit_profile_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "✏️ পরিচিতি ও বায়ো এডিট করুন" else "✏️ Edit Profile & Bio",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = onAdminClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue900),
                        border = BorderStroke(1.dp, RoyalBlue800.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "⚙️ এডমিন ম্যানেজমেন্ট প্যানেল" else "⚙️ Admin Management Console",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onLogoutAdmin,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "🚪 এডমিন লগআউট (Sign Out)" else "🚪 Sign Out Admin",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onAdminClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue900),
                        border = BorderStroke(1.dp, RoyalBlue800.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_gate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "🔒 এডমিন লগইন (Firebase Auth)" else "🔒 Admin Login (Firebase Auth)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Full Biography Dialog
    if (showFullBioDialog) {
        FullBiographyDialog(
            profileInfo = profileInfo,
            language = language,
            onDismiss = { showFullBioDialog = false }
        )
    }

    if (showEditProfileDialog) {
        AdminProfileEditDialog(
            profileInfo = profileInfo,
            language = language,
            onSave = onSaveProfile,
            onDismiss = { showEditProfileDialog = false }
        )
    }
}

@Composable
private fun MilestoneInteractiveCard(
    title: String,
    content: String,
    icon: ImageVector,
    badgeText: String,
    iconTint: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) Color(0xFFF8FAFC) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.5.dp),
        border = BorderStroke(1.dp, if (isExpanded) iconTint.copy(alpha = 0.6f) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag(testTag)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconTint.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = content,
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileTableRow(
    label: String,
    value: String,
    icon: ImageVector,
    isEven: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isEven) Color(0xFFF8FAFC) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RoyalBlue800,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF475569),
            modifier = Modifier.width(115.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FullBiographyDialog(
    profileInfo: ProfileInfo,
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
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("full_biography_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Surface(
                    color = RoyalBlue900,
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "সম্পূর্ণ জীবনবৃত্তান্ত" else "Full Biography",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Profile Header Snapshot
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldAccent, CircleShape)
                            ) {
                                AsyncOrLocalImage(
                                    imageUrl = profileInfo.photoUrl,
                                    contentDescription = profileInfo.nameBn,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) profileInfo.nameBn else profileInfo.nameEn,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalBlue900
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) profileInfo.titleBn else profileInfo.titleEn,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Full text
                    item {
                        Text(
                            text = if (language == AppLanguage.BN) profileInfo.fullBiographyBn else profileInfo.fullBiographyEn,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Justify
                        )
                    }

                    // Quote Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = RoyalBlue800,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) {
                                        "\"মহেশখালী ও কুতুবদিয়া কেবল ভৌগোলিক দুটি দ্বীপ নয়; এটি আগামী দিনের বাংলাদেশের জ্বালানি ও অর্থনৈতিক প্রাণকেন্দ্র।\""
                                    } else {
                                        "\"Moheshkhali and Kutubdia are not merely two coastal islands; they are the future economic and energy lifeblood of Bangladesh.\""
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RoyalBlue900,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "— আশেক উল্লাহ রফিক" else "— Asheq Ullah Rafiq",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }
                    }
                }

                // Close Button Bottom Bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "বন্ধ করুন" else "Close",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
