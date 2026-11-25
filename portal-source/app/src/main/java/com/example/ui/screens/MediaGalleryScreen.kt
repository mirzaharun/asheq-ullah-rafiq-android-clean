package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.AsyncOrLocalImage
import com.example.ui.components.SharedTextFooter

/**
 * Interactive Media Gallery (ছবি ও ভিডিও আর্কাইভ) & News Archive (সংবাদ ও প্রকাশনা আর্কাইভ)
 * Designed in high-end Green & White theme (#006A4E & #FFFFFF).
 * Features:
 * - Single clean search bar
 * - Clean media type filter buttons ("সব", "ছবি", "ভিডিও")
 * - Full bilingual localization (English / Bengali)
 * - Lightbox with zoom and share intent, and Admin CRUD.
 */
@Composable
fun MediaGalleryScreen(
    galleryItems: List<GalleryItem>,
    mediaArchiveItems: List<MediaArchiveItem> = emptyList(),
    importantLinks: List<ImportantExternalLink> = emptyList(),
    language: AppLanguage,
    isAdminAuthenticated: Boolean,
    onAdminLoginClick: () -> Unit = {},
    onAdminLogoutClick: () -> Unit = {},
    onSaveGalleryItem: (GalleryItem) -> Unit = {},
    onDeleteGalleryItem: (String) -> Unit = {},
    onSaveMediaArchiveItem: (MediaArchiveItem) -> Unit = {},
    onDeleteMediaArchiveItem: (String) -> Unit = {},
    onSaveImportantLink: (ImportantExternalLink) -> Unit = {},
    onDeleteImportantLink: (String) -> Unit = {},
    appConfig: AppConfig = AppConfig(),
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTopSection by remember { mutableStateOf(0) } // 0: ছবি ও ভিডিও, 1: সংবাদ আর্কাইভ, 2: গুরুত্বপূর্ণ লিংক
    
    // Filters & Search
    var searchQuery by remember { mutableStateOf("") }
    var selectedMediaCategory by remember { mutableStateOf("সকল") }
    var mediaTypeFilter by remember { mutableStateOf("সকল") } // "সকল", "PHOTO", "VIDEO"

    // Modals / Full-screen Lightbox & Video Viewers
    var zoomableImageItem by remember { mutableStateOf<GalleryItem?>(null) }
    var playableVideo by remember { mutableStateOf<Triple<String, String, String>?>(null) } // (url, title, source)

    // Admin CRUD Dialog states
    var editingGalleryItem by remember { mutableStateOf<GalleryItem?>(null) }
    var isAddingGalleryItem by remember { mutableStateOf(false) }

    var editingMediaItem by remember { mutableStateOf<MediaArchiveItem?>(null) }
    var isAddingMediaItem by remember { mutableStateOf(false) }

    var editingImportantLink by remember { mutableStateOf<ImportantExternalLink?>(null) }
    var isAddingImportantLink by remember { mutableStateOf(false) }

    val greenTheme = Color(0xFF006A4E)
    val greenDark = Color(0xFF004D38)

    // Media Categories for News Archive (Section 1)
    val mediaCategories = listOf(
        "সকল", "সংবাদ প্রতিবেদন", "ভিডিও প্রতিবেদন", "সাক্ষাৎকার",
        "বক্তব্য ও বিবৃতি", "সংবাদ সম্মেলন", "পত্রিকার কাটিং",
        "টকশো ও আলোচনা", "প্রেস রিলিজ"
    )

    // Filtered Gallery Items: Clean search across both languages, plus media type filter
    val filteredGalleryItems = remember(galleryItems, searchQuery, mediaTypeFilter) {
        galleryItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.titleBn.contains(searchQuery, ignoreCase = true) ||
                item.titleEn.contains(searchQuery, ignoreCase = true) ||
                item.captionBn.contains(searchQuery, ignoreCase = true) ||
                item.descriptionBn.contains(searchQuery, ignoreCase = true) ||
                item.locationBn.contains(searchQuery, ignoreCase = true) ||
                item.categoryBn.contains(searchQuery, ignoreCase = true) ||
                item.categoryEn.contains(searchQuery, ignoreCase = true)

            val matchesType = mediaTypeFilter == "সকল" || item.mediaType.equals(mediaTypeFilter, ignoreCase = true)

            matchesSearch && matchesType
        }
    }

    // Filtered Media Archive Items: Clean search + category filter
    val filteredMediaItems = remember(mediaArchiveItems, searchQuery, selectedMediaCategory) {
        mediaArchiveItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.titleBn.contains(searchQuery, ignoreCase = true) ||
                item.titleEn.contains(searchQuery, ignoreCase = true) ||
                item.summaryBn.contains(searchQuery, ignoreCase = true) ||
                item.mediaOutletBn.contains(searchQuery, ignoreCase = true) ||
                item.subjectCategory.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedMediaCategory == "সকল" || item.categoryBn == selectedMediaCategory

            matchesSearch && matchesCategory
        }
    }

    // Unified single scrollable container for smooth natural scrolling
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        // 1. Top Green Header Banner (scrolls out of view naturally)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = greenTheme,
                shadowElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(greenTheme, greenDark)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (language == AppLanguage.BN) "মিডিয়া গ্যালারী ও সংবাদ আর্কাইভ" else "Media Gallery & News Archive",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "ছবি, ভিডিও এবং সংবাদ প্রতিবেদনের ডিজিটাল ভাণ্ডার" else "Digital archive of photos, videos and news reports",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }

        // 2. Section Navigation Tabs
        item {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        NavTab(
                            label = if (language == AppLanguage.BN) "ছবি ও ভিডিও" else "Photos & Videos",
                            isSelected = selectedTopSection == 0,
                            icon = Icons.Default.PhotoLibrary
                        ) { selectedTopSection = 0 }
                    }
                    item {
                        NavTab(
                            label = if (language == AppLanguage.BN) "সংবাদ আর্কাইভ" else "News Archive",
                            isSelected = selectedTopSection == 1,
                            icon = Icons.Default.Newspaper
                        ) { selectedTopSection = 1 }
                    }
                    item {
                        NavTab(
                            label = if (language == AppLanguage.BN) "গুরুত্বপূর্ণ লিংক" else "Important Links",
                            isSelected = selectedTopSection == 2,
                            icon = Icons.Default.Language
                        ) { selectedTopSection = 2 }
                    }
                }
            }
        }

        // 3. Clean Search Bar (Only for Section 0 & 1, with horizontal area filter bar removed)
        if (selectedTopSection == 0 || selectedTopSection == 1) {
            item {
                CleanSearchBar(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    language = language
                )
            }
        }

        // 4. Content Area Items
        when (selectedTopSection) {
            0 -> galleryArchiveSectionItems(
                items = filteredGalleryItems,
                mediaTypeFilter = mediaTypeFilter,
                onMediaTypeChange = { mediaTypeFilter = it },
                language = language,
                isAdmin = isAdminAuthenticated,
                onAddClick = { isAddingGalleryItem = true },
                onCardClick = { item ->
                    if (item.mediaType.equals("VIDEO", ignoreCase = true)) {
                        playableVideo = Triple(item.videoEmbedUrl, if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }, item.verifiedSource)
                    } else {
                        zoomableImageItem = item
                    }
                },
                onEditClick = { editingGalleryItem = it },
                onDeleteClick = onDeleteGalleryItem,
                context = context
            )
            1 -> mediaArchiveSectionItems(
                items = filteredMediaItems,
                categories = mediaCategories,
                selectedCategory = selectedMediaCategory,
                onCategoryChange = { selectedMediaCategory = it },
                language = language,
                isAdmin = isAdminAuthenticated,
                onAddClick = { isAddingMediaItem = true },
                onEditClick = { editingMediaItem = it },
                onDeleteClick = onDeleteMediaArchiveItem,
                context = context
            )
            2 -> importantLinkSectionItems(
                links = importantLinks,
                language = language,
                isAdmin = isAdminAuthenticated,
                onAddClick = { isAddingImportantLink = true },
                onEditClick = { editingImportantLink = it },
                onDeleteClick = onDeleteImportantLink,
                context = context
            )
        }
    }

    // --- Modals ---
    if (zoomableImageItem != null) {
        LightboxViewer(item = zoomableImageItem!!, language = language, onDismiss = { zoomableImageItem = null })
    }

    if (playableVideo != null) {
        VideoPlayerModal(
            videoUrl = playableVideo!!.first,
            title = playableVideo!!.second,
            source = playableVideo!!.third,
            language = language,
            onDismiss = { playableVideo = null }
        )
    }

    // --- Admin Dialogs ---
    if (isAddingGalleryItem || editingGalleryItem != null) {
        GalleryItemFormDialog(
            item = editingGalleryItem ?: GalleryItem(id = "G_${System.currentTimeMillis()}"),
            isNew = isAddingGalleryItem,
            language = language,
            onSave = { onSaveGalleryItem(it); isAddingGalleryItem = false; editingGalleryItem = null },
            onDismiss = { isAddingGalleryItem = false; editingGalleryItem = null }
        )
    }

    if (isAddingMediaItem || editingMediaItem != null) {
        MediaArchiveItemFormDialog(
            item = editingMediaItem ?: MediaArchiveItem(id = "M_${System.currentTimeMillis()}"),
            isNew = isAddingMediaItem,
            language = language,
            onSave = { onSaveMediaArchiveItem(it); isAddingMediaItem = false; editingMediaItem = null },
            onDismiss = { isAddingMediaItem = false; editingMediaItem = null }
        )
    }

    if (isAddingImportantLink || editingImportantLink != null) {
        ImportantLinkFormDialog(
            item = editingImportantLink ?: ImportantExternalLink(id = "L_${System.currentTimeMillis()}"),
            isNew = isAddingImportantLink,
            language = language,
            onSave = { onSaveImportantLink(it); isAddingImportantLink = false; editingImportantLink = null },
            onDismiss = { isAddingImportantLink = false; editingImportantLink = null }
        )
    }
}

// ---------------- UI COMPONENTS ----------------

@Composable
fun NavTab(
    label: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val greenTheme = Color(0xFF006A4E)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) greenTheme else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isSelected) greenTheme else Color(0xFFE2E8F0)),
        modifier = Modifier
            .height(42.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
        }
    }
}

/**
 * Clean Search Bar with the area filter bar removed
 */
@Composable
fun CleanSearchBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    language: AppLanguage
) {
    val greenTheme = Color(0xFF006A4E)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(
                    text = if (language == AppLanguage.BN) "খুঁজুন (শিরোনাম বা বিবরণ)..." else "Search title or description...",
                    fontSize = 12.5.sp,
                    color = Color.Gray
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = greenTheme) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = greenTheme,
                unfocusedBorderColor = Color(0xFFCBD5E1)
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true
        )
    }
}

private fun LazyListScope.galleryArchiveSectionItems(
    items: List<GalleryItem>,
    mediaTypeFilter: String,
    onMediaTypeChange: (String) -> Unit,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onCardClick: (GalleryItem) -> Unit,
    onEditClick: (GalleryItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    context: Context
) {
    if (isAdmin) {
        item {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                AdminHeader(
                    title = if (language == AppLanguage.BN) "গ্যালারি এডমিন" else "Gallery Admin",
                    subtitle = if (language == AppLanguage.BN) "ছবি/ভিডিও আপলোড ও ম্যানেজ করুন" else "Upload and manage photos & videos",
                    onAddClick = onAddClick,
                    language = language
                )
            }
        }
    }

    // Media type filter buttons ("সব", "ছবি", "ভিডিও") - clean, with secondary category filter removed
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("সকল", "PHOTO", "VIDEO").forEach { type ->
                val isSel = mediaTypeFilter == type
                FilterChip(
                    selected = isSel,
                    onClick = { onMediaTypeChange(type) },
                    label = {
                        Text(
                            text = when (type) {
                                "PHOTO" -> if (language == AppLanguage.BN) "📸 ছবি" else "📸 Photos"
                                "VIDEO" -> if (language == AppLanguage.BN) "🎬 ভিডিও" else "🎬 Videos"
                                else -> if (language == AppLanguage.BN) "সব" else "All"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF006A4E),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }

    if (items.isEmpty()) {
        item {
            EmptyState(if (language == AppLanguage.BN) "কোনো মিডিয়া পাওয়া যায়নি" else "No media items found")
        }
    } else {
        items(items, key = { it.id }) { item ->
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                GalleryCard(
                    item = item,
                    language = language,
                    isAdmin = isAdmin,
                    onCardClick = { onCardClick(item) },
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item.id) },
                    onShareClick = { shareGalleryItem(context, item, language) }
                )
            }
        }
    }
}

private fun LazyListScope.mediaArchiveSectionItems(
    items: List<MediaArchiveItem>,
    categories: List<String>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (MediaArchiveItem) -> Unit,
    onDeleteClick: (String) -> Unit,
    context: Context
) {
    if (isAdmin) {
        item {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                AdminHeader(
                    title = if (language == AppLanguage.BN) "সংবাদ আর্কাইভ এডমিন" else "News Archive Admin",
                    subtitle = if (language == AppLanguage.BN) "প্রতিবেদন ও প্রকাশনা যুক্ত করুন" else "Add news reports and press articles",
                    onAddClick = onAddClick,
                    language = language
                )
            }
        }
    }
    
    item {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            items(categories) { cat ->
                val isSel = selectedCategory == cat
                FilterChip(
                    selected = isSel,
                    onClick = { onCategoryChange(cat) },
                    label = {
                        Text(
                            text = getMediaCategoryLabel(cat, language),
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF006A4E),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }

    if (items.isEmpty()) {
        item {
            EmptyState(if (language == AppLanguage.BN) "কোনো প্রতিবেদন পাওয়া যায়নি" else "No news reports found")
        }
    } else {
        items(items, key = { it.id }) { item ->
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                MediaCard(
                    item = item,
                    language = language,
                    isAdmin = isAdmin,
                    onOpen = { openLink(context, item.externalUrl.ifBlank { item.videoOrFileUrl }) },
                    onEdit = { onEditClick(item) },
                    onDelete = { onDeleteClick(item.id) },
                    onShare = { shareMediaItem(context, item, language) }
                )
            }
        }
    }
}

private fun LazyListScope.importantLinkSectionItems(
    links: List<ImportantExternalLink>,
    language: AppLanguage,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (ImportantExternalLink) -> Unit,
    onDeleteClick: (String) -> Unit,
    context: Context
) {
    if (isAdmin) {
        item {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                AdminHeader(
                    title = if (language == AppLanguage.BN) "লিংক ম্যানেজমেন্ট" else "Link Management",
                    subtitle = if (language == AppLanguage.BN) "গুরুত্বপূর্ণ পোর্টাল ও উইকি লিংক" else "Important portal & official links",
                    onAddClick = onAddClick,
                    language = language
                )
            }
        }
    }
    if (links.isEmpty()) {
        item {
            EmptyState(if (language == AppLanguage.BN) "কোনো লিংক নেই" else "No links found")
        }
    } else {
        items(links, key = { it.id }) { link ->
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                ImportantLinkCard(
                    item = link,
                    language = language,
                    isAdmin = isAdmin,
                    onOpenLink = { openLink(context, link.url) },
                    onEditClick = { onEditClick(link) },
                    onDeleteClick = { onDeleteClick(link.id) }
                )
            }
        }
    }
}

@Composable
fun AdminHeader(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit,
    language: AppLanguage
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFE8F5E9),
        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006A4E))
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF2E7D32))
            }
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (language == AppLanguage.BN) "➕ যুক্ত করুন" else "➕ Add New", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun GalleryCard(
    item: GalleryItem,
    language: AppLanguage,
    isAdmin: Boolean,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val displayCategory = if (language == AppLanguage.BN) {
        item.categoryBn
    } else {
        if (item.categoryEn.isNotBlank()) item.categoryEn
        else getGalleryCategoryTitle(item.categoryBn, language)
    }

    val displayTitle = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val displayLocation = if (language == AppLanguage.BN) {
        "📍 ${item.locationBn}${if (item.unionBn.isNotBlank() && item.unionBn != "সকল এলাকা") " (${item.unionBn})" else ""}"
    } else {
        val locEn = translateLocationToEn(item.locationBn)
        val unionEn = if (item.unionBn.isNotBlank() && item.unionBn != "সকল এলাকা") " (${translateLocationToEn(item.unionBn)})" else ""
        "📍 $locEn$unionEn"
    }
    val displaySource = if (language == AppLanguage.BN) {
        "সূত্র: ${item.verifiedSource}"
    } else {
        "Source: ${translateSourceToEn(item.verifiedSource)}"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncOrLocalImage(item.imageUrl.ifBlank { "drawable:moheshkhali_banner" }, null, Modifier.fillMaxSize(), ContentScale.Crop)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF006A4E),
                    modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                ) {
                    Text(
                        text = displayCategory,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (item.mediaType == "VIDEO") {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(displayTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(displayLocation, fontSize = 11.sp, color = Color(0xFF0D9488))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(displaySource, fontSize = 10.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onShareClick, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        if (isAdmin) {
                            IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, null, tint = Color(0xFF006A4E), modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    item: MediaArchiveItem,
    language: AppLanguage,
    isAdmin: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val displayCategory = getMediaCategoryLabel(item.categoryBn, language)
    val displayDate = if (language == AppLanguage.BN) item.publicationDateBn else translateDateToEn(item.publicationDateBn)
    val displayTitle = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val displaySummary = item.summaryBn
    val displayOutlet = if (language == AppLanguage.BN) item.mediaOutletBn else translateOutletToEn(item.mediaOutletBn)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(displayCategory, color = Color(0xFF006A4E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(displayDate, color = Color.Gray, fontSize = 10.sp)
            }
            Text(displayTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.padding(vertical = 4.dp))
            Text(displaySummary, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(displayOutlet, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    if (isAdmin) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFF006A4E), modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                    Button(
                        onClick = onOpen,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))
                    ) {
                        Text(if (language == AppLanguage.BN) "লিংক" else "Link", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// --- Lightbox & Utility ---

@Composable
fun LightboxViewer(item: GalleryItem, language: AppLanguage, onDismiss: () -> Unit) {
    val displayTitle = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val displayCaption = item.captionBn.ifBlank { item.descriptionBn }
    val displayLocationAndSource = if (language == AppLanguage.BN) {
        "📍 ${item.locationBn} • সূত্র: ${item.verifiedSource}"
    } else {
        "📍 ${translateLocationToEn(item.locationBn)} • Source: ${translateSourceToEn(item.verifiedSource)}"
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                val state = rememberTransformableState { zoom, offsetChange, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset += offsetChange
                }
                AsyncOrLocalImage(
                    imageUrl = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().transformable(state).graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                    contentScale = ContentScale.Fit
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(0.4f), CircleShape)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.7f)).padding(16.dp)) {
                    Text(displayTitle, color = Color.White, fontWeight = FontWeight.Bold)
                    if (displayCaption.isNotBlank()) {
                        Text(displayCaption, color = Color.LightGray, fontSize = 12.sp)
                    }
                    Text(displayLocationAndSource, color = Color(0xFF34D399), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerModal(videoUrl: String, title: String, source: String, language: AppLanguage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth(0.95f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == AppLanguage.BN) source else translateSourceToEn(source), color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
                Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    IconButton(onClick = { openLink(context, videoUrl) }, modifier = Modifier.size(64.dp).background(Color.Red, CircleShape)) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Text(title, color = Color.White, modifier = Modifier.padding(vertical = 12.dp))
                Button(onClick = { openLink(context, videoUrl) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))) {
                    Text(if (language == AppLanguage.BN) "ইউটিউবে দেখুন" else "Watch on YouTube")
                }
            }
        }
    }
}

private fun openLink(context: Context, url: String) {
    com.example.util.IntentUtils.safeOpenUrl(context, url.ifBlank { "https://google.com" })
}

private fun shareGalleryItem(context: Context, item: GalleryItem, language: AppLanguage) {
    val title = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val loc = if (language == AppLanguage.BN) item.locationBn else translateLocationToEn(item.locationBn)
    val src = if (language == AppLanguage.BN) item.verifiedSource else translateSourceToEn(item.verifiedSource)
    val text = if (language == AppLanguage.BN) {
        "$title\nস্থান: $loc\nসূত্র: $src"
    } else {
        "$title\nLocation: $loc\nSource: $src"
    }
    com.example.util.IntentUtils.safeShareText(
        context = context,
        text = text,
        subject = title,
        chooserTitle = if (language == AppLanguage.BN) "শেয়ার করুন" else "Share",
        language = language
    )
}

private fun shareMediaItem(context: Context, item: MediaArchiveItem, language: AppLanguage) {
    val title = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val outlet = if (language == AppLanguage.BN) item.mediaOutletBn else translateOutletToEn(item.mediaOutletBn)
    val text = if (language == AppLanguage.BN) {
        "$title\nমাধ্যম: $outlet\nলিংক: ${item.externalUrl}"
    } else {
        "$title\nMedia: $outlet\nLink: ${item.externalUrl}"
    }
    com.example.util.IntentUtils.safeShareText(
        context = context,
        text = text,
        subject = title,
        chooserTitle = if (language == AppLanguage.BN) "শেয়ার করুন" else "Share",
        language = language
    )
}

// --- FORM DIALOGS ---

@Composable
fun GalleryItemFormDialog(item: GalleryItem, isNew: Boolean, language: AppLanguage, onSave: (GalleryItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item.titleBn) }
    var caption by remember { mutableStateOf(item.captionBn) }
    var category by remember { mutableStateOf(item.categoryBn) }
    var location by remember { mutableStateOf(item.locationBn) }
    var union by remember { mutableStateOf(item.unionBn) }
    var source by remember { mutableStateOf(item.verifiedSource) }
    var type by remember { mutableStateOf(item.mediaType) }
    var videoUrl by remember { mutableStateOf(item.videoEmbedUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isNew) (if (language == AppLanguage.BN) "নতুন মিডিয়া" else "Add New Media") else (if (language == AppLanguage.BN) "সম্পাদন করুন" else "Edit Media"),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006A4E)
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (language == AppLanguage.BN) "শিরোনাম" else "Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = caption, onValueChange = { caption = it }, label = { Text(if (language == AppLanguage.BN) "বিবরণ" else "Caption") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(if (language == AppLanguage.BN) "ক্যাটেগরি" else "Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(if (language == AppLanguage.BN) "স্থান" else "Location") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = union, onValueChange = { union = it }, label = { Text(if (language == AppLanguage.BN) "ইউনিয়ন" else "Union") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text(if (language == AppLanguage.BN) "সূত্র" else "Source") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == "PHOTO", onClick = { type = "PHOTO" })
                    Text(if (language == AppLanguage.BN) "ছবি" else "Photo")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = type == "VIDEO", onClick = { type = "VIDEO" })
                    Text(if (language == AppLanguage.BN) "ভিডিও" else "Video")
                }
                if (type == "VIDEO") {
                    OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text(if (language == AppLanguage.BN) "ভিডিও ইউআরএল" else "Video URL") }, modifier = Modifier.fillMaxWidth())
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel") }
                    Button(
                        onClick = {
                            onSave(item.copy(titleBn = title, titleEn = title, captionBn = caption, categoryBn = category, locationBn = location, unionBn = union, verifiedSource = source, mediaType = type, videoEmbedUrl = videoUrl))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun MediaArchiveItemFormDialog(item: MediaArchiveItem, isNew: Boolean, language: AppLanguage, onSave: (MediaArchiveItem) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item.titleBn) }
    var summary by remember { mutableStateOf(item.summaryBn) }
    var outlet by remember { mutableStateOf(item.mediaOutletBn) }
    var date by remember { mutableStateOf(item.publicationDateBn) }
    var url by remember { mutableStateOf(item.externalUrl) }
    var source by remember { mutableStateOf(item.verifiedSource) }
    var category by remember { mutableStateOf(item.categoryBn) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isNew) (if (language == AppLanguage.BN) "নতুন সংবাদ" else "Add New News") else (if (language == AppLanguage.BN) "সম্পাদন করুন" else "Edit News"),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006A4E)
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (language == AppLanguage.BN) "শিরোনাম" else "Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = summary, onValueChange = { summary = it }, label = { Text(if (language == AppLanguage.BN) "সারাংশ" else "Summary") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = outlet, onValueChange = { outlet = it }, label = { Text(if (language == AppLanguage.BN) "মাধ্যম" else "Outlet") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text(if (language == AppLanguage.BN) "তারিখ" else "Date") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(if (language == AppLanguage.BN) "লিংক" else "URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(if (language == AppLanguage.BN) "ক্যাটেগরি" else "Category") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text(if (language == AppLanguage.BN) "সূত্র" else "Source") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel") }
                    Button(
                        onClick = {
                            onSave(item.copy(titleBn = title, titleEn = title, summaryBn = summary, mediaOutletBn = outlet, publicationDateBn = date, externalUrl = url, verifiedSource = source, categoryBn = category))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportantLinkCard(
    item: ImportantExternalLink,
    language: AppLanguage,
    isAdmin: Boolean,
    onOpenLink: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val displayCategory = getLinkCategoryLabel(item.categoryBn, language)
    val displaySource = if (language == AppLanguage.BN) item.sourceName else translateSourceName(item.sourceName)
    val displayTitle = if (language == AppLanguage.BN) item.titleBn else item.titleEn.ifBlank { item.titleBn }
    val displayDesc = if (language == AppLanguage.BN) item.descriptionBn else item.descriptionEn.ifBlank { item.descriptionBn }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLink() }
            .testTag("important_link_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                item.categoryBn.contains("উইকিপিডিয়া") -> Color(0xFF006A4E)
                                item.categoryBn.contains("সংসদ") -> Color(0xFF1E3A8A)
                                item.categoryBn.contains("প্রকল্প") -> Color(0xFFD97706)
                                else -> Color(0xFF0F766E)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            item.categoryBn.contains("সংসদ") -> Icons.Default.AccountBalance
                            else -> Icons.Default.Language
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                            Text(text = displayCategory, color = Color(0xFF006A4E), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (displaySource.isNotBlank()) {
                            Text(text = "• $displaySource", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = displayTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), lineHeight = 20.sp)
                }
            }

            if (displayDesc.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = displayDesc, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Language, null, tint = Color(0xFF006A4E), modifier = Modifier.size(14.dp))
                    Text(text = item.url, fontSize = 11.sp, color = Color(0xFF0284C7), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onOpenLink,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(text = if (language == AppLanguage.BN) "লিংক দেখুন" else "Visit Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, null, tint = Color(0xFF006A4E), modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportantLinkFormDialog(
    item: ImportantExternalLink,
    isNew: Boolean,
    language: AppLanguage,
    onSave: (ImportantExternalLink) -> Unit,
    onDismiss: () -> Unit
) {
    var titleBn by remember { mutableStateOf(item.titleBn) }
    var url by remember { mutableStateOf(item.url) }
    var categoryBn by remember { mutableStateOf(item.categoryBn.ifBlank { "উইকিপিডিয়া" }) }
    var sourceName by remember { mutableStateOf(item.sourceName.ifBlank { "ওয়েব" }) }
    var descriptionBn by remember { mutableStateOf(item.descriptionBn) }

    val categories = listOf("উইকিপিডিয়া", "জাতীয় সংসদ", "মেগা প্রকল্প", "সংবাদ আর্কাইভ", "অফিসিয়াল পোর্টাল")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isNew) (if (language == AppLanguage.BN) "➕ নতুন লিংক" else "➕ Add Link") else (if (language == AppLanguage.BN) "✏️ সম্পাদন করুন" else "✏️ Edit Link"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF006A4E)
                )
                OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text(if (language == AppLanguage.BN) "শিরোনাম" else "Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text(if (language == AppLanguage.BN) "ওয়েব লিংক URL" else "Web Link URL") }, modifier = Modifier.fillMaxWidth())
                Text(if (language == AppLanguage.BN) "ক্যাটেগরি:" else "Category:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { c ->
                        FilterChip(
                            selected = categoryBn == c,
                            onClick = { categoryBn = c },
                            label = { Text(getLinkCategoryLabel(c, language), fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(value = sourceName, onValueChange = { sourceName = it }, label = { Text(if (language == AppLanguage.BN) "উৎস / পোর্টাল নাম" else "Source / Portal Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = descriptionBn, onValueChange = { descriptionBn = it }, label = { Text(if (language == AppLanguage.BN) "সংক্ষিপ্ত বিবরণ" else "Short Description") }, modifier = Modifier.fillMaxWidth())

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel") }
                    Button(
                        onClick = {
                            onSave(item.copy(titleBn = titleBn.ifBlank { "লিংক" }, titleEn = titleBn, url = url.ifBlank { "https://google.com" }, categoryBn = categoryBn, sourceName = sourceName, descriptionBn = descriptionBn))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006A4E))
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

// ---------------- TRANSLATION HELPERS ----------------

private fun getGalleryCategoryTitle(catBn: String, language: AppLanguage): String {
    if (language == AppLanguage.BN) return catBn
    return when (catBn.trim()) {
        "সকল" -> "All"
        "উন্নয়নচিত্র" -> "Development"
        "শিক্ষা ও স্বাস্থ্য" -> "Education & Health"
        "জনজীবন ও জনসেবা" -> "Public Life & Services"
        "রাজনৈতিক ও সাংগঠনিক কার্যক্রম" -> "Political & Organizational"
        "ঐতিহাসিক ছবি" -> "Historical Photos"
        "অনুষ্ঠান ও বিশেষ আয়োজন" -> "Events & Ceremonies"
        "দুর্যোগ ও উপকূল" -> "Disaster & Coastline"
        "ব্যক্তিগত ও স্মৃতিচারণ" -> "Personal & Memories"
        else -> catBn
    }
}

private fun getMediaCategoryLabel(catBn: String, language: AppLanguage): String {
    if (language == AppLanguage.BN) return catBn
    return when (catBn.trim()) {
        "সকল" -> "All"
        "সংবাদ প্রতিবেদন" -> "News Reports"
        "ভিডিও প্রতিবেদন" -> "Video Reports"
        "সাক্ষাৎকার" -> "Interviews"
        "বক্তব্য ও বিবৃতি" -> "Statements"
        "সংবাদ সম্মেলন" -> "Press Conferences"
        "পত্রিকার কাটিং" -> "Clippings"
        "টকশো ও আলোচনা" -> "Talkshows"
        "প্রেস রিলিজ" -> "Press Releases"
        else -> catBn
    }
}

private fun getLinkCategoryLabel(catBn: String, language: AppLanguage): String {
    if (language == AppLanguage.BN) return catBn
    return when {
        catBn.contains("উইকিপিডিয়া") -> "Wikipedia"
        catBn.contains("সংসদ") -> "National Parliament"
        catBn.contains("প্রকল্প") -> "Mega Projects"
        catBn.contains("সংবাদ") -> "News Archive"
        catBn.contains("পোর্টাল") -> "Official Portal"
        else -> catBn
    }
}

private fun translateLocationToEn(loc: String): String {
    if (loc.isBlank()) return "Maheshkhali - Kutubdia"
    return loc.replace("মহেশখালী", "Maheshkhali")
        .replace("কুতুবদিয়া", "Kutubdia")
        .replace("কক্সবাজার", "Cox's Bazar")
        .replace("গোরকঘাটা", "Gorokghata")
        .replace("হোয়ানক", "Hoanak")
        .replace("কালারমারছড়া", "Kalarmarchhora")
        .replace("শাপলাপুর", "Shaplapur")
        .replace("মাতারবাড়ি", "Matarbari")
        .replace("কুতুবজোম", "Kutubjom")
        .replace("ছোটমহেশখালী", "Chhota Maheshkhali")
        .replace("বড়মহেশখালী", "Boro Maheshkhali")
        .replace("বড়ঘোপ", "Baraghop")
        .replace("দক্ষিণ ধুরুং", "South Dhurung")
        .replace("উত্তর ধুরুং", "North Dhurung")
        .replace("কৈয়ারবিল", "Kaiyarbil")
        .replace("লেমশীখালী", "Lemshikhali")
        .replace("আলী আকবর ডেইল", "Ali Akbar Deil")
        .replace("সকল এলাকা", "All Areas")
}

private fun translateSourceToEn(source: String): String {
    if (source.isBlank()) return "Official Archives"
    return when {
        source.contains("সরকারি") || source.contains("প্রেস উইং") -> "Official Archives & Press Wing"
        source.contains("গণমাধ্যম") -> "Media Archives"
        source.contains("সংবাদ") -> "News Archives"
        else -> source
    }
}

private fun translateSourceName(name: String): String {
    if (name.isBlank()) return "Official"
    return when {
        name.contains("ওয়েব") -> "Web"
        name.contains("সংসদ") -> "Parliament"
        name.contains("উইকিপিডিয়া") -> "Wikipedia"
        else -> name
    }
}

private fun translateDateToEn(dateBn: String): String {
    if (dateBn.isBlank()) return ""
    var res = dateBn
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    for (i in 0..9) {
        res = res.replace(bnDigits[i], enDigits[i])
    }
    return res.replace("জানুয়ারি", "January")
        .replace("ফেব্রুয়ারি", "February")
        .replace("মার্চ", "March")
        .replace("এপ্রিল", "April")
        .replace("মে", "May")
        .replace("জুন", "June")
        .replace("জুলাই", "July")
        .replace("আগস্ট", "August")
        .replace("সেপ্টেম্বর", "September")
        .replace("অক্টোবর", "October")
        .replace("নভেম্বর", "November")
        .replace("ডিসেম্বর", "December")
}

private fun translateOutletToEn(outlet: String): String {
    if (outlet.isBlank()) return "National Media"
    return when {
        outlet.contains("প্রথম আলো") -> "Prothom Alo"
        outlet.contains("কালের কণ্ঠ") -> "Kaler Kantho"
        outlet.contains("যুগান্তর") -> "Jugantor"
        outlet.contains("ইত্তেফাক") -> "Ittefaq"
        outlet.contains("বাংলাদেশ প্রতিদিন") -> "Bangladesh Pratidin"
        outlet.contains("দৈনিক কক্সবাজার") -> "Dainik Cox's Bazar"
        outlet.contains("সময় টিভি") -> "Somoy TV"
        outlet.contains("যমুনা টিভি") -> "Jamuna TV"
        outlet.contains("চ্যানেল ২৪") -> "Channel 24"
        outlet.contains("ডিবিসি") -> "DBC News"
        outlet.contains("একাত্তর টিভি") -> "Ekattor TV"
        outlet.contains("বিটিভি") -> "BTV"
        else -> outlet
    }
}
