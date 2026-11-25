package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.data.model.SocialMediaLink
import com.example.ui.theme.FlagGreen700
import com.example.ui.theme.RoyalBlue900

data class SocialBrandStyle(
    val brandColor: Color,
    val icon: ImageVector,
    val defaultHandleBn: String,
    val defaultHandleEn: String,
    val actionTextBn: String,
    val actionTextEn: String
)

fun getSocialBrandStyle(link: SocialMediaLink): SocialBrandStyle {
    val key = "${link.iconType} ${link.platformNameEn} ${link.url}".lowercase()
    return when {
        key.contains("facebook") || key.contains("fb") -> SocialBrandStyle(
            brandColor = Color(0xFF1877F2),
            icon = Icons.Default.ThumbUp,
            defaultHandleBn = "অফিসিয়াল ফেসবুক পেজ",
            defaultHandleEn = "Official Facebook Page",
            actionTextBn = "যুক্ত হোন",
            actionTextEn = "Follow"
        )
        key.contains("youtube") || key.contains("yt") -> SocialBrandStyle(
            brandColor = Color(0xFFFF0000),
            icon = Icons.Default.SmartDisplay,
            defaultHandleBn = "অফিসিয়াল ইউটিউব চ্যানেল",
            defaultHandleEn = "Official YouTube Channel",
            actionTextBn = "সাবস্ক্রাইব",
            actionTextEn = "Subscribe"
        )
        key.contains("whatsapp") || key.contains("wa") -> SocialBrandStyle(
            brandColor = Color(0xFF25D366),
            icon = Icons.Default.Chat,
            defaultHandleBn = "হোয়াটসঅ্যাপ হেল্পলাইন",
            defaultHandleEn = "WhatsApp Helpline",
            actionTextBn = "মেসেজ দিন",
            actionTextEn = "Message"
        )
        key.contains("twitter") || key.contains("x ") || key.endsWith("x") -> SocialBrandStyle(
            brandColor = Color(0xFF0F172A),
            icon = Icons.Default.Share,
            defaultHandleBn = "অফিসিয়াল এক্স (টুইটার)",
            defaultHandleEn = "Official X (Twitter)",
            actionTextBn = "ফলো করুন",
            actionTextEn = "Follow"
        )
        key.contains("instagram") || key.contains("insta") -> SocialBrandStyle(
            brandColor = Color(0xFFE4405F),
            icon = Icons.Default.PhotoCamera,
            defaultHandleBn = "ইনস্টাগ্রাম প্রোফাইল",
            defaultHandleEn = "Instagram Profile",
            actionTextBn = "যুক্ত হোন",
            actionTextEn = "Follow"
        )
        key.contains("telegram") -> SocialBrandStyle(
            brandColor = Color(0xFF0088CC),
            icon = Icons.Default.Send,
            defaultHandleBn = "টেলিগ্রাম চ্যানেল",
            defaultHandleEn = "Telegram Channel",
            actionTextBn = "যোগ দিন",
            actionTextEn = "Join"
        )
        key.contains("linkedin") -> SocialBrandStyle(
            brandColor = Color(0xFF0A66C2),
            icon = Icons.Default.Work,
            defaultHandleBn = "লিংকডইন নেটওয়ার্ক",
            defaultHandleEn = "LinkedIn Network",
            actionTextBn = "কানেক্ট",
            actionTextEn = "Connect"
        )
        else -> SocialBrandStyle(
            brandColor = Color(0xFF006A4E),
            icon = Icons.Default.Language,
            defaultHandleBn = "অফিসিয়াল ওয়েব পোর্টাল",
            defaultHandleEn = "Official Web Portal",
            actionTextBn = "ভিজিট করুন",
            actionTextEn = "Visit"
        )
    }
}

/**
 * Modern Interactive Social Media Box (Grid Card Layout)
 */
@Composable
fun SocialMediaBox(
    link: SocialMediaLink,
    language: AppLanguage,
    isAdmin: Boolean,
    onEdit: (SocialMediaLink) -> Unit,
    onDelete: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = getSocialBrandStyle(link)
    val displayName = if (language == AppLanguage.BN) {
        link.platformNameBn.ifBlank { link.platformNameEn }
    } else {
        link.platformNameEn.ifBlank { link.platformNameBn }
    }
    val displayHandle = if (language == AppLanguage.BN) style.defaultHandleBn else style.defaultHandleEn
    val displayAction = if (language == AppLanguage.BN) style.actionTextBn else style.actionTextEn

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("social_box_${link.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Brand Icon & Admin Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Colored Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.brandColor.copy(alpha = 0.12f))
                        .border(1.dp, style.brandColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = displayName,
                        tint = style.brandColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { onEdit(link) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onDelete(link.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Platform Title
            Text(
                text = displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle / Handle
            Text(
                text = displayHandle,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = style.brandColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayAction,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = style.brandColor
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = style.brandColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modern Social Media Row Card (List Layout)
 */
@Composable
fun SocialMediaCard(
    link: SocialMediaLink,
    isAdmin: Boolean,
    onEdit: (SocialMediaLink) -> Unit,
    onDelete: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    language: AppLanguage = AppLanguage.BN
) {
    val style = getSocialBrandStyle(link)
    val displayName = if (language == AppLanguage.BN) {
        link.platformNameBn.ifBlank { link.platformNameEn }
    } else {
        link.platformNameEn.ifBlank { link.platformNameBn }
    }
    val displayHandle = if (language == AppLanguage.BN) style.defaultHandleBn else style.defaultHandleEn

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("social_card_${link.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = style.brandColor.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp),
                border = BorderStroke(1.dp, style.brandColor.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.brandColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = RoyalBlue900
                )
                Text(
                    text = displayHandle,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            if (isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = { onEdit(link) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(link.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = style.brandColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) style.actionTextBn else style.actionTextEn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = style.brandColor
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = style.brandColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Modern Interactive Social Media Box Section
 * Used on Home Screen, Contact Screen, and Admin Views.
 */
@Composable
fun SocialMediaBoxSection(
    links: List<SocialMediaLink>,
    language: AppLanguage,
    isAdmin: Boolean,
    onSaveLink: (SocialMediaLink) -> Unit,
    onDeleteLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingLink by remember { mutableStateOf<SocialMediaLink?>(null) }
    var isAddingLink by remember { mutableStateOf(false) }

    fun launchSocialUrl(url: String) {
        com.example.util.IntentUtils.safeOpenUrl(context, url, language)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("social_media_box_section")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN) "সোশ্যাল মিডিয়া ও অফিসিয়াল চ্যানেল" else "Social Media & Official Channels",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "জননেতা আশেক উল্লাহ রফিক এমপির অফিসিয়াল ডিজিটাল প্ল্যাটফর্ম" else "Official digital platforms of Asheq Ullah Rafiq MP",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (isAdmin) {
                Button(
                    onClick = { isAddingLink = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FlagGreen700),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_social_link_button")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.BN) "যুক্ত করুন" else "Add New", fontSize = 11.5.sp)
                }
            }
        }

        if (links.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "বর্তমানে কোনো সোশ্যাল মিডিয়া লিংক যুক্ত নেই" else "No social media channels added yet",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            // Interactive 2-Column Grid Box Layout
            val chunked = links.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chunked.forEach { rowLinks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowLinks.forEach { link ->
                            SocialMediaBox(
                                link = link,
                                language = language,
                                isAdmin = isAdmin,
                                onEdit = { editingLink = it },
                                onDelete = onDeleteLink,
                                onClick = { launchSocialUrl(link.url) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowLinks.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Modal Edit / Add Dialog
    if (isAddingLink || editingLink != null) {
        val initial = editingLink ?: SocialMediaLink(id = "SM_${System.currentTimeMillis()}")
        SocialMediaEditDialog(
            link = initial,
            isNew = isAddingLink,
            language = language,
            onSave = { updated ->
                onSaveLink(updated)
                isAddingLink = false
                editingLink = null
                Toast.makeText(context, if (language == AppLanguage.BN) "লিংক সফলভাবে সংরক্ষিত হয়েছে" else "Social channel saved successfully", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                isAddingLink = false
                editingLink = null
            }
        )
    }
}
