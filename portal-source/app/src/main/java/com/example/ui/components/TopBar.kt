package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppConfig
import com.example.data.model.AppLanguage
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RedDanger
import com.example.ui.theme.RoyalBlue800

@Composable
fun PortalTopBar(
    appConfig: AppConfig,
    language: AppLanguage,
    showBackButton: Boolean = false,
    isAdminAuthenticated: Boolean = false,
    unreadNoticeCount: Int = 0,
    onAdminClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onLanguageToggle: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = RoyalBlue800,
        modifier = modifier
            .fillMaxWidth()
            .testTag("portal_top_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional Back Arrow
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(38.dp).testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
            } else {
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Circular profile avatar image of Asheq Ullah Rafiq (1800.png)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                    .background(Color.White)
            ) {
                AsyncOrLocalImage(
                    imageUrl = appConfig.avatarImageUrl,
                    contentDescription = if (language == AppLanguage.BN) appConfig.profileNameBn else appConfig.profileNameEn,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // White name text "Asheq Ullah Rafiq" & subtitle "Development & Public Service"
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) appConfig.profileNameBn else appConfig.profileNameEn,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )
                Text(
                    text = if (language == AppLanguage.BN) appConfig.subTitleBn else appConfig.subTitleEn,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Language Toggle Icon (Bangla 'অ' / English 'A')
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier
                    .testTag("language_toggle_button")
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onLanguageToggle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) "অ" else "A",
                        color = GoldAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "/A" else "/অ",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // White bell notification icon with red indicator
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .testTag("notification_button")
                    .clip(CircleShape)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = if (language == AppLanguage.BN) "বিজ্ঞপ্তি" else "Notifications",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                if (unreadNoticeCount > 0) {
                    val badgeText = if (unreadNoticeCount > 9) "9+" else unreadNoticeCount.toString()
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-1).dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(RedDanger)
                            .padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.testTag("notification_unread_badge")
                        )
                    }
                }
            }
        }
    }
}
