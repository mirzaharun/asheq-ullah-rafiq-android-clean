package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.*
import com.example.data.repository.DefaultPortalContent
import com.example.ui.components.*
import com.example.util.IntentUtils
import com.example.BuildConfig

@Composable
fun HomeScreen(
    weatherData: WeatherData,
    publicServices: List<PublicServiceItem>,
    digitalServices: List<DigitalServiceItem>,
    dailySchedules: List<DailyScheduleItem>,
    specialEvents: List<SpecialEventItem> = emptyList(),
    language: AppLanguage,
    onWeatherClick: () -> Unit,
    onSeaTruckClick: () -> Unit,
    onServiceClick: (String) -> Unit,
    onTourismClick: () -> Unit,
    onDecadeDetailClick: () -> Unit,
    onNoticeClick: () -> Unit,
    modifier: Modifier = Modifier,
    mediaAssets: DynamicMediaAssets = DynamicMediaAssets(),
    appConfig: AppConfig = AppConfig(),
    latestAppRelease: AppRelease? = null,
    isAdminAuthenticated: Boolean = false,
    onEditClick: () -> Unit = {},
    onSaveConfig: (AppConfig) -> Unit = {},
    onSaveService: (DigitalServiceItem) -> Unit = {},
    onDeleteService: (String) -> Unit = {},
    onSaveSchedule: (DailyScheduleItem) -> Unit = {},
    onDeleteSchedule: (String) -> Unit = {},
    onSaveSpecialEvent: (SpecialEventItem) -> Unit = {},
    onDeleteSpecialEvent: (String) -> Unit = {},
    homepageCategories: List<HomepageCategoryItem> = DefaultPortalContent.defaultHomepageCategories,
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(true) }
    var showEditNoticeDialog by remember { mutableStateOf(false) }
    
    // CRUD state
    var editingSchedule by remember { mutableStateOf<DailyScheduleItem?>(null) }
    var isAddingSchedule by remember { mutableStateOf(false) }
    var editingSpecialEvent by remember { mutableStateOf<SpecialEventItem?>(null) }
    var isAddingSpecialEvent by remember { mutableStateOf(false) }
    val updateUrl = latestAppRelease?.downloadUrl?.trim()
        ?.takeIf { Uri.parse(it).host == "firebasestorage.googleapis.com" }
        ?: appConfig.apkUrl.trim()
    val updateVersionName = latestAppRelease?.versionName?.takeIf { it.isNotBlank() }
        ?: appConfig.latestVersionName
    val updateMessage = if (language == AppLanguage.BN) {
        latestAppRelease?.releaseNotesBn?.takeIf { it.isNotBlank() }
            ?: appConfig.updateMessageBn
    } else {
        latestAppRelease?.releaseNotesEn?.takeIf { it.isNotBlank() }
            ?: appConfig.updateMessageEn
    }
    val updateAvailable = latestAppRelease != null ||
        (appConfig.latestVersionCode > BuildConfig.VERSION_CODE &&
            appConfig.apkUrl.trim().startsWith("https://", ignoreCase = true) &&
            !Uri.parse(appConfig.apkUrl.trim()).host.isNullOrBlank())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .testTag("home_screen_content")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeroBanner(
                language = language,
                customVideoSource = mediaAssets.bannerVideoSource,
                customPosterSource = mediaAssets.bannerPosterSource,
                isMuted = isMuted,
                onToggleMute = { isMuted = !isMuted }
            )
            if (isAdminAuthenticated) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit Banner", tint = Color.White)
                }
            }
        }

        if (updateAvailable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BN) {
                                "নতুন অ্যাপ আপডেট${updateVersionName.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}"
                            } else {
                                "App update available${updateVersionName.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF1B5E20)
                        )
                        if (updateMessage.isNotBlank()) {
                            Text(
                                text = updateMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF33691E),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = {
                            IntentUtils.safeOpenUrl(context, updateUrl, language)
                        }
                    ) {
                        Text(if (language == AppLanguage.BN) "আপডেট" else "Update")
                    }
                }
            }
        }

        MarqueeNoticeBar(
            appConfig = appConfig,
            language = language,
            isAdminAuthenticated = isAdminAuthenticated,
            onEditClick = { showEditNoticeDialog = true }
        )

        WeatherAndTransportCards(
            weatherData = weatherData,
            language = language,
            onWeatherClick = onWeatherClick,
            onSeaTruckClick = onSeaTruckClick
        )

        PublicServicesSection(
            services = publicServices,
            homepageCategories = homepageCategories,
            language = language,
            onServiceClick = onServiceClick,
            onTourismClick = onTourismClick
        )

        SpecialEventsSection(
            events = specialEvents,
            language = language,
            isAdmin = isAdminAuthenticated,
            onAddClick = { isAddingSpecialEvent = true },
            onEditClick = { editingSpecialEvent = it },
            onDeleteClick = onDeleteSpecialEvent
        )

        DailyScheduleSection(
            schedules = dailySchedules,
            language = language,
            isAdmin = isAdminAuthenticated,
            onAddClick = { isAddingSchedule = true },
            onEditClick = { editingSchedule = it },
            onDeleteClick = onDeleteSchedule
        )
    }

    // Dialogs
    if (showEditNoticeDialog) {
        AdminNoticeEditDialog(
            appConfig = appConfig,
            language = language,
            onSave = onSaveConfig,
            onDismiss = { showEditNoticeDialog = false }
        )
    }

    if (isAddingSchedule || editingSchedule != null) {
        DailyScheduleEditDialog(item = editingSchedule, isNew = isAddingSchedule, onSave = { onSaveSchedule(it); isAddingSchedule = false; editingSchedule = null }, onDismiss = { isAddingSchedule = false; editingSchedule = null }, language = language)
    }

    if (isAddingSpecialEvent || editingSpecialEvent != null) {
        val initial = editingSpecialEvent ?: SpecialEventItem(id = "SE_${System.currentTimeMillis()}")
        SpecialEventFormDialog(
            item = initial,
            isNew = isAddingSpecialEvent,
            language = language,
            onSave = {
                onSaveSpecialEvent(it)
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            },
            onDismiss = {
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            }
        )
    }
}
