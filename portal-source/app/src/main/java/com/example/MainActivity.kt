package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import android.widget.Toast
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.data.repository.PortalRepository
import com.example.ui.components.PortalBottomNavigationAndFooter
import com.example.ui.components.PortalTopBar
import com.example.ui.screens.*
import com.example.ui.theme.BackgroundLight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoyalBlue800
import com.example.ui.viewmodel.ActiveModal
import com.example.ui.viewmodel.PortalViewModel
import com.example.util.AuthManager

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.service.PortalFirebaseMessagingService
import com.example.util.NotificationSyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            MyApplicationTheme {
                val activityContext = LocalContext.current
                val context = activityContext.applicationContext
                val scope = rememberCoroutineScope()
                val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
                val viewModel: PortalViewModel = viewModel {
                    PortalViewModel(PortalRepository(context))
                }
                val language by viewModel.language.collectAsStateWithLifecycle()
                val currentTab by viewModel.currentTabIndex.collectAsStateWithLifecycle()
                val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()

                // FCM Push Notification Permission & Initialization (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        NotificationSyncManager.syncToken(context)
                        NotificationSyncManager.syncNoticePreferences(
                            context = context,
                            noticesEnabled = viewModel.noticesNotificationEnabled.value,
                            language = if (language == AppLanguage.EN) "en" else "bn"
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    PortalFirebaseMessagingService.createNotificationChannels(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        NotificationSyncManager.syncToken(context)
                        NotificationSyncManager.syncNoticePreferences(
                            context = context,
                            noticesEnabled = viewModel.noticesNotificationEnabled.value,
                            language = if (language == AppLanguage.EN) "en" else "bn"
                        )
                    }
                }

                val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
                val latestAppRelease by viewModel.latestAppRelease.collectAsStateWithLifecycle()
                val filterChips by viewModel.filterChips.collectAsStateWithLifecycle()
                val weatherData by viewModel.weatherData.collectAsStateWithLifecycle()
                val seaTruckUpdate by viewModel.seaTruckUpdate.collectAsStateWithLifecycle()
                val dynamicWatercraftSchedules by viewModel.dynamicWatercraftSchedules.collectAsStateWithLifecycle()
                val dynamicTideSchedule by viewModel.dynamicTideSchedule.collectAsStateWithLifecycle()
                val dynamicMediaAssets by viewModel.dynamicMediaAssets.collectAsStateWithLifecycle()
                val publicServices by viewModel.publicServices.collectAsStateWithLifecycle()
                val tourismSpots by viewModel.filteredTourismSpots.collectAsStateWithLifecycle()
                val developmentProjects by viewModel.developmentProjects.collectAsStateWithLifecycle()
                val emergencyContacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
                val bloodDonors by viewModel.filteredBloodDonors.collectAsStateWithLifecycle()
                val notices by viewModel.notices.collectAsStateWithLifecycle()
                val inAppNotifications by viewModel.inAppNotifications.collectAsStateWithLifecycle()
                val readNotificationIds by viewModel.readNotificationIds.collectAsStateWithLifecycle()
                val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
                val noticesNotificationEnabled by viewModel.noticesNotificationEnabled.collectAsStateWithLifecycle()
                val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
                val mediaArchiveItems by viewModel.mediaArchiveItems.collectAsStateWithLifecycle()
                val importantLinks by viewModel.importantLinks.collectAsStateWithLifecycle()
                val profileInfo by viewModel.profileInfo.collectAsStateWithLifecycle()
                val digitalServices by viewModel.digitalServices.collectAsStateWithLifecycle()
                val dailySchedules by viewModel.dailySchedules.collectAsStateWithLifecycle()
                val specialEvents by viewModel.specialEvents.collectAsStateWithLifecycle()
                val appointmentRequests by viewModel.appointmentRequests.collectAsStateWithLifecycle()
                val govtReports by viewModel.govtReports.collectAsStateWithLifecycle()
                val faqs by viewModel.faqs.collectAsStateWithLifecycle()
                val unionRepresentatives by viewModel.unionRepresentatives.collectAsStateWithLifecycle()
                val socialMediaLinks by viewModel.socialMediaLinks.collectAsStateWithLifecycle()
                val educationResources by viewModel.educationResources.collectAsStateWithLifecycle()
                val economyOpportunities by viewModel.economyOpportunities.collectAsStateWithLifecycle()
                val welfarePrograms by viewModel.welfarePrograms.collectAsStateWithLifecycle()
                val unionServices by viewModel.unionServices.collectAsStateWithLifecycle()
                val developmentCategories by viewModel.developmentCategories.collectAsStateWithLifecycle()
                val homepageCategories by viewModel.homepageCategories.collectAsStateWithLifecycle()
                val citizenPoll by viewModel.citizenPoll.collectAsStateWithLifecycle()

                val tourismCategoryFilter by viewModel.tourismCategoryFilter.collectAsStateWithLifecycle()
                val bloodGroupFilter by viewModel.bloodGroupFilter.collectAsStateWithLifecycle()
                val donorUpazilaFilter by viewModel.donorUpazilaFilter.collectAsStateWithLifecycle()
                val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()
                val adminRole by viewModel.adminRole.collectAsStateWithLifecycle()
                val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
                val pollVotes by viewModel.pollVotes.collectAsStateWithLifecycle()
                val userVotedOption by viewModel.userVotedOption.collectAsStateWithLifecycle()

                LaunchedEffect(language, currentUserId, noticesNotificationEnabled) {
                    if (!currentUserId.isNullOrBlank()) {
                        NotificationSyncManager.syncToken(context)
                    }
                    NotificationSyncManager.syncNoticePreferences(
                        context = context,
                        noticesEnabled = noticesNotificationEnabled,
                        language = if (language == AppLanguage.EN) "en" else "bn"
                    )
                }

                val canEditCms = isAdminAuthenticated && (adminRole == "super_admin" || adminRole == "editor")

                val performAdminLogout: () -> Unit = {
                    NotificationSyncManager.onLogout(context) {
                        AuthManager.signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                            context = activityContext,
                            onSignOutComplete = { viewModel.logoutAdmin() }
                        )
                    }
                }

                val openAdminAction = {
                    if (isAdminAuthenticated) {
                        viewModel.openModal(ActiveModal.AdminPanelModal)
                    } else {
                        viewModel.openModal(ActiveModal.AdminAuthModal)
                    }
                }

                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        language = language,
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    // BackHandler to handle custom backstack
                    BackHandler(enabled = activeModal != ActiveModal.None || currentTab != 0) {
                        if (activeModal != ActiveModal.None) {
                            viewModel.closeModal()
                        } else if (currentTab != 0) {
                            viewModel.selectTab(0)
                        }
                    }

                    if (activeModal is ActiveModal.SeaTruckModal) {
                        WatercraftScheduleScreen(
                            language = language,
                            weatherData = weatherData,
                            dynamicSchedules = dynamicWatercraftSchedules,
                            dynamicTideSchedule = dynamicTideSchedule,
                            onOpenAdmin = openAdminAction,
                            onRefreshTide = { viewModel.refreshTideSchedule() },
                            onBack = { viewModel.closeModal() }
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = BackgroundLight,
                            topBar = {
                                PortalTopBar(
                                    appConfig = appConfig,
                                    language = language,
                                    showBackButton = (currentTab != 0),
                                    isAdminAuthenticated = isAdminAuthenticated,
                                    unreadNoticeCount = unreadNotificationCount,
                                    onAdminClick = openAdminAction,
                                    onBackClick = { viewModel.selectTab(0) },
                                    onLanguageToggle = { viewModel.toggleLanguage() },
                                    onNotificationsClick = { viewModel.openModal(ActiveModal.NoticesModal) }
                                )
                            },
                            floatingActionButton = {
                                if (isAdminAuthenticated) {
                                    ExtendedFloatingActionButton(
                                        onClick = { viewModel.openModal(ActiveModal.AdminPanelModal) },
                                        containerColor = RoyalBlue800,
                                        contentColor = Color.White,
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = GoldAccent
                                            )
                                        },
                                        text = {
                                            Text(
                                                text = if (language == AppLanguage.BN) "এডমিন পোর্টাল" else "Admin Console",
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        modifier = Modifier.testTag("floating_admin_button")
                                    )
                                }
                            },
                            bottomBar = {
                                PortalBottomNavigationAndFooter(
                                    currentTabIndex = currentTab,
                                    onTabSelected = { viewModel.selectTab(it) },
                                    language = language,
                                    appConfig = appConfig,
                                    onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                    onProfileClick = { viewModel.selectTab(3) },
                                    onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                    onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                    onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                            0 -> HomeScreen(
                                weatherData = weatherData,
                                publicServices = publicServices,
                                digitalServices = digitalServices,
                                dailySchedules = dailySchedules,
                                specialEvents = specialEvents,
                                language = language,
                                mediaAssets = dynamicMediaAssets,
                                appConfig = appConfig,
                                latestAppRelease = latestAppRelease,
                                isAdminAuthenticated = canEditCms,
                                onEditClick = openAdminAction,
                                onSaveConfig = { viewModel.updateAppConfig(it) },
                                onSaveService = { viewModel.saveDigitalService(it) },
                                onDeleteService = { viewModel.deleteDigitalService(it) },
                                onSaveSchedule = { viewModel.saveDailySchedule(it) },
                                onDeleteSchedule = { viewModel.deleteDailySchedule(it) },
                                onSaveSpecialEvent = { viewModel.saveSpecialEvent(it) },
                                onDeleteSpecialEvent = { viewModel.deleteSpecialEvent(it) },
                                onWeatherClick = { viewModel.openModal(ActiveModal.WeatherModal) },
                                onSeaTruckClick = { viewModel.openModal(ActiveModal.SeaTruckModal) },
                                onServiceClick = { serviceId ->
                                    when (serviceId) {
                                        "citizens" -> viewModel.openModal(ActiveModal.CitizenServicesModal)
                                        "union" -> viewModel.openModal(ActiveModal.UnionHubModal)
                                        "emergency" -> viewModel.openModal(ActiveModal.EmergencyModal)
                                        "economy" -> viewModel.openModal(ActiveModal.EconomyModal)
                                        "education" -> viewModel.openModal(ActiveModal.EducationModal)
                                        "poll" -> viewModel.openModal(ActiveModal.CitizenPollModal)
                                        "welfare" -> viewModel.openModal(ActiveModal.WelfareModal)
                                        "analytics" -> viewModel.openModal(ActiveModal.DevAnalyticsModal)
                                        "ai_assistant" -> viewModel.openModal(ActiveModal.AIAssistantModal)
                                        "development" -> viewModel.selectTab(1)
                                        "engagement" -> viewModel.openModal(ActiveModal.CitizenPollModal)
                                        "tourism" -> viewModel.openModal(ActiveModal.TourismPortalModal)
                                        "blood" -> viewModel.openModal(ActiveModal.BloodDonorsModal)
                                    }
                                },
                                onTourismClick = { viewModel.openModal(ActiveModal.TourismPortalModal) },
                                onDecadeDetailClick = { viewModel.selectTab(1) },
                                onNoticeClick = { viewModel.openModal(ActiveModal.NoticesModal) },
                                homepageCategories = homepageCategories,
                                onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                onProfileClick = { viewModel.selectTab(3) },
                                onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                            )
                            1 -> DevelopmentTabScreen(
                                language = language,
                                appConfig = appConfig,
                                projects = developmentProjects,
                                categories = developmentCategories,
                                govtReports = govtReports,
                                isAdminAuthenticated = canEditCms,
                                onAdminAddClick = openAdminAction,
                                onSaveProject = { viewModel.saveDevelopmentProject(it) },
                                onDeleteProject = { viewModel.deleteDevelopmentProject(it) },
                                onSaveReport = { viewModel.saveGovtReport(it) },
                                onDeleteReport = { viewModel.deleteGovtReport(it) },
                                onProjectDetailClick = { project ->
                                    viewModel.openModal(ActiveModal.ProjectDetailModal(project))
                                },
                                onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                onProfileClick = { viewModel.selectTab(3) },
                                onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                            )
                            2 -> GalleryTabScreen(
                                items = galleryItems,
                                mediaArchiveItems = mediaArchiveItems,
                                importantLinks = importantLinks,
                                language = language,
                                isAdminAuthenticated = canEditCms,
                                onAdminLoginClick = { viewModel.openModal(ActiveModal.AdminAuthModal) },
                                onAdminLogoutClick = performAdminLogout,
                                onSaveGalleryItem = { viewModel.saveGalleryItem(it) },
                                onDeleteGalleryItem = { viewModel.deleteGalleryItem(it) },
                                onSaveMediaArchiveItem = { viewModel.saveMediaArchiveItem(it) },
                                onDeleteMediaArchiveItem = { viewModel.deleteMediaArchiveItem(it) },
                                onSaveImportantLink = { viewModel.saveImportantLink(it) },
                                onDeleteImportantLink = { viewModel.deleteImportantLink(it) },
                                appConfig = appConfig,
                                onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                onProfileClick = { viewModel.selectTab(3) },
                                onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                            )
                            3 -> ProfileTabScreen(
                                profileInfo = profileInfo,
                                appConfig = appConfig,
                                language = language,
                                isAdminAuthenticated = isAdminAuthenticated,
                                onAdminClick = openAdminAction,
                                onLogoutAdmin = performAdminLogout,
                                onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                onProfileClick = { viewModel.selectTab(3) },
                                onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                            )
                            4 -> ContactTabScreen(
                                language = language,
                                appConfig = appConfig,
                                faqs = faqs,
                                appointmentRequests = appointmentRequests,
                                unionRepresentatives = unionRepresentatives,
                                isAdminAuthenticated = isAdminAuthenticated,
                                adminRole = adminRole,
                                currentUserId = currentUserId,
                                onSaveConfig = { viewModel.updateAppConfig(it) },
                                onSubmitAppointment = { name, phone, upazila, union, cat, msg, file, onSuccess, onError ->
                                    viewModel.submitAppointmentRequest(
                                        name = name,
                                        phone = phone,
                                        upazila = upazila,
                                        union = union,
                                        category = cat,
                                        message = msg,
                                        attachmentUrl = file,
                                        onSuccess = onSuccess,
                                        onError = onError
                                    )
                                },
                                onCheckStatus = { id, onResult ->
                                    viewModel.checkAppointmentStatus(id, onResult)
                                },
                                onUpdateAppointment = { id, status, note, date ->
                                    viewModel.updateAppointmentStatus(
                                        id = id,
                                        status = status,
                                        adminNote = note,
                                        scheduledDate = date,
                                        onError = { err ->
                                            Toast.makeText(activityContext, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onDeleteAppointment = { id ->
                                    viewModel.deleteAppointmentRequest(
                                        id = id,
                                        onError = { err ->
                                            Toast.makeText(activityContext, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                onSaveFaq = { item ->
                                    viewModel.saveFaq(item)
                                },
                                onDeleteFaq = { id ->
                                    viewModel.deleteFaq(id)
                                },
                                onSaveUnionRepresentative = { item ->
                                    viewModel.saveUnionRepresentative(item)
                                },
                                onDeleteUnionRepresentative = { id ->
                                    viewModel.deleteUnionRepresentative(id)
                                },
                                socialMediaLinks = socialMediaLinks,
                                onSaveSocialMediaLink = { viewModel.saveSocialMediaLink(it) },
                                onDeleteSocialMediaLink = { viewModel.deleteSocialMediaLink(it) },
                                onAboutClick = { viewModel.openModal(ActiveModal.AboutModal) },
                                onProfileClick = { viewModel.selectTab(3) },
                                onPolicyClick = { viewModel.openModal(ActiveModal.PrivacyModal) },
                                onFeedbackClick = { viewModel.openModal(ActiveModal.FeedbackModal) },
                                onTermsClick = { viewModel.openModal(ActiveModal.TermsModal) }
                            )

                        }

                        // Modal Sheets & Dialogs
                        when (val modal = activeModal) {
                            is ActiveModal.TourismPortalModal -> {
                                TourismPortalSheet(
                                    spots = tourismSpots,
                                    selectedCategory = tourismCategoryFilter,
                                    language = language,
                                    onCategorySelected = { viewModel.setTourismFilter(it) },
                                    onSpotSelected = { spot ->
                                        viewModel.openModal(ActiveModal.TourismSpotDetailModal(spot))
                                    },
                                    onClose = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.TourismSpotDetailModal -> {
                                TourismSpotDetailDialog(
                                    spot = modal.spot,
                                    language = language,
                                    onDismiss = { viewModel.openModal(ActiveModal.TourismPortalModal) }
                                )
                            }
                            is ActiveModal.WeatherModal -> {
                                WeatherDetailDialog(
                                    weather = weatherData,
                                    language = language,
                                    onRefreshWeather = { onSuccess, onError ->
                                        viewModel.refreshWeatherFromCloud(onSuccess, onError)
                                    },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }

                            is ActiveModal.EmergencyModal -> {
                                EmergencyContactsDialog(
                                    contacts = emergencyContacts,
                                    language = language,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.BloodDonorsModal -> {
                                BloodDonorsDialog(
                                    donors = bloodDonors,
                                    selectedGroup = bloodGroupFilter,
                                    selectedUpazila = donorUpazilaFilter,
                                    language = language,
                                    onGroupSelected = { viewModel.setBloodGroupFilter(it) },
                                    onUpazilaSelected = { viewModel.setDonorUpazilaFilter(it) },
                                    onRegisterClick = { viewModel.openModal(ActiveModal.AddDonorModal) },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.AddDonorModal -> {
                                RegisterDonorDialog(
                                    language = language,
                                    currentUserId = currentUserId,
                                    onSave = { name, group, upazila, area, phone ->
                                        viewModel.registerNewDonor(
                                            name = name,
                                            group = group,
                                            upazila = upazila,
                                            area = area,
                                            phone = phone,
                                            onSuccess = {
                                                Toast.makeText(
                                                    activityContext,
                                                    if (language == AppLanguage.BN) "রক্তদাতা নিবন্ধন সফল হয়েছে!" else "Blood donor registered!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                viewModel.openModal(ActiveModal.BloodDonorsModal)
                                            },
                                            onError = { err ->
                                                Toast.makeText(activityContext, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    onDismiss = { viewModel.openModal(ActiveModal.BloodDonorsModal) }
                                )
                            }
                            is ActiveModal.AdminAuthModal -> {
                                AdminLoginDialog(
                                    language = language,
                                    onVerifyAdminRole = { onResult -> viewModel.verifyCurrentUserAdmin(onResult) },
                                    onSuccess = {
                                        viewModel.openModal(ActiveModal.AdminPanelModal)
                                    },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.AdminPanelModal -> {
                                AdminManagementDialog(
                                    language = language,
                                    currentSignal = weatherData.maritimeSignal,
                                    developmentProjects = developmentProjects,
                                    notices = notices,
                                    galleryItems = galleryItems,
                                    dynamicSchedules = dynamicWatercraftSchedules,
                                    emergencyContacts = emergencyContacts,
                                    bloodDonors = bloodDonors,
                                    specialEvents = specialEvents,
                                    appointmentRequests = appointmentRequests,
                                    dynamicTide = dynamicTideSchedule,
                                    dynamicMedia = dynamicMediaAssets,
                                    weatherData = weatherData,
                                    onSaveProject = { viewModel.saveDevelopmentProject(it) },
                                    onDeleteProject = { viewModel.deleteDevelopmentProject(it) },
                                    onSaveNotice = { viewModel.saveNotice(it) },
                                    onDeleteNotice = { viewModel.deleteNotice(it) },
                                    onSaveGalleryItem = { viewModel.saveGalleryItem(it) },
                                    onDeleteGalleryItem = { viewModel.deleteGalleryItem(it) },
                                    onSaveSchedule = { viewModel.saveWatercraftSchedule(it) },
                                    onDeleteSchedule = { viewModel.deleteWatercraftSchedule(it) },
                                    onResetSchedules = { viewModel.resetWatercraftSchedulesToDefault() },
                                    onSaveEmergencyContact = { viewModel.saveEmergencyContact(it) },
                                    onDeleteEmergencyContact = { viewModel.deleteEmergencyContact(it) },
                                    onSaveBloodDonor = { viewModel.saveBloodDonor(it) },
                                    onDeleteBloodDonor = { viewModel.deleteBloodDonor(it) },
                                    onSaveSpecialEvent = { viewModel.saveSpecialEvent(it) },
                                    onDeleteSpecialEvent = { viewModel.deleteSpecialEvent(it) },
                                    onUpdateAppointment = { id, status, note, date -> viewModel.updateAppointmentStatus(id, status, note, date) },
                                    onUpdateTide = { viewModel.updateTideSchedule(it) },
                                    onUpdateMedia = { viewModel.updateMediaAssets(it) },
                                    onUpdateSignal = { sig -> viewModel.updateMaritimeDangerSignal(sig) },
                                    onUpdateWeather = { viewModel.updateWeatherData(it) },
                                    socialMediaLinks = socialMediaLinks,
                                    onSaveSocialMediaLink = { viewModel.saveSocialMediaLink(it) },
                                    onDeleteSocialMediaLink = { viewModel.deleteSocialMediaLink(it) },
                                    adminRole = adminRole,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.ProjectDetailModal -> {
                                ProjectDetailDialog(
                                    project = modal.project,
                                    language = language,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.NoticesModal -> {
                                NoticesModalDialog(
                                    notifications = inAppNotifications,
                                    readNotificationIds = readNotificationIds,
                                    noticesAlertsEnabled = noticesNotificationEnabled,
                                    currentUserId = currentUserId,
                                    language = language,
                                    onMarkRead = { id -> viewModel.markNotificationRead(id) },
                                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                    onToggleNoticeAlerts = { enabled ->
                                        viewModel.setNoticesNotificationEnabled(context, enabled)
                                    },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.AIAssistantModal -> {
                                AIAssistantDialog(
                                    language = language,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.UnionHubModal -> {
                                UnionHubDialog(
                                    language = language,
                                    unions = unionServices,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.AboutModal -> {
                                AboutAppDialog(
                                    language = language,
                                    appConfig = appConfig,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.PrivacyModal -> {
                                PrivacyPolicyDialog(
                                    language = language,
                                    appConfig = appConfig,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.TermsModal -> {
                                TermsOfServiceDialog(
                                    language = language,
                                    appConfig = appConfig,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.FeedbackModal -> {
                                var isSubmittingFeedback by remember { mutableStateOf(false) }
                                GeneralFeedbackDialog(
                                    language = language,
                                    currentUserId = currentUserId,
                                    isSubmitting = isSubmittingFeedback,
                                    onSubmit = { n, p, u, c, m, a ->
                                        isSubmittingFeedback = true
                                        viewModel.submitGeneralFeedback(
                                            name = n,
                                            phone = p,
                                            union = u,
                                            category = c,
                                            message = m,
                                            attachmentUrl = a,
                                            onSuccess = {
                                                isSubmittingFeedback = false
                                                Toast.makeText(
                                                    activityContext,
                                                    if (language == AppLanguage.BN) "আপনার মতামত সফলভাবে জমা হয়েছে!" else "Feedback submitted successfully!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                viewModel.closeModal()
                                            },
                                            onError = { err ->
                                                isSubmittingFeedback = false
                                                Toast.makeText(activityContext, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.CitizenServicesModal -> {
                                CitizenServicesDialog(
                                    language = language,
                                    services = digitalServices,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.EconomyModal -> {
                                EconomyModalDialog(
                                    language = language,
                                    opportunities = economyOpportunities,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.EducationModal -> {
                                EducationModalDialog(
                                    language = language,
                                    resources = educationResources,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.CitizenPollModal -> {
                                CitizenPollDialog(
                                    language = language,
                                    pollVotes = pollVotes,
                                    userVotedOption = userVotedOption,
                                    currentUserId = currentUserId,
                                    activePoll = citizenPoll,
                                    onVote = { viewModel.submitPollVote(it) },
                                    onSubmitVoice = { n, p, u, m, onSuccess, onError ->
                                        viewModel.submitGeneralFeedback(
                                            name = n,
                                            phone = p,
                                            union = u,
                                            category = "জনমত ও পরামর্শ",
                                            message = m,
                                            onSuccess = onSuccess,
                                            onError = onError
                                        )
                                    },
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.WelfareModal -> {
                                WelfareModalDialog(
                                    language = language,
                                    programs = welfarePrograms,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            is ActiveModal.DevAnalyticsModal -> {
                                DevAnalyticsDialog(
                                    language = language,
                                    onDismiss = { viewModel.closeModal() }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
}
}

