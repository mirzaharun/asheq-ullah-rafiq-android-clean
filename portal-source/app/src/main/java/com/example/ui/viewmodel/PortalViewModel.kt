package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.DefaultPortalContent
import com.example.data.repository.PortalRepository
import com.example.util.InAppNotificationHelper
import com.example.util.NotificationSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.*

sealed interface ActiveModal {
    object None : ActiveModal
    object WeatherModal : ActiveModal
    object SeaTruckModal : ActiveModal
    object EmergencyModal : ActiveModal
    object BloodDonorsModal : ActiveModal
    object TourismPortalModal : ActiveModal
    data class TourismSpotDetailModal(val spot: TourismSpot) : ActiveModal
    data class ProjectDetailModal(val project: DevelopmentProject) : ActiveModal
    object NoticesModal : ActiveModal
    object AdminAuthModal : ActiveModal
    object AdminPanelModal : ActiveModal
    object AddDonorModal : ActiveModal
    object ContactFeedbackSuccessModal : ActiveModal
    object AIAssistantModal : ActiveModal
    object UnionHubModal : ActiveModal
    object AboutModal : ActiveModal
    object PrivacyModal : ActiveModal
    object TermsModal : ActiveModal
    object FeedbackModal : ActiveModal
    object CitizenServicesModal : ActiveModal
    object EconomyModal : ActiveModal
    object EducationModal : ActiveModal
    object CitizenPollModal : ActiveModal
    object WelfareModal : ActiveModal
    object DevAnalyticsModal : ActiveModal
}

class PortalViewModel(
    private val repository: PortalRepository? = null
) : ViewModel() {

    private val _language = MutableStateFlow(AppLanguage.BN)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _currentTabIndex = MutableStateFlow(0)
    val currentTabIndex: StateFlow<Int> = _currentTabIndex.asStateFlow()

    private val _activeModal = MutableStateFlow<ActiveModal>(ActiveModal.None)
    val activeModal: StateFlow<ActiveModal> = _activeModal.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _adminRole = MutableStateFlow<String?>(null)
    val adminRole: StateFlow<String?> = _adminRole.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _readNotificationIds = MutableStateFlow(
        repository?.getAppContext()?.let {
            InAppNotificationHelper.loadReadIds(it, null)
        } ?: emptySet()
    )
    val readNotificationIds: StateFlow<Set<String>> =
        _readNotificationIds.asStateFlow()

    private val _noticesNotificationEnabled = MutableStateFlow(
        repository?.getAppContext()?.let {
            InAppNotificationHelper.isNoticesAlertEnabled(it)
        } ?: true
    )
    val noticesNotificationEnabled: StateFlow<Boolean> =
        _noticesNotificationEnabled.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        try {
            val auth = FirebaseAuth.getInstance()
            firebaseAuth = auth

            val listener = FirebaseAuth.AuthStateListener { firebase ->
                handleAuthState(firebase.currentUser)
            }

            authStateListener = listener
            auth.addAuthStateListener(listener)

            handleAuthState(auth.currentUser)

        } catch (exception: Exception) {
            _isAdminAuthenticated.value = false
            _adminRole.value = null
            _currentUserId.value = null
            _currentUserEmail.value = null
        }
    }

    private fun handleAuthState(user: FirebaseUser?) {
        val appCtx = repository?.getAppContext()

        if (user == null) {
            _currentUserId.value = null
            _currentUserEmail.value = null
            _isAdminAuthenticated.value = false
            _adminRole.value = null

            _readNotificationIds.value =
                appCtx?.let {
                    InAppNotificationHelper.loadReadIds(it, null)
                } ?: emptySet()

            repository?.refreshProtectedListeners(null, false, null)

        } else {
            _currentUserId.value = user.uid
            _currentUserEmail.value = user.email

            _readNotificationIds.value =
                appCtx?.let {
                    InAppNotificationHelper.loadReadIds(it, user.uid)
                } ?: emptySet()

            if (repository != null) {
                repository.verifyAdminRole { isAdmin, role ->
                    _isAdminAuthenticated.value = isAdmin
                    _adminRole.value = role

                    repository.refreshProtectedListeners(
                        user.uid,
                        isAdmin,
                        role
                    )
                }
            } else {
                _isAdminAuthenticated.value = false
                _adminRole.value = null
            }
        }
    }

    fun verifyCurrentUserAdmin(
        onResult: (isAdmin: Boolean, role: String?) -> Unit
    ) {
        val repo = repository

        if (repo == null) {
            _isAdminAuthenticated.value = false
            _adminRole.value = null
            onResult(false, null)
            return
        }

        repo.verifyAdminRole { isAdmin, role ->
            _isAdminAuthenticated.value = isAdmin
            _adminRole.value = role

            val uid = try {
                FirebaseAuth.getInstance().currentUser?.uid
            } catch (_: Exception) {
                null
            }

            _currentUserId.value = uid

            repo.refreshProtectedListeners(
                uid,
                isAdmin,
                role
            )

            onResult(isAdmin, role)
        }
    }

    fun logoutAdmin() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {
            // Sign-out state is reset below as a fallback.
        }

        _isAdminAuthenticated.value = false
        _adminRole.value = null
        _currentUserId.value = null
        _currentUserEmail.value = null

        val appCtx = repository?.getAppContext()

        _readNotificationIds.value =
            appCtx?.let {
                InAppNotificationHelper.loadReadIds(it, null)
            } ?: emptySet()

        repository?.refreshProtectedListeners(null, false, null)
    }

    private val _tourismCategoryFilter = MutableStateFlow<String?>(null)
    val tourismCategoryFilter: StateFlow<String?> =
        _tourismCategoryFilter.asStateFlow()

    private val _bloodGroupFilter = MutableStateFlow<String?>(null)
    val bloodGroupFilter: StateFlow<String?> =
        _bloodGroupFilter.asStateFlow()

    private val _donorUpazilaFilter = MutableStateFlow<String?>(null)
    val donorUpazilaFilter: StateFlow<String?> =
        _donorUpazilaFilter.asStateFlow()

    private val _localPollVotes =
        MutableStateFlow(mapOf(0 to 342, 1 to 289, 2 to 415, 3 to 198))

    val pollVotes: StateFlow<Map<Int, Int>> =
        repository?.pollVotes ?: _localPollVotes.asStateFlow()

    private val _localUserVotedOption = MutableStateFlow<Int?>(null)

    val userVotedOption: StateFlow<Int?> =
        repository?.userVotedOption ?: _localUserVotedOption.asStateFlow()

    fun submitPollVote(optionIndex: Int) {
        if (repository != null) {
            repository.submitPollVote(optionIndex)
        } else if (_localUserVotedOption.value == null) {
            _localUserVotedOption.value = optionIndex

            _localPollVotes.update { current ->
                val currentCount = current[optionIndex] ?: 0
                current + (optionIndex to currentCount + 1)
            }
        }
    }

    val appConfig: StateFlow<AppConfig> =
        repository?.appConfig ?: MutableStateFlow(AppConfig())

    val latestAppRelease: StateFlow<AppRelease?> =
        repository?.latestAppRelease ?: MutableStateFlow(null)

    val filterChips: StateFlow<List<FilterChipItem>> =
        repository?.filterChips ?: MutableStateFlow(emptyList())

    val weatherData: StateFlow<WeatherData> =
        repository?.weatherData ?: MutableStateFlow(WeatherData())

    val seaTruckUpdate: StateFlow<SeaTruckUpdate> =
        repository?.seaTruckUpdate ?: MutableStateFlow(SeaTruckUpdate())

    val dynamicWatercraftSchedules: StateFlow<List<DynamicWatercraftSchedule>> =
        repository?.dynamicWatercraftSchedules ?: MutableStateFlow(emptyList())

    val dynamicTideSchedule: StateFlow<DynamicTideSchedule> =
        repository?.dynamicTideSchedule ?: MutableStateFlow(DynamicTideSchedule())

    val dynamicMediaAssets: StateFlow<DynamicMediaAssets> =
        repository?.dynamicMediaAssets ?: MutableStateFlow(DynamicMediaAssets())

    val publicServices: StateFlow<List<PublicServiceItem>> =
        repository?.publicServices ?: MutableStateFlow(emptyList())

    val tourismSpots: StateFlow<List<TourismSpot>> =
        repository?.tourismSpots ?: MutableStateFlow(emptyList())

    val developmentProjects: StateFlow<List<DevelopmentProject>> =
        repository?.developmentProjects ?: MutableStateFlow(emptyList())

    val emergencyContacts: StateFlow<List<EmergencyContact>> =
        repository?.emergencyContacts ?: MutableStateFlow(emptyList())

    val bloodDonors: StateFlow<List<BloodDonor>> =
        repository?.bloodDonors ?: MutableStateFlow(emptyList())

    val notices: StateFlow<List<NoticeFeedItem>> =
        repository?.notices ?: MutableStateFlow(emptyList())

    val galleryItems: StateFlow<List<GalleryItem>> =
        repository?.galleryItems ?: MutableStateFlow(emptyList())

    val mediaArchiveItems: StateFlow<List<MediaArchiveItem>> =
        repository?.mediaArchiveItems ?: MutableStateFlow(emptyList())

    val importantLinks: StateFlow<List<ImportantExternalLink>> =
        repository?.importantLinks ?: MutableStateFlow(emptyList())

    val specialEvents: StateFlow<List<SpecialEventItem>> =
        repository?.specialEvents ?: MutableStateFlow(emptyList())

    val citizenGrievances: StateFlow<List<CitizenGrievance>> =
        repository?.citizenGrievances ?: MutableStateFlow(emptyList())

    val profileInfo: StateFlow<ProfileInfo> =
        repository?.profileInfo ?: MutableStateFlow(ProfileInfo())

    val digitalServices: StateFlow<List<DigitalServiceItem>> =
        repository?.digitalServices
            ?: MutableStateFlow(DefaultPortalContent.defaultDigitalServices)

    val dailySchedules: StateFlow<List<DailyScheduleItem>> =
        repository?.dailySchedules ?: MutableStateFlow(emptyList())

    val appointmentRequests: StateFlow<List<AppointmentRequest>> =
        repository?.appointmentRequests ?: MutableStateFlow(emptyList())

    val govtReports: StateFlow<List<GovtReportItem>> =
        repository?.govtReports ?: MutableStateFlow(emptyList())

    val faqs: StateFlow<List<FaqItem>> =
        repository?.faqs ?: MutableStateFlow(emptyList())

    val unionRepresentatives: StateFlow<List<UnionRepresentative>> =
        repository?.unionRepresentatives ?: MutableStateFlow(emptyList())

    val socialMediaLinks: StateFlow<List<SocialMediaLink>> =
        repository?.socialMediaLinks ?: MutableStateFlow(emptyList())

    val educationResources: StateFlow<List<EducationResourceItem>> =
        repository?.educationResources
            ?: MutableStateFlow(DefaultPortalContent.defaultEducationResources)

    val economyOpportunities: StateFlow<List<EconomyOpportunityItem>> =
        repository?.economyOpportunities
            ?: MutableStateFlow(DefaultPortalContent.defaultEconomyOpportunities)

    val welfarePrograms: StateFlow<List<WelfareProgramItem>> =
        repository?.welfarePrograms
            ?: MutableStateFlow(DefaultPortalContent.defaultWelfarePrograms)

    val unionServices: StateFlow<List<UnionServiceHub>> =
        repository?.unionServices
            ?: MutableStateFlow(DefaultPortalContent.defaultUnionServices)

    val developmentCategories: StateFlow<List<DevelopmentCategoryItem>> =
        repository?.developmentCategories
            ?: MutableStateFlow(DefaultPortalContent.defaultDevelopmentCategories)

    val homepageCategories: StateFlow<List<HomepageCategoryItem>> =
        repository?.homepageCategories
            ?: MutableStateFlow(DefaultPortalContent.defaultHomepageCategories)

    val citizenPoll: StateFlow<CitizenPollItem> =
        repository?.citizenPoll
            ?: MutableStateFlow(DefaultPortalContent.defaultCitizenPoll)

    val filteredBloodDonors: StateFlow<List<BloodDonor>> = combine(
        bloodDonors,
        bloodGroupFilter,
        donorUpazilaFilter
    ) { donors, group, upazila ->
        donors.filter { donor ->
            (group == null || donor.bloodGroup == group) &&
                (upazila == null ||
                    donor.upazila.equals(upazila, ignoreCase = true))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredTourismSpots: StateFlow<List<TourismSpot>> = combine(
        tourismSpots,
        tourismCategoryFilter
    ) { spots, category ->
        if (category == null) {
            spots
        } else {
            spots.filter {
                it.category.equals(category, ignoreCase = true)
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /*
     * Important:
     * Push notification preference is NOT used to filter this list.
     * Published notices remain visible in the in-app notification list
     * even when the user disables notice push alerts.
     */
    val inAppNotifications: StateFlow<List<InAppNotification>> = combine(
        notices,
        appointmentRequests,
        citizenGrievances,
        currentUserId
    ) { noticeList, appList, grievanceList, uid ->
        InAppNotificationHelper.buildNotifications(
            notices = noticeList,
            appointments = appList,
            grievances = grievanceList,
            currentUserId = uid
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    val unreadNotificationCount: StateFlow<Int> = combine(
        inAppNotifications,
        readNotificationIds
    ) { items, readIds ->
        items.count { it.id !in readIds }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        0
    )

    fun markNotificationRead(notificationId: String) {
        val ctx = repository?.getAppContext()
        val uid = _currentUserId.value

        _readNotificationIds.value =
            if (ctx != null) {
                InAppNotificationHelper.markRead(
                    ctx,
                    notificationId,
                    uid
                )
            } else {
                _readNotificationIds.value + notificationId
            }
    }

    fun markAllNotificationsRead() {
        val ids = inAppNotifications.value.map { it.id }
        val ctx = repository?.getAppContext()
        val uid = _currentUserId.value

        _readNotificationIds.value =
            if (ctx != null) {
                InAppNotificationHelper.markAllRead(
                    ctx,
                    ids,
                    uid
                )
            } else {
                _readNotificationIds.value + ids
            }
    }

    /*
     * This preference controls notice PUSH alerts only.
     * It does not hide notices from the in-app notification screen.
     */
    fun setNoticesNotificationEnabled(
        context: Context,
        enabled: Boolean
    ) {
        _noticesNotificationEnabled.value = enabled

        InAppNotificationHelper.setNoticesAlertEnabled(
            context,
            enabled
        )

        NotificationSyncManager.syncNoticePreferences(
            context = context,
            noticesEnabled = enabled,
            language = if (_language.value == AppLanguage.EN) "en" else "bn"
        )
    }

    fun toggleLanguage() {
        _language.update {
            if (it == AppLanguage.BN) AppLanguage.EN else AppLanguage.BN
        }
    }

    fun selectTab(index: Int) {
        _currentTabIndex.value = index
    }

    fun openModal(modal: ActiveModal) {
        _activeModal.value = modal
    }

    fun closeModal() {
        _activeModal.value = ActiveModal.None
    }

    fun setTourismFilter(category: String?) {
        _tourismCategoryFilter.value = category
    }

    fun setBloodGroupFilter(group: String?) {
        _bloodGroupFilter.value = group
    }

    fun setDonorUpazilaFilter(upazila: String?) {
        _donorUpazilaFilter.value = upazila
    }

    fun updateMaritimeDangerSignal(signal: Int) {
        val (descBn, descEn) = when (signal) {
            1 -> "১ নম্বর দূরবর্তী সতর্ক সংকেত" to
                "Distant Cautionary Signal No. 1"

            2 -> "২ নম্বর দূরবর্তী হুঁশিয়ারি সংকেত" to
                "Distant Warning Signal No. 2"

            3 -> "৩ নম্বর স্থানীয় সতর্ক সংকেত" to
                "Local Cautionary Signal No. 3"

            4 -> "৪ নম্বর স্থানীয় হুঁশিয়ারি সংকেত" to
                "Local Warning Signal No. 4"

            else -> "সতর্ক সংকেত নেই (স্বাভাবিক)" to
                "No Warning Signal (Normal)"
        }

        repository?.updateWeatherMaritimeSignal(
            signal,
            descBn,
            descEn
        )
    }

    fun updateSeaTruckServiceStatus(
        isNormal: Boolean,
        customNoticeBn: String,
        customNoticeEn: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateSeaTruckNotice(
            customNoticeBn,
            customNoticeEn,
            isNormal,
            onSuccess,
            onError
        )
    }

    fun registerNewDonor(
        name: String,
        group: String,
        upazila: String,
        area: String,
        phone: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val donor = BloodDonor(
            id = "B_${System.currentTimeMillis()}",
            userId = _currentUserId.value.orEmpty(),
            nameBn = name,
            nameEn = name,
            bloodGroup = group,
            upazila = upazila,
            areaBn = area,
            areaEn = area,
            phone = phone,
            lastDonatedBn = "নতুন নিবন্ধিত",
            lastDonatedEn = "Newly Registered",
            isAvailable = true
        )

        repository?.saveBloodDonor(
            donor,
            onSuccess,
            onError
        )
    }

    fun publishAdminNotice(
        titleBn: String,
        titleEn: String,
        detailsBn: String,
        detailsEn: String,
        isUrgent: Boolean
    ) {
        val notice = NoticeFeedItem(
            id = "N_${System.currentTimeMillis()}",
            titleBn = titleBn,
            titleEn = titleEn,
            dateBn = "আজ",
            dateEn = "Today",
            categoryBn = "প্রশাসনিক বিজ্ঞপ্তি",
            categoryEn = "Admin Notice",
            detailsBn = detailsBn,
            detailsEn = detailsEn,
            isUrgent = isUrgent
        )

        repository?.saveNotice(notice)
    }

    fun submitCitizenGrievance(
        name: String,
        phone: String,
        upazila: String,
        subject: String,
        details: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.submitCitizenGrievance(
            name,
            phone,
            upazila,
            subject,
            details,
            onSuccess,
            onError
        )
    }

    // --- Projects CRUD ---

    fun saveDevelopmentProject(
        project: DevelopmentProject,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveDevelopmentProject(
            project,
            onSuccess,
            onError
        )
    }

    fun deleteDevelopmentProject(
        projectId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteDevelopmentProject(
            projectId,
            onSuccess,
            onError
        )
    }

    // --- Govt Reports CRUD ---

    fun saveGovtReport(
        report: GovtReportItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveGovtReport(
            report,
            onSuccess,
            onError
        )
    }

    fun deleteGovtReport(
        reportId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteGovtReport(
            reportId,
            onSuccess,
            onError
        )
    }

    // --- Gallery CRUD ---

    fun saveGalleryItem(
        item: GalleryItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveGalleryItem(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteGalleryItem(
        galleryId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteGalleryItem(
            galleryId,
            onSuccess,
            onError
        )
    }

    // --- Media Archive Items CRUD ---

    fun saveMediaArchiveItem(
        item: MediaArchiveItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveMediaArchiveItem(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteMediaArchiveItem(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteMediaArchiveItem(
            id,
            onSuccess,
            onError
        )
    }

    // --- Important External Links CRUD ---

    fun saveImportantLink(
        item: ImportantExternalLink,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveImportantLink(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteImportantLink(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteImportantLink(
            id,
            onSuccess,
            onError
        )
    }

    // --- Special Events CRUD ---

    fun saveSpecialEvent(
        item: SpecialEventItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveSpecialEvent(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteSpecialEvent(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteSpecialEvent(
            id,
            onSuccess,
            onError
        )
    }

    // --- Notices CRUD ---

    fun saveNotice(
        notice: NoticeFeedItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveNotice(
            notice,
            onSuccess,
            onError
        )
    }

    fun deleteNotice(
        noticeId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteNotice(
            noticeId,
            onSuccess,
            onError
        )
    }

    // --- Emergency Contacts CRUD ---

    fun saveEmergencyContact(
        contact: EmergencyContact,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveEmergencyContact(
            contact,
            onSuccess,
            onError
        )
    }

    fun deleteEmergencyContact(
        contactId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteEmergencyContact(
            contactId,
            onSuccess,
            onError
        )
    }

    // --- Blood Donors CRUD ---

    fun saveBloodDonor(
        donor: BloodDonor,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveBloodDonor(
            donor,
            onSuccess,
            onError
        )
    }

    fun deleteBloodDonor(
        donorId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteBloodDonor(
            donorId,
            onSuccess,
            onError
        )
    }

    // --- Weather Data CRUD & Read-Only Cloud Sync ---

    fun refreshWeatherFromCloud(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val repo = repository

        if (repo == null) {
            onSuccess()
        } else {
            repo.refreshWeatherFromCloud(
                onSuccess,
                onError
            )
        }
    }

    fun updateWeatherData(
        weather: WeatherData,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateWeatherData(
            weather,
            onSuccess,
            onError
        )
    }

    // --- Watercraft Schedules CRUD ---

    fun saveWatercraftSchedule(
        schedule: DynamicWatercraftSchedule,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveWatercraftSchedule(
            schedule,
            onSuccess,
            onError
        )
    }

    fun deleteWatercraftSchedule(
        scheduleId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteWatercraftSchedule(
            scheduleId,
            onSuccess,
            onError
        )
    }

    fun resetWatercraftSchedulesToDefault(
        onSuccess: () -> Unit = {}
    ) {
        repository?.resetWatercraftSchedulesToDefault(onSuccess)
    }

    // --- Tide Schedule CRUD ---

    fun updateTideSchedule(
        tide: DynamicTideSchedule,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateTideSchedule(
            tide,
            onSuccess,
            onError
        )
    }

    // --- Media Assets CRUD ---

    fun updateMediaAssets(
        mediaAssets: DynamicMediaAssets,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateMediaAssets(
            mediaAssets,
            onSuccess,
            onError
        )
    }

    fun updateAppConfig(
        config: AppConfig,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateAppConfig(
            config,
            onSuccess,
            onError
        )
    }

    fun updateProfileInfo(
        profile: ProfileInfo,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateProfileInfo(
            profile,
            onSuccess,
            onError
        )
    }

    // --- Digital Services CRUD ---

    fun saveDigitalService(
        item: DigitalServiceItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveDigitalService(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteDigitalService(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteDigitalService(
            id,
            onSuccess,
            onError
        )
    }

    // --- Daily Schedule CRUD ---

    fun saveDailySchedule(
        item: DailyScheduleItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveDailySchedule(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteDailySchedule(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteDailySchedule(
            id,
            onSuccess,
            onError
        )
    }

    // --- Appointment Requests ---

    fun submitAppointmentRequest(
        name: String,
        phone: String,
        upazila: String,
        union: String,
        category: String,
        message: String,
        attachmentUrl: String = "",
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val request = AppointmentRequest(
            name = name,
            phone = phone,
            upazila = upazila,
            union = union,
            category = category,
            message = message,
            attachmentUrl = attachmentUrl
        )

        repository?.submitAppointmentRequest(
            request,
            onSuccess,
            onError
        )
    }

    fun checkAppointmentStatus(
        trackingId: String,
        onResult: (AppointmentRequest?) -> Unit
    ) {
        repository?.getAppointmentByTrackingId(
            trackingId,
            onResult
        )
    }

    fun updateAppointmentStatus(
        id: String,
        status: String,
        adminNote: String = "",
        scheduledDate: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.updateAppointmentStatus(
            id,
            status,
            adminNote,
            scheduledDate,
            onSuccess,
            onError
        )
    }

    fun deleteAppointmentRequest(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteAppointmentRequest(
            id,
            onSuccess,
            onError
        )
    }

    // --- FAQ CRUD ---

    fun saveFaq(
        faq: FaqItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveFaq(
            faq,
            onSuccess,
            onError
        )
    }

    fun deleteFaq(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteFaq(
            id,
            onSuccess,
            onError
        )
    }

    // --- General Feedback ---

    fun submitGeneralFeedback(
        name: String,
        phone: String,
        union: String,
        category: String,
        message: String,
        attachmentUrl: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.submitGeneralFeedback(
            name,
            phone,
            union,
            category,
            message,
            attachmentUrl,
            onSuccess,
            onError
        )
    }

    // --- Union Representatives ---

    fun saveUnionRepresentative(
        item: UnionRepresentative,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveUnionRepresentative(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteUnionRepresentative(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteUnionRepresentative(
            id,
            onSuccess,
            onError
        )
    }

    fun refreshTideSchedule() {
        repository?.refreshTideSchedule()
    }

    // --- Social Media Links ---

    fun saveSocialMediaLink(
        item: SocialMediaLink,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.saveSocialMediaLink(
            item,
            onSuccess,
            onError
        )
    }

    fun deleteSocialMediaLink(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        repository?.deleteSocialMediaLink(
            id,
            onSuccess,
            onError
        )
    }

    override fun onCleared() {
        authStateListener?.let { listener ->
            firebaseAuth?.removeAuthStateListener(listener)
        }

        authStateListener = null
        firebaseAuth = null

        super.onCleared()
    }
}
