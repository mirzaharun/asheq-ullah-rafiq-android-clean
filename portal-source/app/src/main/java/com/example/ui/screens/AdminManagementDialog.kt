package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.MediaStorageManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdminPanelSection {
    PROJECTS,
    NOTICES,
    GALLERY,
    SCHEDULES,
    EMERGENCY,
    DONORS,
    TIDES_WEATHER,
    MEDIA,
    SPECIAL_EVENTS,
    APPOINTMENTS,
    SOCIAL_MEDIA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementDialog(
    language: AppLanguage,
    currentSignal: Int,
    developmentProjects: List<DevelopmentProject>,
    notices: List<NoticeFeedItem>,
    galleryItems: List<GalleryItem>,
    dynamicSchedules: List<DynamicWatercraftSchedule>,
    emergencyContacts: List<EmergencyContact>,
    bloodDonors: List<BloodDonor>,
    specialEvents: List<SpecialEventItem> = emptyList(),
    appointmentRequests: List<AppointmentRequest> = emptyList(),
    dynamicTide: DynamicTideSchedule,
    dynamicMedia: DynamicMediaAssets,
    weatherData: WeatherData,
    onSaveProject: (DevelopmentProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onSaveNotice: (NoticeFeedItem) -> Unit,
    onDeleteNotice: (String) -> Unit,
    onSaveGalleryItem: (GalleryItem) -> Unit,
    onDeleteGalleryItem: (String) -> Unit,
    onSaveSchedule: (DynamicWatercraftSchedule) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onResetSchedules: () -> Unit,
    onSaveEmergencyContact: (EmergencyContact) -> Unit,
    onDeleteEmergencyContact: (String) -> Unit,
    onSaveBloodDonor: (BloodDonor) -> Unit,
    onDeleteBloodDonor: (String) -> Unit,
    onSaveSpecialEvent: (SpecialEventItem) -> Unit = {},
    onDeleteSpecialEvent: (String) -> Unit = {},
    onUpdateAppointment: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onUpdateTide: (DynamicTideSchedule) -> Unit,
    onUpdateMedia: (DynamicMediaAssets) -> Unit,
    onUpdateSignal: (Int) -> Unit,
    onUpdateWeather: (WeatherData) -> Unit,
    socialMediaLinks: List<SocialMediaLink> = emptyList(),
    onSaveSocialMediaLink: (SocialMediaLink) -> Unit = {},
    onDeleteSocialMediaLink: (String) -> Unit = {},
    adminRole: String? = "super_admin",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val canEditCms = adminRole == "super_admin" || adminRole == "editor"
    val canManageApps = adminRole == "super_admin" || adminRole == "application_manager"
    val visibleSections = remember(canEditCms, canManageApps) {
        buildList {
            if (canEditCms) {
                add(AdminPanelSection.PROJECTS)
                add(AdminPanelSection.NOTICES)
                add(AdminPanelSection.GALLERY)
                add(AdminPanelSection.SCHEDULES)
                add(AdminPanelSection.EMERGENCY)
                add(AdminPanelSection.DONORS)
                add(AdminPanelSection.TIDES_WEATHER)
                add(AdminPanelSection.MEDIA)
                add(AdminPanelSection.SPECIAL_EVENTS)
            }
            if (canManageApps) {
                add(AdminPanelSection.APPOINTMENTS)
            }
            if (canEditCms) {
                add(AdminPanelSection.SOCIAL_MEDIA)
            }
        }.ifEmpty { listOf(AdminPanelSection.PROJECTS) }
    }
    var selectedSection by remember(visibleSections) { mutableStateOf(visibleSections.first()) }

    // Dialog state holders for editing/adding
    var editingProject by remember { mutableStateOf<DevelopmentProject?>(null) }
    var isAddingProject by remember { mutableStateOf(false) }

    var editingNotice by remember { mutableStateOf<NoticeFeedItem?>(null) }
    var isAddingNotice by remember { mutableStateOf(false) }

    var editingGalleryItem by remember { mutableStateOf<GalleryItem?>(null) }
    var isAddingGalleryItem by remember { mutableStateOf(false) }

    var editingSchedule by remember { mutableStateOf<DynamicWatercraftSchedule?>(null) }
    var isAddingSchedule by remember { mutableStateOf(false) }

    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var isAddingContact by remember { mutableStateOf(false) }

    var editingDonor by remember { mutableStateOf<BloodDonor?>(null) }
    var isAddingDonor by remember { mutableStateOf(false) }

    var editingSpecialEvent by remember { mutableStateOf<SpecialEventItem?>(null) }
    var isAddingSpecialEvent by remember { mutableStateOf(false) }

    var editingAppointment by remember { mutableStateOf<AppointmentRequest?>(null) }
    var editingSocialLink by remember { mutableStateOf<SocialMediaLink?>(null) }
    var isAddingSocialLink by remember { mutableStateOf(false) }


    var deleteConfirmItem by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun showSuccessToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("admin_management_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Native Bar
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "এডমিন ম্যানেজমেন্ট পোর্টাল" else "Admin Management Console",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00E676))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "ফায়ারবেস লাইভ ক্লাউড সিঙ্ক সক্রিয়" else "Firebase Live Cloud Sync Active",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_admin_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Scrollable Native Mobile Tabs (Role-Scoped)
                val selectedTabIndex = visibleSections.indexOf(selectedSection).coerceAtLeast(0)
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = RoyalBlue800,
                    contentColor = Color.White,
                    edgePadding = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (AdminPanelSection.PROJECTS in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.PROJECTS,
                            onClick = { selectedSection = AdminPanelSection.PROJECTS },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "প্রকল্প" else "Projects",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Apartment, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_projects")
                        )
                    }
                    if (AdminPanelSection.NOTICES in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.NOTICES,
                            onClick = { selectedSection = AdminPanelSection.NOTICES },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "বিজ্ঞপ্তি" else "Notices",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Campaign, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_notices")
                        )
                    }
                    if (AdminPanelSection.GALLERY in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.GALLERY,
                            onClick = { selectedSection = AdminPanelSection.GALLERY },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "গ্যালারি" else "Gallery",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_gallery")
                        )
                    }
                    if (AdminPanelSection.SCHEDULES in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.SCHEDULES,
                            onClick = { selectedSection = AdminPanelSection.SCHEDULES },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "নৌযান সূচি" else "Schedules",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.DirectionsBoat, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_schedules")
                        )
                    }
                    if (AdminPanelSection.EMERGENCY in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.EMERGENCY,
                            onClick = { selectedSection = AdminPanelSection.EMERGENCY },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "জরুরি সেবা" else "Emergency",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.ContactPhone, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_emergency")
                        )
                    }
                    if (AdminPanelSection.DONORS in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.DONORS,
                            onClick = { selectedSection = AdminPanelSection.DONORS },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "রক্তদাতা" else "Donors",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Bloodtype, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_donors")
                        )
                    }
                    if (AdminPanelSection.TIDES_WEATHER in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.TIDES_WEATHER,
                            onClick = { selectedSection = AdminPanelSection.TIDES_WEATHER },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "জোয়ার ও সংকেত" else "Tides & Weather",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Water, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_tides")
                        )
                    }
                    if (AdminPanelSection.MEDIA in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.MEDIA,
                            onClick = { selectedSection = AdminPanelSection.MEDIA },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "মিডিয়া" else "Media",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_media")
                        )
                    }
                    if (AdminPanelSection.SPECIAL_EVENTS in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.SPECIAL_EVENTS,
                            onClick = { selectedSection = AdminPanelSection.SPECIAL_EVENTS },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "বিশেষ দিবস" else "Special Events",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Event, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_special_events")
                        )
                    }
                    if (AdminPanelSection.APPOINTMENTS in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.APPOINTMENTS,
                            onClick = { selectedSection = AdminPanelSection.APPOINTMENTS },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "আবেদন ও সাক্ষাত" else "Appointments",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.QuestionAnswer, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_appointments")
                        )
                    }
                    if (AdminPanelSection.SOCIAL_MEDIA in visibleSections) {
                        Tab(
                            selected = selectedSection == AdminPanelSection.SOCIAL_MEDIA,
                            onClick = { selectedSection = AdminPanelSection.SOCIAL_MEDIA },
                            text = {
                                Text(
                                    text = if (language == AppLanguage.BN) "সোস্যাল মিডিয়া" else "Social Media",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("admin_tab_social_media")
                        )
                    }
                }

                // Section Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF7F9FC))
                ) {
                    when (selectedSection) {
                        AdminPanelSection.PROJECTS -> {
                            ProjectsAdminSection(
                                language = language,
                                projects = developmentProjects,
                                onAddNew = { isAddingProject = true },
                                onEdit = { editingProject = it },
                                onDelete = { proj ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "উন্নয়ন প্রকল্প: ${proj.titleBn}" else "Project: ${proj.titleEn}") to {
                                        onDeleteProject(proj.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "প্রকল্প সফলভাবে মুছে ফেলা হয়েছে" else "Project deleted successfully")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.NOTICES -> {
                            NoticesAdminSection(
                                language = language,
                                notices = notices,
                                onAddNew = { isAddingNotice = true },
                                onEdit = { editingNotice = it },
                                onDelete = { notice ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "বিজ্ঞপ্তি: ${notice.titleBn}" else "Notice: ${notice.titleEn}") to {
                                        onDeleteNotice(notice.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "বিজ্ঞপ্তি মুছে ফেলা হয়েছে" else "Notice deleted successfully")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.GALLERY -> {
                            GalleryAdminSection(
                                language = language,
                                items = galleryItems,
                                onAddNew = { isAddingGalleryItem = true },
                                onEdit = { editingGalleryItem = it },
                                onDelete = { item ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "গ্যালারি আইটেম: ${item.titleBn}" else "Gallery Item: ${item.titleEn}") to {
                                        onDeleteGalleryItem(item.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "গ্যালারি আইটেম মুছে ফেলা হয়েছে" else "Gallery item deleted")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.SCHEDULES -> {
                            SchedulesAdminSection(
                                language = language,
                                schedules = dynamicSchedules,
                                onAddNew = { isAddingSchedule = true },
                                onEdit = { editingSchedule = it },
                                onDelete = { sched ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "সূচি: ${sched.routeTitleBn}" else "Schedule: ${sched.routeTitleEn}") to {
                                        onDeleteSchedule(sched.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "সূচি মুছে ফেলা হয়েছে" else "Schedule deleted")
                                    }
                                },
                                onReset = onResetSchedules
                            )
                        }
                        AdminPanelSection.EMERGENCY -> {
                            EmergencyAdminSection(
                                language = language,
                                contacts = emergencyContacts,
                                onAddNew = { isAddingContact = true },
                                onEdit = { editingContact = it },
                                onDelete = { contact ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "জরুরি যোগাযোগ: ${contact.titleBn}" else "Contact: ${contact.titleEn}") to {
                                        onDeleteEmergencyContact(contact.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "যোগাযোগ মুছে ফেলা হয়েছে" else "Contact deleted")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.DONORS -> {
                            DonorsAdminSection(
                                language = language,
                                donors = bloodDonors,
                                onAddNew = { isAddingDonor = true },
                                onEdit = { editingDonor = it },
                                onDelete = { donor ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "রক্তদাতা: ${donor.nameBn}" else "Donor: ${donor.nameEn}") to {
                                        onDeleteBloodDonor(donor.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "রক্তদাতা মুছে ফেলা হয়েছে" else "Donor removed")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.TIDES_WEATHER -> {
                            TideAndWeatherAdminSection(
                                language = language,
                                dynamicTide = dynamicTide,
                                weatherData = weatherData,
                                currentSignal = currentSignal,
                                onUpdateTide = {
                                    onUpdateTide(it)
                                    showSuccessToast(if (language == AppLanguage.BN) "জোয়ারের তথ্য ফায়ারবেসে সংরক্ষিত" else "Tide schedule saved to Firebase")
                                },
                                onUpdateSignal = {
                                    onUpdateSignal(it)
                                    showSuccessToast(if (language == AppLanguage.BN) "সতর্ক সংকেত হালনাগাদ হয়েছে" else "Maritime signal updated")
                                }
                            )
                        }
                        AdminPanelSection.MEDIA -> {
                            MediaAdminSection(
                                language = language,
                                dynamicMedia = dynamicMedia,
                                onUpdateMedia = {
                                    onUpdateMedia(it)
                                    showSuccessToast(if (language == AppLanguage.BN) "মিডিয়া অ্যাসেট সংরক্ষিত হয়েছে" else "Media assets saved")
                                }
                            )
                        }
                        AdminPanelSection.SPECIAL_EVENTS -> {
                            SpecialEventsAdminSection(
                                language = language,
                                events = specialEvents,
                                onAddNew = { isAddingSpecialEvent = true },
                                onEdit = { editingSpecialEvent = it },
                                onDelete = { evt ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "বিশেষ দিবস: ${evt.titleBn}" else "Event: ${evt.titleEn}") to {
                                        onDeleteSpecialEvent(evt.id)
                                        showSuccessToast(if (language == AppLanguage.BN) "বিশেষ দিবস মুছে ফেলা হয়েছে" else "Special event deleted")
                                    }
                                }
                            )
                        }
                        AdminPanelSection.APPOINTMENTS -> {
                            AppointmentsAdminSection(
                                language = language,
                                requests = appointmentRequests,
                                onEdit = { editingAppointment = it }
                            )
                        }
                        AdminPanelSection.SOCIAL_MEDIA -> {
                            SocialMediaAdminSection(
                                language = language,
                                links = socialMediaLinks,
                                onAddNew = { isAddingSocialLink = true },
                                onEdit = { editingSocialLink = it },
                                onDelete = { link ->
                                    deleteConfirmItem = (if (language == AppLanguage.BN) "লিঙ্ক: ${link.platformNameBn}" else "Link: ${link.platformNameEn}") to {
                                        onDeleteSocialMediaLink(link.id)
                                        showSuccessToast("Deleted")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Add or Edit Project
    if (isAddingProject || editingProject != null) {
        val initial = editingProject ?: DevelopmentProject(id = "PROJ_${System.currentTimeMillis()}")
        ProjectEditDialog(
            language = language,
            project = initial,
            isNew = isAddingProject,
            onSave = { updated ->
                onSaveProject(updated)
                showSuccessToast(if (language == AppLanguage.BN) "প্রকল্প ফায়ারবেসে সফলভাবে সংরক্ষিত" else "Project saved to Firebase successfully")
                isAddingProject = false
                editingProject = null
            },
            onDismiss = {
                isAddingProject = false
                editingProject = null
            }
        )
    }

    // Modal: Add or Edit Notice
    if (isAddingNotice || editingNotice != null) {
        val initial = editingNotice ?: NoticeFeedItem(id = "N_${System.currentTimeMillis()}")
        NoticeEditDialog(
            language = language,
            notice = initial,
            isNew = isAddingNotice,
            onSave = { updated ->
                onSaveNotice(updated)
                showSuccessToast(if (language == AppLanguage.BN) "বিজ্ঞপ্তি ফায়ারবেসে সফলভাবে সংরক্ষিত" else "Notice saved to Firebase successfully")
                isAddingNotice = false
                editingNotice = null
            },
            onDismiss = {
                isAddingNotice = false
                editingNotice = null
            }
        )
    }

    // Modal: Add or Edit Gallery Item
    if (isAddingGalleryItem || editingGalleryItem != null) {
        val initial = editingGalleryItem ?: GalleryItem(id = "G_${System.currentTimeMillis()}")
        GalleryItemEditDialog(
            language = language,
            galleryItem = initial,
            isNew = isAddingGalleryItem,
            onSave = { updated ->
                onSaveGalleryItem(updated)
                showSuccessToast(if (language == AppLanguage.BN) "গ্যালারি ফায়ারবেসে সংরক্ষিত" else "Gallery saved to Firebase")
                isAddingGalleryItem = false
                editingGalleryItem = null
            },
            onDismiss = {
                isAddingGalleryItem = false
                editingGalleryItem = null
            }
        )
    }

    // Modal: Add or Edit Watercraft Schedule
    if (isAddingSchedule || editingSchedule != null) {
        val initial = editingSchedule ?: DynamicWatercraftSchedule(id = "SCHED_${System.currentTimeMillis()}")
        ScheduleEditDialog(
            language = language,
            schedule = initial,
            isNew = isAddingSchedule,
            onSave = { updated ->
                onSaveSchedule(updated)
                showSuccessToast(if (language == AppLanguage.BN) "নৌযান সূচি ফায়ারবেসে সংরক্ষিত" else "Schedule saved to Firebase")
                isAddingSchedule = false
                editingSchedule = null
            },
            onDismiss = {
                isAddingSchedule = false
                editingSchedule = null
            }
        )
    }

    // Modal: Add or Edit Emergency Contact
    if (isAddingContact || editingContact != null) {
        val initial = editingContact ?: EmergencyContact(id = "E_${System.currentTimeMillis()}")
        ContactEditDialog(
            language = language,
            contact = initial,
            isNew = isAddingContact,
            onSave = { updated ->
                onSaveEmergencyContact(updated)
                showSuccessToast(if (language == AppLanguage.BN) "জরুরি সেবা সংরক্ষিত হয়েছে" else "Emergency contact saved")
                isAddingContact = false
                editingContact = null
            },
            onDismiss = {
                isAddingContact = false
                editingContact = null
            }
        )
    }

    // Modal: Add or Edit Blood Donor
    if (isAddingDonor || editingDonor != null) {
        val initial = editingDonor ?: BloodDonor(id = "B_${System.currentTimeMillis()}")
        DonorEditDialog(
            language = language,
            donor = initial,
            isNew = isAddingDonor,
            onSave = { updated ->
                onSaveBloodDonor(updated)
                showSuccessToast(if (language == AppLanguage.BN) "রক্তদাতা তথ্য সংরক্ষিত হয়েছে" else "Donor info saved")
                isAddingDonor = false
                editingDonor = null
            },
            onDismiss = {
                isAddingDonor = false
                editingDonor = null
            }
        )
    }

    // Modal: Add or Edit Special Event
    if (isAddingSpecialEvent || editingSpecialEvent != null) {
        val initial = editingSpecialEvent ?: SpecialEventItem(id = "EVT_${System.currentTimeMillis()}")
        SpecialEventFormDialog(
            item = initial,
            isNew = isAddingSpecialEvent,
            language = language,
            onSave = { updated ->
                onSaveSpecialEvent(updated)
                showSuccessToast(if (language == AppLanguage.BN) "বিশেষ দিবস ফায়ারবেসে সংরক্ষিত" else "Special event saved")
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            },
            onDismiss = {
                isAddingSpecialEvent = false
                editingSpecialEvent = null
            }
        )
    }

    // Modal: Edit Appointment
    editingAppointment?.let { request ->
        AppointmentEditDialog(
            language = language,
            request = request,
            onSave = { status, note, date ->
                onUpdateAppointment(request.id, status, note, date)
                showSuccessToast(if (language == AppLanguage.BN) "আবেদনের স্ট্যাটাস হালনাগাদ হয়েছে" else "Application status updated")
                editingAppointment = null
            },
            onDismiss = { editingAppointment = null }
        )
    }

    // Modal: Add or Edit Social Media Link
    if (isAddingSocialLink || editingSocialLink != null) {
        val initial = editingSocialLink ?: SocialMediaLink(id = "SOC_${System.currentTimeMillis()}")
        SocialMediaEditDialog(
            link = initial,
            isNew = isAddingSocialLink,
            language = language,
            onSave = { updated ->
                onSaveSocialMediaLink(updated)
                isAddingSocialLink = false
                editingSocialLink = null
                showSuccessToast(if (language == AppLanguage.BN) "সোস্যাল মিডিয়া লিঙ্ক সংরক্ষিত হয়েছে" else "Social media link saved")
            },
            onDismiss = {
                isAddingSocialLink = false
                editingSocialLink = null
            }
        )
    }

    deleteConfirmItem?.let { (itemTitle, onConfirm) ->
        AlertDialog(
            onDismissRequest = { deleteConfirmItem = null },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(
                    text = if (language == AppLanguage.BN) "স্থায়ীভাবে মুছে ফেলতে চান?" else "Delete Permanently?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.BN)
                        "আপনি কি নিশ্চিত যে '$itemTitle' ফায়ারবেস ডাটাবেজ থেকে মুছে ফেলবেন? এটি রিফ্রেশ করলেও আর ফিরে আসবে না।"
                    else
                        "Are you sure you want to delete '$itemTitle' from Firebase? This will be permanently erased."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        deleteConfirmItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(if (language == AppLanguage.BN) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteConfirmItem = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 1: PROJECTS
// -------------------------------------------------------------------------------------------------
@Composable
private fun ProjectsAdminSection(
    language: AppLanguage,
    projects: List<DevelopmentProject>,
    onAddNew: () -> Unit,
    onEdit: (DevelopmentProject) -> Unit,
    onDelete: (DevelopmentProject) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "উন্নয়ন মেগা প্রকল্পসমূহ" else "Development Projects",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "মোট ${projects.size} টি প্রকল্প ক্লাউডে সংরক্ষিত" else "Total ${projects.size} projects in cloud",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_new_project_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন যোগ করুন" else "+ Add New",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো প্রকল্প পাওয়া যায়নি" else "No projects found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projects, key = { it.id }) { proj ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("project_item_${proj.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        AsyncOrLocalImage(
                                            imageUrl = proj.imageUrl,
                                            contentDescription = "Project image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = if (language == AppLanguage.BN) proj.titleBn else proj.titleEn,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalBlue900
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                color = RoyalBlue50,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) proj.categoryBn else proj.categoryEn,
                                                    fontSize = 11.sp,
                                                    color = RoyalBlue800,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                color = GoldAccent.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) proj.budgetBn else proj.budgetEn,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF996500),
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    color = if (proj.status == "Completed") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${proj.completionPercentage}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (proj.status == "Completed") ForestGreen else Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == AppLanguage.BN) proj.descriptionBn else proj.descriptionEn,
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFEDF2F7))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons Row (Edit & Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onEdit(proj) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                    border = BorderStroke(1.dp, RoyalBlue800),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("edit_project_${proj.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { onDelete(proj) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .testTag("delete_project_${proj.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 2: NOTICES
// -------------------------------------------------------------------------------------------------
@Composable
private fun NoticesAdminSection(
    language: AppLanguage,
    notices: List<NoticeFeedItem>,
    onAddNew: () -> Unit,
    onEdit: (NoticeFeedItem) -> Unit,
    onDelete: (NoticeFeedItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "জরুরি নোটিশ ও বার্তা" else "Admin Notices",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "মোট ${notices.size} টি নোটিশ" else "Total ${notices.size} notices",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_new_notice_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন নোটিশ" else "+ Add Notice",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (notices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো নোটিশ নেই" else "No notices available",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notices, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, if (item.isUrgent) Color(0xFFFFCDD2) else Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("notice_item_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.isUrgent) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.BN) "🚨 জরুরি নোটিশ" else "🚨 URGENT",
                                            fontSize = 11.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        color = RoyalBlue50,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.BN) item.categoryBn else item.categoryEn,
                                            fontSize = 11.sp,
                                            color = RoyalBlue800,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = if (language == AppLanguage.BN) item.dateBn else item.dateEn,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.BN) item.detailsBn else item.detailsEn,
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFEDF2F7))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onEdit(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                    border = BorderStroke(1.dp, RoyalBlue800),
                                    modifier = Modifier.height(40.dp).testTag("edit_notice_${item.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { onDelete(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(40.dp).testTag("delete_notice_${item.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 3: GALLERY
// -------------------------------------------------------------------------------------------------
@Composable
private fun GalleryAdminSection(
    language: AppLanguage,
    items: List<GalleryItem>,
    onAddNew: () -> Unit,
    onEdit: (GalleryItem) -> Unit,
    onDelete: (GalleryItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "মিডিয়া ও ফটো গ্যালারি" else "Photo & Video Gallery",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "মোট ${items.size} টি মিডিয়া ফাইল সংরক্ষিত" else "${items.size} media items in gallery",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("add_new_gallery_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন ছবি" else "+ Add Photo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো ছবি পাওয়া যায়নি" else "No photos found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("gallery_item_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    AsyncOrLocalImage(
                                        imageUrl = item.imageUrl,
                                        contentDescription = "Gallery Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue900
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = (if (language == AppLanguage.BN) item.categoryBn else item.categoryEn) + " • " + (if (language == AppLanguage.BN) item.dateBn else item.dateEn),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFEDF2F7))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onEdit(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                    border = BorderStroke(1.dp, RoyalBlue800),
                                    modifier = Modifier.height(40.dp).testTag("edit_gallery_${item.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { onDelete(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(40.dp).testTag("delete_gallery_${item.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 4: SCHEDULES
// -------------------------------------------------------------------------------------------------
@Composable
private fun SchedulesAdminSection(
    language: AppLanguage,
    schedules: List<DynamicWatercraftSchedule>,
    onAddNew: () -> Unit,
    onEdit: (DynamicWatercraftSchedule) -> Unit,
    onDelete: (DynamicWatercraftSchedule) -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "সি-ট্রাক ও বোট সময়সূচি" else "Watercraft Schedules",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "${schedules.size} টি নৌযান রুট কনফিগার করা" else "${schedules.size} vessel routes configured",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp).testTag("reset_schedules_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = if (language == AppLanguage.BN) "রিসেট" else "Reset", fontSize = 12.sp)
                }

                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp).testTag("add_new_schedule_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "নতুন রুট" else "+ Route",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(schedules, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("schedule_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) item.routeTitleBn else item.routeTitleEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )

                            Surface(
                                color = RoyalBlue50,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) item.vesselTypeBn else item.vesselTypeEn,
                                    fontSize = 11.sp,
                                    color = RoyalBlue800,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == AppLanguage.BN) item.officialNoticeTextBn else item.officialNoticeTextEn,
                            fontSize = 12.sp,
                            color = Color(0xFF4A5568)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val slots = if (language == AppLanguage.BN) item.timeSlotsBn else item.timeSlotsEn
                            slots.take(4).forEach { slot ->
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = slot,
                                        fontSize = 10.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFEDF2F7))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onEdit(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                border = BorderStroke(1.dp, RoyalBlue800),
                                modifier = Modifier.height(40.dp).testTag("edit_schedule_${item.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onDelete(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(40.dp).testTag("delete_schedule_${item.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 5: EMERGENCY CONTACTS
// -------------------------------------------------------------------------------------------------
@Composable
private fun EmergencyAdminSection(
    language: AppLanguage,
    contacts: List<EmergencyContact>,
    onAddNew: () -> Unit,
    onEdit: (EmergencyContact) -> Unit,
    onDelete: (EmergencyContact) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "জরুরি সেবা হটলাইন" else "Emergency Helplines",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "${contacts.size} টি জরুরি নম্বর ডাটাবেজে সক্রিয়" else "${contacts.size} emergency contacts in database",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp).testTag("add_new_contact_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন নম্বর" else "+ Add Contact",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(contacts, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("contact_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )

                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    fontSize = 10.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📞 ${item.phone}" + if (item.secondaryPhone != null) " / ${item.secondaryPhone}" else "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📍 " + if (language == AppLanguage.BN) item.addressBn else item.addressEn,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFEDF2F7))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onEdit(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                border = BorderStroke(1.dp, RoyalBlue800),
                                modifier = Modifier.height(40.dp).testTag("edit_contact_${item.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onDelete(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(40.dp).testTag("delete_contact_${item.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 6: BLOOD DONORS
// -------------------------------------------------------------------------------------------------
@Composable
private fun DonorsAdminSection(
    language: AppLanguage,
    donors: List<BloodDonor>,
    onAddNew: () -> Unit,
    onEdit: (BloodDonor) -> Unit,
    onDelete: (BloodDonor) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "রক্তদাতা ডিরেক্টরি" else "Blood Donors Registry",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "${donors.size} জন নিবন্ধিত রক্তদাতা" else "${donors.size} registered donors",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp).testTag("add_new_donor_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন দাতা" else "+ Add Donor",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(donors, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("donor_item_${item.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) item.nameBn else item.nameEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )

                            Surface(
                                color = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "🩸 ${item.bloodGroup}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 ${item.upazila} • " + if (language == AppLanguage.BN) item.areaBn else item.areaEn,
                            fontSize = 12.sp,
                            color = Color(0xFF4A5568)
                        )
                        Text(
                            text = "📞 ${item.phone}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue800
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFEDF2F7))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onEdit(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                border = BorderStroke(1.dp, RoyalBlue800),
                                modifier = Modifier.height(40.dp).testTag("edit_donor_${item.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onDelete(item) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                modifier = Modifier.height(40.dp).testTag("delete_donor_${item.id}")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 7: TIDES & WEATHER
// -------------------------------------------------------------------------------------------------
@Composable
private fun TideAndWeatherAdminSection(
    language: AppLanguage,
    dynamicTide: DynamicTideSchedule,
    weatherData: WeatherData,
    currentSignal: Int,
    onUpdateTide: (DynamicTideSchedule) -> Unit,
    onUpdateSignal: (Int) -> Unit
) {
    var highTide1 by remember { mutableStateOf(dynamicTide.highTide1Bn) }
    var highTide2 by remember { mutableStateOf(dynamicTide.highTide2Bn) }
    var lowTide1 by remember { mutableStateOf(dynamicTide.lowTide1Bn) }
    var lowTide2 by remember { mutableStateOf(dynamicTide.lowTide2Bn) }
    var marqueeText by remember { mutableStateOf(dynamicTide.safetyMarqueeBn) }
    var selectedSignal by remember { mutableStateOf(currentSignal) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (language == AppLanguage.BN) "জোয়ার-ভাটা ও আবহাওয়া সংকেত কনফিগারেশন" else "Tide Schedule & Weather Signals",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalBlue900
        )

        // Maritime Danger Signal Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "বঙ্গোপসাগরীয় সামুদ্রিক সতর্ক সংকেত" else "Maritime Warning Signal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (0..4).forEach { sig ->
                        val isSelected = selectedSignal == sig
                        Button(
                            onClick = {
                                selectedSignal = sig
                                onUpdateSignal(sig)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RoyalBlue800 else Color(0xFFF1F5F9),
                                contentColor = if (isSelected) Color.White else RoyalBlue900
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("signal_button_$sig")
                        ) {
                            Text(
                                text = if (sig == 0) "০" else "$sig",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Tide Timing Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "জোয়ার-ভাটা সময়সূচি" else "Tide Timing Schedule",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = highTide1,
                        onValueChange = { highTide1 = it },
                        label = { Text(if (language == AppLanguage.BN) "১ম জোয়ার" else "1st High Tide") },
                        modifier = Modifier.weight(1f).testTag("tide_high1_input")
                    )
                    OutlinedTextField(
                        value = highTide2,
                        onValueChange = { highTide2 = it },
                        label = { Text(if (language == AppLanguage.BN) "২য় জোয়ার" else "2nd High Tide") },
                        modifier = Modifier.weight(1f).testTag("tide_high2_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lowTide1,
                        onValueChange = { lowTide1 = it },
                        label = { Text(if (language == AppLanguage.BN) "১ম ভাটা" else "1st Low Tide") },
                        modifier = Modifier.weight(1f).testTag("tide_low1_input")
                    )
                    OutlinedTextField(
                        value = lowTide2,
                        onValueChange = { lowTide2 = it },
                        label = { Text(if (language == AppLanguage.BN) "২য় ভাটা" else "2nd Low Tide") },
                        modifier = Modifier.weight(1f).testTag("tide_low2_input")
                    )
                }

                OutlinedTextField(
                    value = marqueeText,
                    onValueChange = { marqueeText = it },
                    label = { Text(if (language == AppLanguage.BN) "নিরাপত্তা স্ক্রল বার্তা" else "Safety Alert Ticker") },
                    modifier = Modifier.fillMaxWidth().testTag("tide_marquee_input"),
                    minLines = 2
                )

                Button(
                    onClick = {
                        val updated = dynamicTide.copy(
                            highTide1Bn = highTide1,
                            highTide2Bn = highTide2,
                            lowTide1Bn = lowTide1,
                            lowTide2Bn = lowTide2,
                            safetyMarqueeBn = marqueeText,
                            lastUpdated = "এখনই"
                        )
                        onUpdateTide(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_tide_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "ফায়ারবেসে সংরক্ষণ করুন" else "Save to Firebase",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 8: MEDIA & ASSETS
// -------------------------------------------------------------------------------------------------
@Composable
private fun MediaAdminSection(
    language: AppLanguage,
    dynamicMedia: DynamicMediaAssets,
    onUpdateMedia: (DynamicMediaAssets) -> Unit
) {
    val context = LocalContext.current
    var bannerVideo by remember { mutableStateOf(dynamicMedia.bannerVideoSource) }
    var bannerPoster by remember { mutableStateOf(dynamicMedia.bannerPosterSource) }
    var profilePic by remember { mutableStateOf(dynamicMedia.profilePictureSource) }

    val posterPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) {
                bannerPoster = localPath
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) {
                profilePic = localPath
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (language == AppLanguage.BN) "হোম ভিডিও ব্যানার ও প্রোফাইল ছবি" else "Banner Media & Profile Assets",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalBlue900
        )

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Banner Poster Image Picker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "হোম ভিডিও ব্যানার পোস্টার ছবি" else "Home Banner Poster Image",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                AsyncOrLocalImage(
                                    imageUrl = bannerPoster,
                                    contentDescription = "Poster Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        posterPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (language == AppLanguage.BN) "গ্যালারি থেকে ছবি নির্বাচন" else "Pick Poster Image", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Profile Avatar Image Picker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "নেতার প্রোফাইল ফটো" else "Leader Profile Avatar Photo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                AsyncOrLocalImage(
                                    imageUrl = profilePic,
                                    contentDescription = "Profile Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        avatarPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (language == AppLanguage.BN) "গ্যালারি থেকে ছবি নির্বাচন" else "Pick Profile Photo", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = bannerVideo,
                    onValueChange = { bannerVideo = it },
                    label = { Text(if (language == AppLanguage.BN) "ভিডিও ব্যানার পাথ / URL" else "Banner Video Path / URL") },
                    modifier = Modifier.fillMaxWidth().testTag("media_banner_video_input")
                )

                Button(
                    onClick = {
                        val updated = dynamicMedia.copy(
                            bannerVideoSource = bannerVideo,
                            bannerPosterSource = bannerPoster,
                            profilePictureSource = profilePic,
                            lastUpdated = "আজ"
                        )
                        onUpdateMedia(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_media_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "মিডিয়া তথ্য সংরক্ষণ করুন" else "Save Media Assets",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// MODAL DIALOGS: ADD / EDIT
// -------------------------------------------------------------------------------------------------

@Composable
private fun ProjectEditDialog(
    language: AppLanguage,
    project: DevelopmentProject,
    isNew: Boolean,
    onSave: (DevelopmentProject) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var titleBn by remember { mutableStateOf(project.titleBn) }
    var titleEn by remember { mutableStateOf(project.titleEn) }
    var categoryBn by remember { mutableStateOf(project.categoryBn) }
    var categoryEn by remember { mutableStateOf(project.categoryEn) }
    var budgetBn by remember { mutableStateOf(project.budgetBn) }
    var completionStr by remember { mutableStateOf(project.completionPercentage.toString()) }
    var descBn by remember { mutableStateOf(project.descriptionBn) }
    var impactBn by remember { mutableStateOf(project.impactBn) }
    var imageUrl by remember { mutableStateOf(project.imageUrl) }
    var union by remember { mutableStateOf(project.union) }
    var upazila by remember { mutableStateOf(project.upazila) }
    var year by remember { mutableStateOf(project.year.toString()) }
    var beforeImageUrl by remember { mutableStateOf(project.beforeImageUrl) }
    var afterImageUrl by remember { mutableStateOf(project.afterImageUrl) }
    var pdfUrl by remember { mutableStateOf(project.pdfUrl) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) imageUrl = localPath
        }
    }

    val beforePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) beforeImageUrl = localPath
        }
    }

    val afterPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) afterImageUrl = localPath
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.90f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন প্রকল্প যোগ করুন" else "+ Add New Project")
                    else
                        (if (language == AppLanguage.BN) "✏️ প্রকল্প সম্পাদনা" else "✏️ Edit Project"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Image Picker with Thumbnail Preview
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (language == AppLanguage.BN) "প্রকল্পের ছবি / ব্যানার" else "Project Image / Banner",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RoyalBlue900
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    AsyncOrLocalImage(
                                        imageUrl = imageUrl.ifBlank { "drawable:dev_port" },
                                        contentDescription = "Project Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(40.dp).testTag("pick_project_image_button")
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == AppLanguage.BN) "গ্যালারি থেকে ছবি যুক্ত করুন" else "Pick Image from Gallery",
                                            fontSize = 11.5.sp
                                        )
                                    }
                                    if (imageUrl.isNotBlank() && imageUrl != "drawable:dev_port") {
                                        TextButton(
                                            onClick = { imageUrl = "drawable:dev_port" },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.BN) "রিমুভ করুন" else "Remove",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text("প্রকল্পের নাম (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("project_title_bn_input")
                    )
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text("Project Title (English)") },
                        modifier = Modifier.fillMaxWidth().testTag("project_title_en_input")
                    )
                    Text(
                        text = if (language == AppLanguage.BN) "উন্নয়ন ক্যাটাগরি নির্বাচন করুন বা লিখুন:" else "Select or Enter Development Category:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue900
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(STANDARD_DEVELOPMENT_CATEGORIES) { cat ->
                            val isSelected = normalizeDevelopmentText(categoryBn) == normalizeDevelopmentText(cat.labelBn)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryBn = cat.labelBn
                                    categoryEn = cat.labelEn
                                },
                                label = { Text(if (language == AppLanguage.BN) cat.labelBn else cat.labelEn, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = categoryBn,
                        onValueChange = {
                            categoryBn = it
                            val matched = STANDARD_DEVELOPMENT_CATEGORIES.find { c ->
                                normalizeDevelopmentText(c.labelBn) == normalizeDevelopmentText(it)
                            }
                            if (matched != null) {
                                categoryEn = matched.labelEn
                            }
                        },
                        label = { Text("ক্যাটেগরি (যেমন: বিদ্যুৎ ও জ্বালানি / পর্যটন)") },
                        modifier = Modifier.fillMaxWidth().testTag("project_category_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = budgetBn,
                            onValueChange = { budgetBn = it },
                            label = { Text("বাজেট (যেমন: ১৭,৭৭৭ কোটি)") },
                            modifier = Modifier.weight(1f).testTag("project_budget_input")
                        )
                        OutlinedTextField(
                            value = completionStr,
                            onValueChange = { completionStr = it },
                            label = { Text("অগ্রগতি (%)") },
                            modifier = Modifier.weight(0.6f).testTag("project_completion_input")
                        )
                    }
                    OutlinedTextField(
                        value = descBn,
                        onValueChange = { descBn = it },
                        label = { Text("বিস্তারিত বিবরণ (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("project_desc_input"),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = impactBn,
                        onValueChange = { impactBn = it },
                        label = { Text("জনকল্যাণ ও সুফল (বাংলা)") },
                        modifier = Modifier.fillMaxWidth().testTag("project_impact_input"),
                        minLines = 2
                    )

                    Text("আর্কাইভ ও লোকেশন ফিল্ডস:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Moheshkhali" to "মহেশখালী", "Kutubdia" to "কুতুবদিয়া").forEach { (code, bnLabel) ->
                            FilterChip(
                                selected = upazila.equals(code, ignoreCase = true),
                                onClick = { upazila = code },
                                label = { Text(if (language == AppLanguage.BN) bnLabel else code, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = upazila,
                            onValueChange = { upazila = it },
                            label = { Text("উপজেলা (Moheshkhali/Kutubdia)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = union,
                            onValueChange = { union = it },
                            label = { Text("ইউনিয়ন") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("বছর (যেমন: 2024)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pdfUrl,
                        onValueChange = { pdfUrl = it },
                        label = { Text("PDF রিপোর্ট লিঙ্ক") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Before & After Pickers
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("আগে ও পরে তুলনা ছবি (Historical):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray).clickable { 
                                    beforePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    AsyncOrLocalImage(beforeImageUrl.ifBlank { "drawable:asheq_avatar" }, null, Modifier.fillMaxSize(), ContentScale.Crop)
                                }
                                Text("Before (আগে)", fontSize = 10.sp)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray).clickable { 
                                    afterPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    AsyncOrLocalImage(afterImageUrl.ifBlank { "drawable:asheq_avatar" }, null, Modifier.fillMaxSize(), ContentScale.Crop)
                                }
                                Text("After (পরে)", fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val completion = completionStr.toIntOrNull() ?: 0
                            val resolvedBn = categoryBn.ifBlank { "উন্নয়ন" }
                            val resolvedEn = categoryEn.ifBlank { resolvedBn }
                            val updated = project.copy(
                                titleBn = titleBn.ifBlank { "নতুন প্রকল্প" },
                                titleEn = titleEn.ifBlank { "New Project" },
                                categoryBn = resolvedBn,
                                categoryEn = resolvedEn,
                                budgetBn = budgetBn,
                                completionPercentage = completion,
                                descriptionBn = descBn,
                                impactBn = impactBn,
                                imageUrl = imageUrl.ifBlank { "drawable:dev_port" },
                                status = if (completion >= 100) "Completed" else "Ongoing",
                                upazila = upazila,
                                union = union,
                                year = year.toIntOrNull() ?: 2024,
                                beforeImageUrl = beforeImageUrl,
                                afterImageUrl = afterImageUrl,
                                pdfUrl = pdfUrl
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp).testTag("save_project_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "ফায়ারবেসে সেভ করুন" else "Save to Firebase")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeEditDialog(
    language: AppLanguage,
    notice: NoticeFeedItem,
    isNew: Boolean,
    onSave: (NoticeFeedItem) -> Unit,
    onDismiss: () -> Unit
) {
    var titleBn by remember { mutableStateOf(notice.titleBn) }
    var titleEn by remember { mutableStateOf(notice.titleEn) }
    var detailsBn by remember { mutableStateOf(notice.detailsBn) }
    var detailsEn by remember { mutableStateOf(notice.detailsEn) }
    var categoryBn by remember { mutableStateOf(notice.categoryBn) }
    var isUrgent by remember { mutableStateOf(notice.isUrgent) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন নোটিশ প্রকাশ" else "+ Publish Notice")
                    else
                        (if (language == AppLanguage.BN) "✏️ নোটিশ সম্পাদনা" else "✏️ Edit Notice"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("শিরোনাম (বাংলা)") },
                    modifier = Modifier.fillMaxWidth().testTag("notice_title_bn_input")
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryBn,
                    onValueChange = { categoryBn = it },
                    label = { Text("ক্যাটেগরি (যেমন: নৌ-নিরাপত্তা / প্রশাসনিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detailsBn,
                    onValueChange = { detailsBn = it },
                    label = { Text("বিস্তারিত বিবরণ (বাংলা)") },
                    modifier = Modifier.fillMaxWidth().testTag("notice_details_bn_input"),
                    minLines = 3
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isUrgent = !isUrgent }.padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isUrgent, onCheckedChange = { isUrgent = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BN) "🚨 জরুরি / ব্রেকিং নোটিশ হিসেবে দেখান" else "Mark as Urgent / Breaking",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUrgent) Color(0xFFD32F2F) else Color.DarkGray
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = notice.copy(
                                titleBn = titleBn.ifBlank { "বিজ্ঞপ্তি" },
                                titleEn = titleEn.ifBlank { "Notice" },
                                detailsBn = detailsBn,
                                detailsEn = detailsEn.ifBlank { detailsBn },
                                categoryBn = categoryBn.ifBlank { "সাধারণ" },
                                categoryEn = categoryBn,
                                dateBn = "আজ",
                                dateEn = "Today",
                                isUrgent = isUrgent
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.height(44.dp).testTag("save_notice_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "প্রকাশ করুন" else "Publish")
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryItemEditDialog(
    language: AppLanguage,
    galleryItem: GalleryItem,
    isNew: Boolean,
    onSave: (GalleryItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var titleBn by remember { mutableStateOf(galleryItem.titleBn) }
    var titleEn by remember { mutableStateOf(galleryItem.titleEn) }
    var categoryBn by remember { mutableStateOf(galleryItem.categoryBn) }
    var imageUrl by remember { mutableStateOf(galleryItem.imageUrl) }
    var mediaType by remember { mutableStateOf(galleryItem.mediaType) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val localPath = MediaStorageManager.persistCustomImage(context, uri)
            if (localPath != null) {
                imageUrl = localPath
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন ছবি/মিডিয়া যুক্ত করুন" else "+ Add New Media")
                    else
                        (if (language == AppLanguage.BN) "✏️ মিডিয়া সম্পাদনা" else "✏️ Edit Media"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                // Thumbnail & Image Picker
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (language == AppLanguage.BN) "গ্যালারি ছবি" else "Gallery Photo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RoyalBlue900
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                            ) {
                                AsyncOrLocalImage(
                                    imageUrl = imageUrl.ifBlank { "drawable:moheshkhali_banner" },
                                    contentDescription = "Gallery Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp).testTag("pick_gallery_image_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) "গ্যালারি থেকে ছবি যুক্ত করুন" else "Pick Image from Gallery",
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("ছবির শিরোনাম (বাংলা)") },
                    modifier = Modifier.fillMaxWidth().testTag("gallery_title_input")
                )
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryBn,
                    onValueChange = { categoryBn = it },
                    label = { Text("ক্যাটেগরি (যেমন: উন্নয়ন অগ্রযাত্রা / ঐতিহ্য ও পর্যটন)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Media Type Selector (PHOTO vs VIDEO)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = mediaType.equals("PHOTO", ignoreCase = true),
                        onClick = { mediaType = "PHOTO" },
                        label = { Text(if (language == AppLanguage.BN) "📷 ছবি (Photo)" else "📷 Photo") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mediaType.equals("VIDEO", ignoreCase = true),
                        onClick = { mediaType = "VIDEO" },
                        label = { Text(if (language == AppLanguage.BN) "🎥 ভিডিও (Video)" else "🎥 Video") },
                        modifier = Modifier.weight(1f)
                    )
                }

                var videoUrl by remember { mutableStateOf(galleryItem.videoEmbedUrl) }
                if (mediaType.equals("VIDEO", ignoreCase = true)) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("ভিডিও লিঙ্ক (যেমন: https://youtube.com/...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = galleryItem.copy(
                                titleBn = titleBn.ifBlank { "গ্যালারি মিডিয়া" },
                                titleEn = titleEn.ifBlank { titleBn },
                                categoryBn = categoryBn.ifBlank { "ঐতিহ্য ও পর্যটন" },
                                categoryEn = categoryBn,
                                imageUrl = imageUrl.ifBlank { "drawable:moheshkhali_banner" },
                                mediaType = mediaType,
                                videoEmbedUrl = videoUrl,
                                dateBn = "আজ",
                                dateEn = "Today"
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.height(44.dp).testTag("save_gallery_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সেভ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEditDialog(
    language: AppLanguage,
    schedule: DynamicWatercraftSchedule,
    isNew: Boolean,
    onSave: (DynamicWatercraftSchedule) -> Unit,
    onDismiss: () -> Unit
) {
    var routeBn by remember { mutableStateOf(schedule.routeTitleBn) }
    var routeEn by remember { mutableStateOf(schedule.routeTitleEn) }
    var vesselTypeBn by remember { mutableStateOf(schedule.vesselTypeBn) }
    var noticeBn by remember { mutableStateOf(schedule.officialNoticeTextBn) }
    var timesStr by remember { mutableStateOf(schedule.timeSlotsBn.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন নৌযান রুট" else "+ New Vessel Route")
                    else
                        (if (language == AppLanguage.BN) "✏️ রুট সম্পাদনা" else "✏️ Edit Route"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                OutlinedTextField(
                    value = routeBn,
                    onValueChange = { routeBn = it },
                    label = { Text("রুটের নাম (যেমন: কুতুবদিয়া থেকে মগনামা)") },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_route_input")
                )
                OutlinedTextField(
                    value = vesselTypeBn,
                    onValueChange = { vesselTypeBn = it },
                    label = { Text("যানবাহনের ধরন (সি-ট্রাক / স্পিডবোট)") },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_vessel_input")
                )
                OutlinedTextField(
                    value = timesStr,
                    onValueChange = { timesStr = it },
                    label = { Text("সময়সমূহ (কমা দিয়ে লিখুন, যেমন: সকাল ৭:৩০, দুপুর ১২:০০)") },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_times_input")
                )
                OutlinedTextField(
                    value = noticeBn,
                    onValueChange = { noticeBn = it },
                    label = { Text("অফিসিয়াল বার্তা / নোটিশ") },
                    modifier = Modifier.fillMaxWidth().testTag("schedule_notice_input"),
                    minLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val slots = timesStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            val updated = schedule.copy(
                                routeTitleBn = routeBn.ifBlank { "নৌযান রুট" },
                                routeTitleEn = routeEn.ifBlank { routeBn },
                                vesselTypeBn = vesselTypeBn.ifBlank { "সি-ট্রাক" },
                                timeSlotsBn = slots,
                                timeSlotsEn = slots,
                                officialNoticeTextBn = noticeBn
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.height(44.dp).testTag("save_schedule_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সেভ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactEditDialog(
    language: AppLanguage,
    contact: EmergencyContact,
    isNew: Boolean,
    onSave: (EmergencyContact) -> Unit,
    onDismiss: () -> Unit
) {
    var titleBn by remember { mutableStateOf(contact.titleBn) }
    var titleEn by remember { mutableStateOf(contact.titleEn) }
    var category by remember { mutableStateOf(contact.category) }
    var phone by remember { mutableStateOf(contact.phone) }
    var addressBn by remember { mutableStateOf(contact.addressBn) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন জরুরি সেবা" else "+ Add Contact")
                    else
                        (if (language == AppLanguage.BN) "✏️ যোগাযোগ সম্পাদনা" else "✏️ Edit Contact"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("সেবার নাম (যেমন: মহেশখালী থানা)") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_title_input")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("ক্যাটেগরি (POLICE / HOSPITAL / FIRE / COAST_GUARD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর") },
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )
                OutlinedTextField(
                    value = addressBn,
                    onValueChange = { addressBn = it },
                    label = { Text("ঠিকানা / অবস্থান") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = contact.copy(
                                titleBn = titleBn.ifBlank { "জরুরি সেবা" },
                                titleEn = titleEn.ifBlank { titleBn },
                                category = category.ifBlank { "EMERGENCY" },
                                phone = phone,
                                addressBn = addressBn
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.height(44.dp).testTag("save_contact_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সেভ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DonorEditDialog(
    language: AppLanguage,
    donor: BloodDonor,
    isNew: Boolean,
    onSave: (BloodDonor) -> Unit,
    onDismiss: () -> Unit
) {
    var nameBn by remember { mutableStateOf(donor.nameBn) }
    var bloodGroup by remember { mutableStateOf(donor.bloodGroup) }
    var upazila by remember { mutableStateOf(donor.upazila) }
    var areaBn by remember { mutableStateOf(donor.areaBn) }
    var phone by remember { mutableStateOf(donor.phone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.95f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isNew)
                        (if (language == AppLanguage.BN) "+ নতুন রক্তদাতা নিবন্ধন" else "+ Register Donor")
                    else
                        (if (language == AppLanguage.BN) "✏️ রক্তদাতা তথ্য সম্পাদনা" else "✏️ Edit Donor"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )

                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text("রক্তদাতার নাম") },
                    modifier = Modifier.fillMaxWidth().testTag("donor_name_input")
                )
                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("রক্তের গ্রুপ (যেমন: A+, B+, O+, AB+)") },
                    modifier = Modifier.fillMaxWidth().testTag("donor_group_input")
                )
                OutlinedTextField(
                    value = upazila,
                    onValueChange = { upazila = it },
                    label = { Text("উপজেলা (Moheshkhali / Kutubdia)") },
                    modifier = Modifier.fillMaxWidth().testTag("donor_upazila_input")
                )
                OutlinedTextField(
                    value = areaBn,
                    onValueChange = { areaBn = it },
                    label = { Text("এলাকা / ইউনিয়ন") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর") },
                    modifier = Modifier.fillMaxWidth().testTag("donor_phone_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.height(44.dp)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = donor.copy(
                                nameBn = nameBn.ifBlank { "রক্তদাতা" },
                                nameEn = nameBn,
                                bloodGroup = bloodGroup.ifBlank { "O+" },
                                upazila = upazila.ifBlank { "Moheshkhali" },
                                areaBn = areaBn,
                                areaEn = areaBn,
                                phone = phone,
                                isAvailable = true
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.height(44.dp).testTag("save_donor_submit_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// SECTION 9: SPECIAL EVENTS & COUNTDOWNS
// -------------------------------------------------------------------------------------------------
@Composable
private fun SpecialEventsAdminSection(
    language: AppLanguage,
    events: List<SpecialEventItem>,
    onAddNew: () -> Unit,
    onEdit: (SpecialEventItem) -> Unit,
    onDelete: (SpecialEventItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "দিনপঞ্জি ও বিশেষ দিবস" else "Special Days & Events",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "মোট ${events.size} টি বিশেষ দিবস নথিভুক্ত" else "Total ${events.size} events registered",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp).testTag("add_new_special_event_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (language == AppLanguage.BN) "নতুন দিবস" else "+ Add Event",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো বিশেষ দিবস পাওয়া যায়নি" else "No special events found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("special_event_item_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (item.categoryBn == "জাতীয় দিবস" || item.categoryEn == "National Day") Color(0xFFE8F5E9) else Color(0xFFE0F2FE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (item.categoryBn == "জাতীয় দিবস" || item.categoryEn == "National Day")
                                            (if (language == AppLanguage.BN) "জাতীয় দিবস" else "National Day")
                                        else
                                            (if (language == AppLanguage.BN) "স্থানীয় উন্নয়ন দিবস" else "Local Milestone"),
                                        fontSize = 11.sp,
                                        color = if (item.categoryBn == "জাতীয় দিবস" || item.categoryEn == "National Day") ForestGreen else RoyalBlue800,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = if (language == AppLanguage.BN) item.dateDisplayBn else item.dateDisplayEn,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF006A4E)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            if (item.descriptionBn.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) item.descriptionBn else item.descriptionEn,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A5568)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFEDF2F7))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onEdit(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                    border = BorderStroke(1.dp, RoyalBlue800),
                                    modifier = Modifier.height(40.dp).testTag("edit_special_event_${item.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "এডিট" else "Edit", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { onDelete(item) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    modifier = Modifier.height(40.dp).testTag("delete_special_event_${item.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "মুছুন" else "Delete", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentsAdminSection(
    language: AppLanguage,
    requests: List<AppointmentRequest>,
    onEdit: (AppointmentRequest) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Text(
            text = if (language == AppLanguage.BN) "আবেদন ও সাক্ষাতের অনুরোধসমূহ" else "Appointment Requests",
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
        )
        Text(
            text = if (language == AppLanguage.BN) "মোট ${requests.size} টি আবেদন প্রাপ্ত" else "Total ${requests.size} requests received",
            fontSize = 12.sp, color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (language == AppLanguage.BN) "কোনো আবেদন নেই" else "No requests found", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(requests) { request ->
                    Card(
                        onClick = { onEdit(request) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(request.trackingId, fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 14.sp)
                                Surface(
                                    color = when(request.status) {
                                        "PENDING" -> Color.Gray
                                        "REVIEWING" -> Color(0xFF0284C7)
                                        "SCHEDULED" -> ForestGreen
                                        "COMPLETED" -> ForestGreen
                                        "CANCELLED" -> Color.Red
                                        else -> Color.Gray
                                    }.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        request.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(request.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${request.category} • ${request.phone}", fontSize = 12.sp, color = Color.Gray)
                            if (request.scheduledDate.isNotBlank()) {
                                Text("Scheduled: ${request.scheduledDate}", fontWeight = FontWeight.Bold, color = RoyalBlue900, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentEditDialog(
    language: AppLanguage,
    request: AppointmentRequest,
    onSave: (status: String, note: String, date: String) -> Unit,
    onDismiss: () -> Unit
) {
    var status by remember { mutableStateOf(request.status) }
    var note by remember { mutableStateOf(request.adminNote) }
    var date by remember { mutableStateOf(request.scheduledDate) }

    val statuses = listOf("Pending", "Under Review", "REVIEWING", "SCHEDULED", "In Progress", "COMPLETED", "Resolved", "CANCELLED")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (language == AppLanguage.BN) "আবেদন পর্যালোচনা" else "Review Application", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                
                Text("${if (language == AppLanguage.BN) "আবেদনকারী:" else "Applicant:"} ${request.name}", fontSize = 14.sp)
                Text("${if (language == AppLanguage.BN) "বার্তা:" else "Message:"} ${request.message}", fontSize = 13.sp, color = Color.Gray)

                Text(if (language == AppLanguage.BN) "স্ট্যাটাস পরিবর্তন:" else "Change Status", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(statuses) { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = date, onValueChange = { date = it },
                    label = { Text(if (language == AppLanguage.BN) "সাক্ষাতের সময় (যদি থাকে)" else "Scheduled Time (if any)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(if (language == AppLanguage.BN) "এডমিন নোট" else "Admin Note") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = { onSave(status, note, date) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900)
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialMediaAdminSection(
    language: AppLanguage,
    links: List<SocialMediaLink>,
    onAddNew: () -> Unit,
    onEdit: (SocialMediaLink) -> Unit,
    onDelete: (SocialMediaLink) -> Unit
) {
    val context = LocalContext.current
    fun launchLink(url: String) {
        com.example.util.IntentUtils.safeOpenUrl(context, url, language)
    }

    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (language == AppLanguage.BN) "সোস্যাল মিডিয়া ব্যবস্থাপনা" else "Social Media Management",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "মোট লিঙ্ক: ${links.size}টি" else "Total Links: ${links.size}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Button(
                onClick = onAddNew,
                colors = ButtonDefaults.buttonColors(containerColor = FlagGreen700),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (language == AppLanguage.BN) "নতুন লিঙ্ক" else "Add New")
            }
        }

        if (links.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (language == AppLanguage.BN) "কোনো লিঙ্ক পাওয়া যায়নি" else "No links found",
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(links, key = { it.id }) { link ->
                    SocialMediaCard(
                        link = link,
                        isAdmin = true,
                        language = language,
                        onEdit = onEdit,
                        onDelete = { onDelete(link) },
                        onClick = { launchLink(link.url) }
                    )
                }
            }
        }
    }
}



