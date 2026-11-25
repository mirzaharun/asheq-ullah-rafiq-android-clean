package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.AppLanguage
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900
import com.example.ui.theme.TextMuted

data class NavItem(
    val titleBn: String,
    val titleEn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun PortalBottomNavigationAndFooter(
    currentTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    language: AppLanguage,
    appConfig: AppConfig,
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem(
            titleBn = "হোম",
            titleEn = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_item_home"
        ),
        NavItem(
            titleBn = "উন্নয়ন",
            titleEn = "Development",
            selectedIcon = Icons.Filled.TrendingUp,
            unselectedIcon = Icons.Outlined.TrendingUp,
            testTag = "nav_item_development"
        ),
        NavItem(
            titleBn = "গ্যালারি",
            titleEn = "Gallery",
            selectedIcon = Icons.Filled.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
            testTag = "nav_item_gallery"
        ),
        NavItem(
            titleBn = "পরিচিতি",
            titleEn = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
            testTag = "nav_item_profile"
        ),
        NavItem(
            titleBn = "যোগাযোগ",
            titleEn = "Contact",
            selectedIcon = Icons.Filled.Phone,
            unselectedIcon = Icons.Outlined.Phone,
            testTag = "nav_item_contact"
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .testTag("portal_bottom_container")
    ) {
        // Fixed 5-tab Material 3 Navigation Bar
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = currentTabIndex == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = RoyalBlue800,
                        selectedTextColor = RoyalBlue800,
                        indicatorColor = Color(0xFFE3F2FD),
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }

        // Single Shared Global Footer Component (6 links + copyright) rendered at the very bottom below navigation buttons
        SharedTextFooter(
            language = language,
            appConfig = appConfig,
            onAboutClick = onAboutClick,
            onProfileClick = onProfileClick,
            onPolicyClick = onPolicyClick,
            onFeedbackClick = onFeedbackClick,
            onTermsClick = onTermsClick
        )
    }
}

@Composable
fun SharedTextFooter(
    language: AppLanguage,
    appConfig: AppConfig,
    onAboutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPolicyClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("shared_text_footer"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "অ্যাপ সম্পর্কে" else "About",
                    tag = "footer_link_about",
                    onClick = onAboutClick
                )
                SharedFooterSeparator()
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "পরিচিতি" else "Profile",
                    tag = "footer_link_profile",
                    onClick = onProfileClick
                )
                SharedFooterSeparator()
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "গোপনীয়তা" else "Privacy",
                    tag = "footer_link_privacy",
                    onClick = onPolicyClick
                )
                SharedFooterSeparator()
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "শর্তাবলী" else "Terms",
                    tag = "footer_link_terms",
                    onClick = onTermsClick
                )
                SharedFooterSeparator()
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "মতামত" else "Feedback",
                    tag = "footer_link_feedback",
                    onClick = onFeedbackClick
                )
                SharedFooterSeparator()
                SharedFooterLinkText(
                    text = if (language == AppLanguage.BN) "শেয়ার" else "Share",
                    tag = "footer_link_share",
                    onClick = {
                        val shareSubject = if (language == AppLanguage.BN) {
                            "আশেক উল্লাহ রফিক ডিজিটাল পোর্টাল"
                        } else {
                            "Asheq Ullah Rafiq Digital Portal"
                        }
                        val shareText = if (language == AppLanguage.BN) {
                            "মহেশখালী ও কুতুবদিয়ার উন্নয়ন প্রকল্প, আবহাওয়া ও সি-ট্রাক সময়সূচি এবং নাগরিক সেবার জন্য আশেক উল্লাহ রফিক ডিজিটাল পোর্টাল ব্যবহার করুন।"
                        } else {
                            "Explore Moheshkhali & Kutubdia development projects, coastal weather/sea-truck schedules, and public services on the Asheq Ullah Rafiq Digital Portal."
                        }
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    if (language == AppLanguage.BN) "অ্যাপ শেয়ার করুন" else "Share App"
                                )
                            )
                        } catch (_: Exception) {
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (language == AppLanguage.BN) appConfig.copyrightBn else appConfig.copyrightEn,
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.testTag("shared_footer_copyright")
        )
    }
}

@Composable
private fun SharedFooterLinkText(
    text: String,
    tag: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = RoyalBlue800,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    )
}

@Composable
private fun SharedFooterSeparator() {
    Text(
        text = "·",
        fontSize = 10.sp,
        color = Color(0xFF94A3B8),
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}
