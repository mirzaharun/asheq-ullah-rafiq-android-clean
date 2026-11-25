package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.util.MediaStorageManager

private fun getConfiguredFirestore(context: Context): FirebaseFirestore {
    val databaseId = context.applicationContext
        .getString(R.string.firestore_database_id)
        .trim()
    require(databaseId.isNotBlank() && databaseId != "(default)") {
        "A named Firestore database ID must be configured for this app."
    }
    return FirebaseFirestore.getInstance(databaseId)
}

class PortalRepository(
    private val firestore: FirebaseFirestore? = null,
    private val context: Context? = null
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val ADMIN_ROLES = setOf("super_admin", "editor", "application_manager")
        private val CONTENT_ADMIN_ROLES = setOf("super_admin", "editor")
        private val APP_MANAGER_ROLES = setOf("super_admin", "application_manager")
    }

    @Volatile
    private var currentAuthUid: String? = null
    @Volatile
    private var currentAdminRole: String? = null
    @Volatile
    private var canEditCmsContent: Boolean = false
    @Volatile
    private var canManageApplications: Boolean = false

    private var applicationsListener: ListenerRegistration? = null
    private var appointmentsListener: ListenerRegistration? = null
    private var grievancesListener: ListenerRegistration? = null
    private var pollVoteListener: ListenerRegistration? = null

    fun getAppContext(): Context? = context?.applicationContext

    constructor(context: Context) : this(
        getConfiguredFirestore(context),
        context.applicationContext
    )

    private val auth by lazy {
        try {
            Firebase.auth
        } catch (e: Throwable) {
            Log.w("PortalRepository", "FirebaseAuth not initialized: ${e.message}")
            null
        }
    }

    // 1. App Configuration & Branding
    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _latestAppRelease = MutableStateFlow<AppRelease?>(null)
    val latestAppRelease: StateFlow<AppRelease?> = _latestAppRelease.asStateFlow()

    // 2. Dynamic Filter Chips
    private val _filterChips = MutableStateFlow(
        listOf(
            FilterChipItem("all", "সবকিছু", "All", "all", 0, true),
            FilterChipItem("mega", "বড় প্রকল্প", "Mega Projects", "বড় প্রকল্প", 1, true),
            FilterChipItem("road", "সড়ক ও যোগাযোগ", "Roads & Transport", "সড়ক ও যোগাযোগ", 2, true),
            FilterChipItem("power", "বিদ্যুৎ ও জ্বালানি", "Power & Energy", "বিদ্যুৎ ও জ্বালানি", 3, true),
            FilterChipItem("edu", "শিক্ষা ও স্বাস্থ্য", "Education & Health", "শিক্ষা ও স্বাস্থ্য", 4, true)
        )
    )
    val filterChips: StateFlow<List<FilterChipItem>> = _filterChips.asStateFlow()

    // 3. Weather
    private val _weatherData = MutableStateFlow(WeatherData())
    val weatherData: StateFlow<WeatherData> = _weatherData.asStateFlow()

    // 4. Sea Truck Update
    private val _seaTruckUpdate = MutableStateFlow(
        SeaTruckUpdate(
            generalStatusBn = "চলাচল স্বাভাবিক ও সময়সূচী অনুযায়ী",
            generalStatusEn = "Service Operational & on schedule",
            noticeBn = "ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা মেনে চলুন।",
            noticeEn = "Follow security guidance at jetties during low tide.",
            routes = listOf(
                SeaTruckRoute("R1", "কক্সবাজার (৬ নং ঘাট)", "Cox's Bazar (Ghat No. 6)", "মহেশখালী (গোরকঘাটা জেটি)", "Moheshkhali (Gorakghata)", "এসটি শহীদ আব্দুর রব", "ST Shaheed Abdur Rob", "সকাল ০৭:০০, ০৯:৩০, দুপুর ১২:০০, বিকাল ০৩:৩০", "07:00 AM, 09:30 AM, 12:00 PM, 03:30 PM", 45, "নিয়মিত চলছে", "Operating normally"),
                SeaTruckRoute("R2", "মগনামা ঘাট (পেকুয়া)", "Magnama Ghat (Pekua)", "কুতুবদিয়া (দরবার ঘাট)", "Kutubdia (Darbar Ghat)", "এসটি কুতুবদিয়া এক্সপ্রেস", "ST Kutubdia Express", "সকাল ০৮:০০, ১০:৩০, দুপুর ০১:০০, বিকাল ০৪:০০", "08:00 AM, 10:30 AM, 01:00 PM, 04:00 PM", 50, "নিয়মিত চলছে", "Operating normally"),
                SeaTruckRoute("R3", "কক্সবাজার (কস্তুরী ঘাট)", "Cox's Bazar (Kasturi Ghat)", "মহেশখালী (স্পিডবোট রুট)", "Moheshkhali (Speedboat)", "দ্রুতগামী স্পিডবোট বহর", "Speedboat Fleet", "প্রতি ১৫ মিনিট অন্তর", "Every 15 mins", 110, "চলমান", "Active")
            )
        )
    )
    val seaTruckUpdate: StateFlow<SeaTruckUpdate> = _seaTruckUpdate.asStateFlow()

    // 4.1 Dynamic Watercraft Schedules
    val defaultWatercraftSchedules = listOf(
        DynamicWatercraftSchedule(
            id = "KUT_TO_MOG",
            routeGroup = "KUTUBDIA_MOGNAMA",
            routeTitleBn = "কুতুবদিয়া থেকে মগনামা",
            routeTitleEn = "Kutubdia to Mognama",
            isOriginDeparture = true,
            officialNoticeTextBn = "কুতুবদিয়া থেকে মগনামা: সকাল ৭:৩০ মিনিট, দুপুর ১২:০০ মিনিট, বিকাল ৫:০০ মিনিট।",
            officialNoticeTextEn = "Kutubdia to Mognama: 07:30 AM, 12:00 PM, 05:00 PM.",
            timeSlotsBn = listOf("সকাল ৭:৩০ মিনিট", "দুপুর ১২:০০ মিনিট", "বিকাল ৫:০০ মিনিট"),
            timeSlotsEn = listOf("07:30 AM", "12:00 PM", "05:00 PM"),
            vesselTypeBn = "সি-ট্রাক",
            vesselTypeEn = "Sea-Truck",
            order = 1
        ),
        DynamicWatercraftSchedule(
            id = "MOG_TO_KUT",
            routeGroup = "KUTUBDIA_MOGNAMA",
            routeTitleBn = "মগনামা থেকে কুতুবদিয়া",
            routeTitleEn = "Mognama to Kutubdia",
            isOriginDeparture = false,
            officialNoticeTextBn = "মগনামা থেকে কুতুবদিয়া: সকাল ৯:০০ মিনিট, দুপুর ২:০০ মিনিট, সন্ধ্যা ৭:০০ মিনিট।",
            officialNoticeTextEn = "Mognama to Kutubdia: 09:00 AM, 02:00 PM, 07:00 PM.",
            timeSlotsBn = listOf("সকাল ৯:০০ মিনিট", "দুপুর ২:০০ মিনিট", "সন্ধ্যা ৭:০০ মিনিট"),
            timeSlotsEn = listOf("09:00 AM", "02:00 PM", "07:00 PM"),
            vesselTypeBn = "সি-ট্রাক",
            vesselTypeEn = "Sea-Truck",
            order = 2
        ),
        DynamicWatercraftSchedule(
            id = "MAH_TO_COX",
            routeGroup = "MAHESHKHALI_COXSBAZAR",
            routeTitleBn = "মহেশখালী হইতে কক্সবাজার",
            routeTitleEn = "Maheshkhali to Cox's Bazar",
            isOriginDeparture = true,
            officialNoticeTextBn = "মহেশখালী হইতে কক্সবাজার: সকাল ৭:০০ মিনিট, সকাল ০৯:৪৫ মিনিট, সকাল ১১:০০ মিনিট, দুপুর ২:০০ মিনিট, বিকাল ৫:০০ টা, রাত ৭:০০ মিনিট।",
            officialNoticeTextEn = "Maheshkhali to Cox's Bazar: 07:00 AM, 09:45 AM, 11:00 AM, 02:00 PM, 05:00 PM, 07:00 PM.",
            timeSlotsBn = listOf("সকাল ৭:০০ মিনিট", "সকাল ০৯:৪৫ মিনিট", "সকাল ১১:০০ মিনিট", "দুপুর ২:০০ মিনিট", "বিকাল ৫:০০ টা", "রাত ৭:০০ মিনিট"),
            timeSlotsEn = listOf("07:00 AM", "09:45 AM", "11:00 AM", "02:00 PM", "05:00 PM", "07:00 PM"),
            vesselTypeBn = "স্পিডবোট",
            vesselTypeEn = "Speedboat",
            order = 3
        ),
        DynamicWatercraftSchedule(
            id = "COX_TO_MAH",
            routeGroup = "MAHESHKHALI_COXSBAZAR",
            routeTitleBn = "কক্সবাজার হইতে মহেশখালী",
            routeTitleEn = "Cox's Bazar to Maheshkhali",
            isOriginDeparture = false,
            officialNoticeTextBn = "কক্সবাজার হইতে মহেশখালী: সকাল ৮:০০ মিনিট, সকাল ১০:০০ মিনিট, দুপুর ১২:০০ মিনিট, বিকাল ৩:০০ মিনিট, সন্ধ্যা ৬:০০ টা, রাত ৯:০০ মিনিট।",
            officialNoticeTextEn = "Cox's Bazar to Maheshkhali: 08:00 AM, 10:00 AM, 12:00 PM, 03:00 PM, 06:00 PM, 09:00 PM.",
            timeSlotsBn = listOf("সকাল ৮:০০ মিনিট", "সকাল ১০:০০ মিনিট", "দুপুর ১২:০০ মিনিট", "বিকাল ৩:০০ মিনিট", "সন্ধ্যা ৬:০০ টা", "রাত ৯:০০ মিনিট"),
            timeSlotsEn = listOf("08:00 AM", "10:00 AM", "12:00 PM", "03:00 PM", "06:00 PM", "09:00 PM"),
            vesselTypeBn = "স্পিডবোট",
            vesselTypeEn = "Speedboat",
            order = 4
        )
    )

    private val _dynamicWatercraftSchedules = MutableStateFlow(defaultWatercraftSchedules)
    val dynamicWatercraftSchedules: StateFlow<List<DynamicWatercraftSchedule>> = _dynamicWatercraftSchedules.asStateFlow()

    // 4.2 Dynamic Tide Schedule
    private val _dynamicTideSchedule = MutableStateFlow(DynamicTideSchedule())
    val dynamicTideSchedule: StateFlow<DynamicTideSchedule> = _dynamicTideSchedule.asStateFlow()

    // 4.3 Dynamic Media Assets
    private val _dynamicMediaAssets = MutableStateFlow(DynamicMediaAssets())
    val dynamicMediaAssets: StateFlow<DynamicMediaAssets> = _dynamicMediaAssets.asStateFlow()

    // 5. Public Services
    private val _publicServices = MutableStateFlow(
        listOf(
            PublicServiceItem("emergency", "জরুরি সেবা", "Emergency", "EMERGENCY", "পুলিশ, হাসপাতাল, কোস্টগার্ড", "Police, Hospital, Coast Guard", 0xFFD32F2F),
            PublicServiceItem("blood", "রক্তদাতা", "Blood Donors", "BLOOD", "মহেশখালী ও কুতুবদিয়া ডিরেক্টরি", "Moheshkhali & Kutubdia Registry", 0xFFC2185B),
            PublicServiceItem("development", "উন্নয়ন", "Development", "DEVELOPMENT", "মেগা প্রকল্প ও অগ্রগতির চিত্র", "Mega Projects & Milestones", 0xFF1976D2),
            PublicServiceItem("engagement", "জনসম্পৃক্ততা", "Engagement", "ENGAGEMENT", "নাগরিক মতামত ও আবেদন", "Citizen Feedback & Petitions", 0xFF00796B),
            PublicServiceItem("tourism", "পর্যটন ও ভ্রমণ", "Tourism & Travel", "TOURISM", "আদিনাথ, সোনাদিয়া, কুতুবদিয়া", "Adinath, Sonadia, Kutubdia", 0xFFF57C00)
        )
    )
    val publicServices: StateFlow<List<PublicServiceItem>> = _publicServices.asStateFlow()

    // 6. Tourism Spots
    val defaultTourismSpots = listOf(
        TourismSpot(
            spotId = "adinath_temple",
            titleBn = "আদিনাথ মন্দির (মৈনাক পাহাড়)",
            titleEn = "Adinath Temple (Mainak Hill)",
            category = "Moheshkhali",
            coverImageUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&q=80",
            descriptionBn = "মহেশখালী দ্বীপের সর্বোচ্চ মৈনাক পাহাড়ের চূড়ায় অবস্থিত ঐতিহাসিক ও সনাতন ধর্মাবলম্বীদের অন্যতম প্রধান তীর্থস্থান আদিনাথ শিব মন্দির।",
            descriptionEn = "Perched on the summit of Mainak Hill in Moheshkhali, Adinath Temple is one of the most revered ancient Hindu pilgrimage sites.",
            travelRouteDetailsBn = "কক্সবাজার ৬ নং ঘাট থেকে স্পিডবোটে মহেশখালী গোরকঘাটা জেটি (২০ মিনিট), সেখান থেকে সিএনজি নিয়ে মৈনাক পাহাড় (১৫ মিনিট)।",
            travelRouteDetailsEn = "Speedboat from Cox's Bazar Ghat 6 to Gorakghata (20 mins), then local CNG to Mainak Hill.",
            transportTypes = listOf("Speedboat", "Sea-truck", "CNG Auto"),
            approxFareBdt = "স্পিডবোট: ১১০ টাকা, সিএনজি: ৩০-৪০ টাকা",
            recommendedHotelsBn = listOf("মহেশখালী জেলা পরিষদ ডাকবাংলো", "হোটেল অবকাশ"),
            recommendedHotelsEn = listOf("Zila Parishad Dak Bungalow", "Hotel Abakash"),
            seafoodRestaurantsBn = listOf("আদিনাথ হিলভিউ রেস্তোরাঁ", "গোরকঘাটা তাজা রূপচাঁদা ভাত হোটেল"),
            seafoodRestaurantsEn = listOf("Adinath Hillview Restaurant", "Gorakghata Fresh Pomfret Hotels"),
            googleMapsUrl = "https://maps.google.com/?q=Adinath+Temple+Moheshkhali",
            navigationStepsBn = listOf("১. কক্সবাজার ৬ নং ঘাটে পৌঁছান", "২. লাইফ জ্যাকেট পরে বোটে উঠুন", "৩. গোরকঘাটা জেটি থেকে সিএনজি নিন"),
            navigationStepsEn = listOf("1. Reach Ghat No. 6", "2. Board boat with life jacket", "3. Take local CNG to temple")
        ),
        TourismSpot(
            spotId = "sonadia_island",
            titleBn = "সোনাদিয়া দ্বীপ (মহেশখালী)",
            titleEn = "Sonadia Island (Moheshkhali)",
            category = "Moheshkhali",
            coverImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
            descriptionBn = "লাল কাঁকড়ার অভয়াশ্রম, ঝাউবন, শামুক-ঝিনুক খচিত অনাবিল সমুদ্রসৈকত এবং পরিযায়ী পাখির কলকাকলিতে মুখরিত এক দ্বীপ।",
            descriptionEn = "Pristine coastal island renowned for red crabs, virgin sand dunes, and tranquil shores.",
            travelRouteDetailsBn = "মহেশখালী ঘাট থেকে ট্রলার বা স্পিডবোটে সরাসরি সোনাদিয়া দ্বীপে যাতায়াত।",
            travelRouteDetailsEn = "Board a trawler or private boat from Gorakghata to Sonadia Island.",
            transportTypes = listOf("Trawler", "Speedboat", "Walking"),
            approxFareBdt = "ট্রলার: ৫০-৮০ টাকা",
            recommendedHotelsBn = listOf("ইকো-ক্যাম্পিং (অনুমোদন সাপেক্ষে)", "মহেশখালী সদরের হোটেল"),
            recommendedHotelsEn = listOf("Eco-camping with permit", "Moheshkhali Sadar hotels"),
            seafoodRestaurantsBn = listOf("তাজা সামুদ্রিক মাছ ও শুঁটকি ভর্তা"),
            seafoodRestaurantsEn = listOf("Fresh island seafood and local rice"),
            googleMapsUrl = "https://maps.google.com/?q=Sonadia+Island+Moheshkhali",
            navigationStepsBn = listOf("১. মহেশখালী ঘাট থেকে ট্রলারে উঠুন", "২. ম্যানগ্রোভ খাঁড়ি অতিক্রম করুন", "৩. সৈকতে পদার্পণ করুন"),
            navigationStepsEn = listOf("1. Board trawler at Gorakghata", "2. Navigate coastal creeks", "3. Explore virgin beach")
        ),
        TourismSpot(
            spotId = "kutubdia_lighthouse",
            titleBn = "কুতুবদিয়া বাতিঘর (Lighthouse)",
            titleEn = "Kutubdia Lighthouse",
            category = "Kutubdia",
            coverImageUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&q=80",
            descriptionBn = "১৮৪৬ সালে ব্রিটিশ আমলে প্রতিষ্ঠিত ঐতিহাসিক কুতুবদিয়া বাতিঘর, যা বঙ্গোপসাগরের গভীর জলসীমায় চলাচলকারী জাহাজগুলোকে দিকনির্দেশনা দেয়।",
            descriptionEn = "Built in 1846 during British rule, guiding vessels across the Bay of Bengal for generations.",
            travelRouteDetailsBn = "মগনামা ঘাট থেকে বোট বা স্পিডবোটে কুতুবদিয়া দরবার ঘাট (১৫ মিনিট), সেখান থেকে সিএনজিতে বাতিঘর।",
            travelRouteDetailsEn = "Cross from Magnama Ghat by boat to Kutubdia Darbar Ghat (15 mins), then CNG to Lighthouse.",
            transportTypes = listOf("Speedboat", "Sea-truck", "CNG Auto"),
            approxFareBdt = "বোট: ৫০-৮০ টাকা, সিএনজি: ৫০ টাকা",
            recommendedHotelsBn = listOf("হোটেল সাগরিকা (বড়ঘোপ)", "উপজেলা রেস্টহাউজ"),
            recommendedHotelsEn = listOf("Hotel Sagarika (Baraghop)", "Upazila Guest House"),
            seafoodRestaurantsBn = listOf("উপকূলীয় তাজা লইট্টা ও রূপচাঁদা ভাতের হোটেল"),
            seafoodRestaurantsEn = listOf("Baraghop coastal fish diners"),
            googleMapsUrl = "https://maps.google.com/?q=Kutubdia+Lighthouse",
            navigationStepsBn = listOf("১. মগনামা ঘাট থেকে বোট নিন", "২. কুতুবদিয়া দরবার ঘাটে নামুন", "৩. সিএনজি নিয়ে বাতিঘরে যান"),
            navigationStepsEn = listOf("1. Take boat at Magnama Ghat", "2. Arrive at Darbar Ghat", "3. Take local CNG to Lighthouse")
        ),
        TourismSpot(
            spotId = "kutubdia_sea_beach",
            titleBn = "কুতুবদিয়া সমুদ্রসৈকত ও বায়ুবিদ্যুৎ কেন্দ্র",
            titleEn = "Kutubdia Sea Beach & Windmill Park",
            category = "Kutubdia",
            coverImageUrl = "https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?w=800&q=80",
            descriptionBn = "২০ কিলোমিটার দীর্ঘ নিরিবিলি সৈকত ও দেশের প্রথম বাণিজ্যিক বায়ুবিদ্যুৎ কেন্দ্র (Windmill)।",
            descriptionEn = "An unspoiled 20km sandy beach alongside Bangladesh's pioneer commercial wind turbine farm.",
            travelRouteDetailsBn = "কুতুবদিয়া বড়ঘোপ বা সদর থেকে সিএনজিতে ২০ মিনিটে আলী আকবর ডেইল সৈকত।",
            travelRouteDetailsEn = "From Kutubdia Sadar take a CNG (20 mins) to Ali Akbar Deil beach.",
            transportTypes = listOf("CNG Auto", "Battery Rickshaw", "Motorbike"),
            approxFareBdt = "সিএনজি: ৬০-৮০ টাকা",
            recommendedHotelsBn = listOf("বড়ঘোপ বাজারের আবাসিক হোটেল ও সরকারি রেস্টহাউজ"),
            recommendedHotelsEn = listOf("Baraghop coastal lodges & government rest houses"),
            seafoodRestaurantsBn = listOf("সৈকত সংলগ্ন বিচ ক্যাফে ও স্থানীয় ডাব"),
            seafoodRestaurantsEn = listOf("Beachfront eateries serving fresh coconut and island fish"),
            googleMapsUrl = "https://maps.google.com/?q=Kutubdia+Windmill+Beach",
            navigationStepsBn = listOf("১. সদর থেকে পশ্চিম উপকূলীয় সড়কে যান", "২. বায়ুবিদ্যুৎ উইন্ডমিলের পাদদেশে যান"),
            navigationStepsEn = listOf("1. Drive along west coastal road", "2. Reach windmill park")
        )
    )
    private val _tourismSpots = MutableStateFlow(defaultTourismSpots)
    val tourismSpots: StateFlow<List<TourismSpot>> = _tourismSpots.asStateFlow()

    // 7. Development Projects (Synced with Firestore collection: development_projects)
    val defaultDevelopmentProjects = listOf(
        DevelopmentProject(
            id = "port",
            titleBn = "মাতারবাড়ি গভীর সমুদ্রবন্দর",
            titleEn = "Matarbari Deep Sea Port",
            categoryBn = "মেগা প্রকল্প",
            categoryEn = "Mega Infrastructure",
            budgetBn = "১৭,৭৭৭ কোটি টাকা",
            budgetEn = "BDT 17,777 Crore",
            completionPercentage = 88,
            descriptionBn = "বাংলাদেশের প্রথম গভীর সমুদ্র বন্দর, যা ১৮.৫ মিটার ড্রাফটের বাণিজ্যিক জাহাজ ভেড়ার সক্ষমতা সম্পন্ন।",
            descriptionEn = "Bangladesh's premier deep sea port capable of berthing vessels with 18.5m draft.",
            impactBn = "লক্ষাধিক প্রত্যক্ষ ও পরোক্ষ কর্মসংস্থান, উপকূলীয় অর্থনীতির ব্যাপক রূপান্তর।",
            impactEn = "Over 100,000 jobs and profound transformation of coastal industrial economy.",
            timelineBn = "২০১৪ – ২০২৪",
            timelineEn = "2014 – 2024",
            imageUrl = "drawable:dev_port",
            status = "Ongoing",
            upazila = "Moheshkhali",
            union = "মাতারবাড়ি",
            year = 2024
        ),
        DevelopmentProject(
            id = "power",
            titleBn = "মাতারবাড়ি বিদ্যুৎ কেন্দ্র",
            titleEn = "Matarbari Power Plant",
            categoryBn = "বিদ্যুৎ",
            categoryEn = "Power & Energy",
            budgetBn = "৫১,৮৫৪ কোটি টাকা",
            budgetEn = "BDT 51,854 Crore",
            completionPercentage = 95,
            descriptionBn = "পরিবেশবান্ধব আল্ট্রা সুপার ক্রিটিক্যাল প্রযুক্তিতে নির্মিত দেশের বৃহত্তম বিদ্যুৎ উৎপাদন কেন্দ্র।",
            descriptionEn = "Modern eco-efficient ultra super critical thermal power station feeding power into the national grid.",
            impactBn = "শিল্প কারখানায় নির্ভরযোগ্য বিদ্যুৎ এবং দক্ষিণ চট্টগ্রামের জ্বালানি নিরাপত্তা।",
            impactEn = "Industrial energy reliability and regional economic growth.",
            timelineBn = "২০১৪ – ২০২৪",
            timelineEn = "2014 – 2024",
            imageUrl = "drawable:dev_power",
            status = "Ongoing",
            upazila = "Moheshkhali",
            union = "মাতারবাড়ি",
            year = 2023,
            beforeImageUrl = "https://images.unsplash.com/photo-1473496169904-658ba7c44d8a?w=800&q=80",
            afterImageUrl = "https://images.unsplash.com/photo-1466611653911-954ffea113ad?w=800&q=80",
            pdfUrl = "https://example.com/report_power.pdf"
        ),
        DevelopmentProject(
            id = "edu",
            titleBn = "মহেশখালী সরকারি কলেজ আধুনিকায়ন",
            titleEn = "Moheshkhali Govt College Modernization",
            categoryBn = "শিক্ষা",
            categoryEn = "Education",
            budgetBn = "২৫ কোটি টাকা",
            budgetEn = "BDT 25 Crore",
            completionPercentage = 100,
            descriptionBn = "নতুন আইসিটি ভবন ও ল্যাবরেটরি স্থাপন।",
            descriptionEn = "Establishment of new ICT building and modern laboratories.",
            impactBn = "শিক্ষার্থীদের কারিগরি জ্ঞান বৃদ্ধি।",
            impactEn = "Enhanced technical knowledge for students.",
            timelineBn = "২০২০ – ২০২২",
            timelineEn = "2020 – 2022",
            imageUrl = "drawable:dev_edu",
            status = "Completed",
            upazila = "Moheshkhali",
            union = "বড় মহেশখালী",
            year = 2022,
            beforeImageUrl = "https://images.unsplash.com/photo-1541339907198-e08756eaa539?w=800&q=80",
            afterImageUrl = "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=800&q=80",
            pdfUrl = "https://example.com/edu_report.pdf"
        ),
        DevelopmentProject(
            id = "health",
            titleBn = "কুতুবদিয়া উপজেলা স্বাস্থ্য কমপ্লেক্স",
            titleEn = "Kutubdia Health Complex",
            categoryBn = "স্বাস্থ্য",
            categoryEn = "Health",
            budgetBn = "১৫ কোটি টাকা",
            budgetEn = "BDT 15 Crore",
            completionPercentage = 90,
            descriptionBn = "৫০ শয্যা বিশিষ্ট আধুনিক চিকিৎসা কেন্দ্র।",
            descriptionEn = "50-bed modern healthcare facility.",
            impactBn = "দ্বীপবাসীর জন্য উন্নত চিকিৎসা সেবা।",
            impactEn = "Improved healthcare for islanders.",
            timelineBn = "২০২১ – ২০২৪",
            timelineEn = "2021 – 2024",
            imageUrl = "drawable:dev_health",
            status = "Ongoing",
            upazila = "Kutubdia",
            union = "বড়ঘোপ",
            year = 2024
        ),
        DevelopmentProject(
            id = "road_kutub",
            titleBn = "কুতুবদিয়া প্রধান সড়ক উন্নয়ন",
            titleEn = "Kutubdia Main Road Development",
            categoryBn = "যোগাযোগ",
            categoryEn = "Communication",
            budgetBn = "৪৫ কোটি টাকা",
            budgetEn = "BDT 45 Crore",
            completionPercentage = 100,
            descriptionBn = "দ্বীপের প্রধান সংযোগ সড়ক প্রশস্তকরণ ও পিচ ঢালাই।",
            descriptionEn = "Widening and carpeting of the island's main artery.",
            impactBn = "দ্রুত যোগাযোগ ও অর্থনৈতিক গতিশীলতা।",
            impactEn = "Faster communication and economic mobility.",
            timelineBn = "২০১৮ – ২০২১",
            timelineEn = "2018 – 2021",
            imageUrl = "drawable:dev_road",
            status = "Completed",
            upazila = "Kutubdia",
            union = "লেমশীখালী",
            year = 2021,
            beforeImageUrl = "https://images.unsplash.com/photo-1515162305114-8d3ad470f19c?w=800&q=80",
            afterImageUrl = "https://images.unsplash.com/photo-1469000157175-e30de26983d1?w=800&q=80"
        ),
        DevelopmentProject(
            id = "rural",
            titleBn = "হোয়ানক গ্রামীণ রাস্তা",
            titleEn = "Hoanok Rural Road",
            categoryBn = "গ্রামীণ",
            categoryEn = "Rural Development",
            budgetBn = "৫ কোটি টাকা",
            budgetEn = "BDT 5 Crore",
            completionPercentage = 100,
            descriptionBn = "হোয়ানক ইউনিয়নের অভ্যন্তরীণ রাস্তা সলিং ও ঢালাই।",
            descriptionEn = "Internal road soling and casting in Hoanok union.",
            impactBn = "কৃষকদের পণ্য পরিবহনে সুবিধা।",
            impactEn = "Ease of transport for farmers.",
            timelineBn = "২০২২ – ২০২৩",
            timelineEn = "2022 – 2023",
            imageUrl = "drawable:dev_rural",
            status = "Completed",
            upazila = "Moheshkhali",
            union = "হোয়ানক",
            year = 2023
        ),
        DevelopmentProject(
            id = "shield",
            titleBn = "উপকূলীয় বেড়িবাঁধ সুরক্ষা",
            titleEn = "Coastal Embankment Protection",
            categoryBn = "সুরক্ষা",
            categoryEn = "Coastal Protection",
            budgetBn = "৩০০ কোটি টাকা",
            budgetEn = "BDT 300 Crore",
            completionPercentage = 85,
            descriptionBn = "সিসি ব্লক দ্বারা টেকসই বেড়িবাঁধ নির্মাণ।",
            descriptionEn = "Construction of sustainable embankments with CC blocks.",
            impactBn = "জলোচ্ছ্বাস থেকে জানমাল রক্ষা।",
            impactEn = "Protection of life and property from tidal surges.",
            timelineBn = "২০১৪ – ২০২৪",
            timelineEn = "2014 – 2024",
            imageUrl = "drawable:dev_shield",
            status = "Ongoing",
            upazila = "Moheshkhali",
            union = "ধলঘাটা",
            year = 2024
        ),
        DevelopmentProject(
            id = "industry",
            titleBn = "ধলঘাটা অর্থনৈতিক অঞ্চল",
            titleEn = "Dhalghata Economic Zone",
            categoryBn = "শিল্প",
            categoryEn = "Industry",
            budgetBn = "৫০০০ কোটি টাকা",
            budgetEn = "BDT 5000 Crore",
            completionPercentage = 40,
            descriptionBn = "শিল্প পার্ক ও বিনিয়োগ অঞ্চল উন্নয়ন।",
            descriptionEn = "Development of industrial park and investment zone.",
            impactBn = "বিপুল কর্মসংস্থান সৃষ্টি।",
            impactEn = "Creation of mass employment.",
            timelineBn = "২০২৩ – ২০২৮",
            timelineEn = "2023 – 2028",
            imageUrl = "drawable:dev_ez",
            status = "Ongoing",
            upazila = "Moheshkhali",
            union = "ধলঘাটা",
            year = 2024
        ),
        DevelopmentProject(
            id = "union_dev",
            titleBn = "কালারমারছড়া আদর্শ গ্রাম",
            titleEn = "Kalarmarchara Model Village",
            categoryBn = "ইউনিয়ন",
            categoryEn = "Union Development",
            budgetBn = "১০ কোটি টাকা",
            budgetEn = "BDT 10 Crore",
            completionPercentage = 100,
            descriptionBn = "কালারমারছড়া ইউনিয়নে আধুনিক ড্রেনেজ ও কমিউনিটি সেন্টার।",
            descriptionEn = "Modern drainage and community center in Kalarmarchara union.",
            impactBn = "জীবনযাত্রার মান উন্নয়ন।",
            impactEn = "Improvement of living standards.",
            timelineBn = "২০১৫ – ২০১৭",
            timelineEn = "2015 – 2017",
            imageUrl = "drawable:moheshkhali_banner",
            status = "Completed",
            upazila = "Moheshkhali",
            union = "কালারমারছড়া",
            year = 2017
        ),
        DevelopmentProject(
            id = "archive_item",
            titleBn = "উন্নয়ন আর্কাইভ - প্রথম দশক",
            titleEn = "Dev Archive - First Decade",
            categoryBn = "আর্কাইভ",
            categoryEn = "Archive",
            budgetBn = "০",
            budgetEn = "0",
            completionPercentage = 100,
            descriptionBn = "২০১৪-২০২৪ সালের সামগ্রিক উন্নয়নের ডিজিটাল নথি।",
            descriptionEn = "Digital documentation of overall development 2014-2024.",
            impactBn = "তথ্য সংরক্ষণ।",
            impactEn = "Information preservation.",
            timelineBn = "২০১৪ – ২০২৪",
            timelineEn = "2014 – 2024",
            imageUrl = "drawable:dev_kutubdia",
            status = "Completed",
            upazila = "All",
            union = "All",
            year = 2014,
            beforeImageUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=800&q=80",
            afterImageUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?w=800&q=80",
            pdfUrl = "https://example.com/decade_report.pdf"
        )
    )

    private val _developmentProjects = MutableStateFlow(defaultDevelopmentProjects)
    val developmentProjects: StateFlow<List<DevelopmentProject>> = _developmentProjects.asStateFlow()

    // 8. Gallery Items (Synced with Firestore collection: gallery_items)
    val defaultGalleryItems = listOf(
        GalleryItem("g1", "মাতারবাড়ী গভীর সমুদ্র বন্দর জেটি পরিদর্শন", "Matarbari Port Inspection", "উন্নয়নচিত্র", "Development Pictures", "drawable:dev_port", "PHOTO", "", "২০২৬", "2026", "মাতারবাড়ি গভীর সমুদ্র বন্দর ও চ্যানেল প্রকল্প সরাসরি পরিদর্শনকালে জননেতা আশেক উল্লাহ রফিক।", "মাতারবাড়ি গভীর সমুদ্রবন্দর চ্যানেল", "মাতারবাড়ি, মহেশখালী", "মাতারবাড়ি", "মহেশখালী", "২০২৬", "সরকারি তথ্যভাণ্ডার ও প্রেস উইং", true),
        GalleryItem("g2", "দ্বীপের শিক্ষা ও স্বাস্থ্যসেবা প্রতিষ্ঠান পরিদর্শন", "Education & Health Facility Visit", "শিক্ষা ও স্বাস্থ্য", "Education & Health", "drawable:asheq_profile_custom", "PHOTO", "", "২০২৬", "2026", "মহেশখালী সরকারি কলেজ ও উপজেলা স্বাস্থ্য কমপ্লেক্স আকস্মিক পরিদর্শন।", "শিক্ষা ও স্বাস্থ্য ব্যবস্থার আধুনিকায়ন", "গোরকঘাটা, মহেশখালী", "গোরকঘাটা", "মহেশখালী", "২০২৬", "শিক্ষা মন্ত্রণালয় ও প্রেস উইং", true),
        GalleryItem("g3", "জনসাধারণের সাথে মতবিনিময় ও নাগরিক সেবা প্রদান", "Citizen Service & Public Interaction", "জনজীবন ও জনসেবা", "Public Life & Citizen Service", "drawable:asheq_avatar", "PHOTO", "", "২০২৬", "2026", "তৃণমূল মানুষের দোরগোড়ায় নাগরিক সেবা পৌঁছে দেওয়ার বিশেষ কর্মসূচি।", "নাগরিক সেবা ক্যাম্পেইন", "বড়ঘোপ, কুতুবদিয়া", "বড়ঘোপ", "কুতুবদিয়া", "২০২৬", "উপজেলা প্রশাসন", true),
        GalleryItem("g4", "রাজনৈতিক ও সাংগঠনিক কর্মী সমাবেশ", "Political & Organizational Rally", "রাজনৈতিক ও সাংগঠনিক কার্যক্রম", "Political Activities", "drawable:moheshkhali_banner", "PHOTO", "", "২০২৬", "2026", "উপজেলা ও ইউনিয়ন আওয়ামী লীগ ও সহযোগী সংগঠনের বিশাল কর্মী সমাবেশ।", "দলীয় শৃঙ্খলা ও উন্নয়ন ধারা অব্যাহত রাখা", "মহেশখালী পৌরসভা", "গোরকঘাটা", "মহেশখালী", "২০২৬", "দলীয় প্রেস উইং", true),
        GalleryItem("g5", "কুতুবদিয়ার ঐতিহাসিক বাতিঘর ও প্রবীণ ব্যক্তিত্ব", "Historical Kutubdia Lighthouse", "ঐতিহাসিক ছবি", "Historical Photos", "drawable:dev_kutubdia", "PHOTO", "", "২০২৬", "2026", "১৮৪৬ সালে ব্রিটিশ আমলে নির্মিত কুতুবদিয়ার ঐতিহ্যবাহী বাতিঘর চত্বর।", "ঐতিহাসিক বাতিঘর দর্শন", "আলী আকবর ডেইল, কুতুবদিয়া", "আলী আকবর ডেইল", "কুতুবদিয়া", "২০২৬", "ঐতিহাসিক আর্কাইভ", true),
        GalleryItem("g6", "জাতীয় দিবস উদযাপন ও বিশেষ সাংস্কৃতিক অনুষ্ঠান", "National Day Cultural Event", "অনুষ্ঠান ও বিশেষ আয়োজন", "Special Events", "drawable:banner_video_poster", "PHOTO", "", "২০২৬", "2026", "মহান স্বাধীনতা দিবস উপলক্ষে আয়োজিত আলোচনা সভা ও সাংস্কৃতিক অনুষ্ঠান।", "স্বাধীনতা দিবস উদযাপন", "উপজেলা প্রাঙ্গণ, মহেশখালী", "গোরকঘাটা", "মহেশখালী", "২০২৬", "উপজেলা তথ্য অফিস", true),
        GalleryItem("g7", "উপকূলীয় দুর্যোগ ব্যবস্থাপনা ও সিসি ব্লক বেড়িবাঁধ পরিদর্শন", "Coastal Disaster & Embankment Inspection", "দুর্যোগ ও উপকূল", "Disaster & Coast", "drawable:dev_road", "PHOTO", "", "২০২৬", "2026", "ঘূর্ণিঝড় ও জোয়ারের পানি থেকে রক্ষায় নির্মিত সিসি ব্লক বেড়িবাঁধের কাজ পরিদর্শন।", "জলবায়ু সহনশীল বেড়িবাঁধ", "কালারমারছড়া, মহেশখালী", "কালারমারছড়া", "মহেশখালী", "২০২৬", "পানি উন্নয়ন বোর্ড", true),
        GalleryItem("g8", "ব্যক্তিগত স্মৃতিচারণ ও দ্বীপবাসীর সাথে অন্তরঙ্গ মুহূর্ত", "Personal Memories with Island People", "ব্যক্তিগত ও স্মৃতিচারণ", "Personal Memories", "drawable:asheq_profile_custom", "PHOTO", "", "২০২৬", "2026", "দ্বীপবাসীর সুখে-দুঃখে পাশে থাকার নানা আবেগঘন মুহূর্তের স্মৃতি।", "জনমানুষের সাথে আত্মিক বন্ধন", "মহেশখালী ও কুতুবদিয়া", "সকল এলাকা", "মহেশখালী-কুতুবদিয়া", "২০২৬", "ব্যক্তিগত আর্কাইভ", true)
    )
    private val _galleryItems = MutableStateFlow(defaultGalleryItems)
    val galleryItems: StateFlow<List<GalleryItem>> = _galleryItems.asStateFlow()

    val defaultMediaArchiveItems = listOf(
        MediaArchiveItem("m1", "মাতারবাড়ি গভীর সমুদ্র বন্দরের প্রথম বাণিজ্যিক জাহাজ ভেড়ার ঐতিহাসিক মুহূর্ত", "Historic Arrival of First Ship at Matarbari Port", "সংবাদ প্রতিবেদন", "দৈনিক প্রথম আলো", "১২ অক্টোবর, ২০২৪", "মাতারবাড়ি গভীর সমুদ্র বন্দরে প্রথমবারের মতো বাণিজ্যিক জাহাজ নোঙর করার মাধ্যমে বাংলাদেশের সমুদ্র অর্থনীতিতে নতুন যুগের সূচনা হয়েছে।", "https://www.prothomalo.com", "", "অর্থনীতি ও বাণিজ্য", "মহেশখালী", "মাতারবাড়ি", "প্রথম আলো", true),
        MediaArchiveItem("m2", "কুতুবদিয়ায় সাবমেরিন ক্যাবল বিদ্যুৎ সংযোগ নিয়ে সময় টিভির বিশেষ ভিডিও প্রতিবেদন", "Somoy TV Video Report on Kutubdia Submarine Electricity", "ভিডিও প্রতিবেদন", "সময় টিভি", "২০ আগস্ট, ২০২৪", "সাগরের তলদেশ দিয়ে সাবমেরিন ক্যাবল টেনে কুতুবদিয়া দ্বীপে শতভাগ বিদ্যুৎ সরবরাহের সরেজমিন ভিডিও প্রতিবেদন।", "https://www.youtube.com", "https://www.youtube.com", "বিদ্যুৎ ও জ্বালানি", "কুতুবদিয়া", "বড়ঘোপ", "সময় টিভি", true),
        MediaArchiveItem("m3", "উপকূলীয় উন্নয়ন ও কর্মসংস্থান নিয়ে সাবেক এমপি আশেক উল্লাহ রফিকের একান্ত সাক্ষাৎকার", "Exclusive Interview on Coastal Development", "সাক্ষাৎকার", "দৈনিক কালের কণ্ঠ", "১০ সেপ্টেম্বর, ২০২৪", "দ্বীপ উপজেলা মহেশখালী ও কুতুবদিয়ার মেগা প্রকল্প, কর্মসংস্থান এবং ভবিষ্যৎ পরিকল্পনা নিয়ে একান্ত সাক্ষাৎকার।", "https://www.kalerkantho.com", "", "রাজনীতি ও উন্নয়ন", "মহেশখালী", "সকল এলাকা", "কালের কণ্ঠ", true),
        MediaArchiveItem("m4", "জাতীয় সংসদে কুতুবদিয়া বেড়িবাঁধ ও উপকূলীয় সুরক্ষার জোরালো বক্তব্য", "Parliament Speech on Coastal Protection", "বক্তব্য ও বিবৃতি", "জাতীয় সংসদ সচিবালয়", "১৫ মার্চ, ২০২৪", "জাতীয় সংসদে কুতুবদিয়া ও মহেশখালীর লাখ লাখ মানুষকে জলোচ্ছ্বাস থেকে রক্ষায় টেকসই সিসি ব্লক বেড়িবাঁধ নির্মাণের দাবি।", "http://www.parliament.gov.bd", "", "সংসদীয় কার্যক্রম", "কুতুবদিয়া", "সকল এলাকা", "সংসদ সচিবালয়", true),
        MediaArchiveItem("m5", "মাতারবাড়ি অর্থনৈতিক অঞ্চল ও শিল্পায়নের অগ্রগতি নিয়ে সংবাদ সম্মেলন", "Press Conference on Matarbari Economic Zone", "সংবাদ সম্মেলন", "প্রেস ক্লাব, কক্সবাজার", "৫ জুন, ২০২৪", "মাতারবাড়ি অর্থনৈতিক করিডোর এবং স্থানীয় তরুণদের কর্মসংস্থান সৃষ্টির অগ্রগতি তুলে ধরতে বিশেষ সংবাদ সম্মেলন।", "https://www.banglatribune.com", "", "শিল্প ও কর্মসংস্থান", "মহেশখালী", "গোরকঘাটা", "সংবাদ মাধ্যম", true),
        MediaArchiveItem("m6", "দ্বীপবাসীর জীবনমান ও মেগা প্রকল্প নিয়ে সংবাদপত্রের বিশেষ কাটিং", "Newspaper Clippings Collection on Island Development", "পত্রিকার কাটিং", "দৈনিক ইত্তেফাক", "১৮ জুলাই, ২০২৪", "মহেশখালী-কুতুবদিয়ার যোগাযোগ, বিদ্যুৎ, শিক্ষা ও স্বাস্থ্য খাতের অভূতপূর্ব উন্নয়নের সংবাদপত্র কাটিং আর্কাইভ।", "https://www.ittefaq.com.bd", "", "সংবাদ সংগ্রহ", "মহেশখালী-কুতুবদিয়া", "সকল এলাকা", "ইত্তেফাক আর্কাইভ", true),
        MediaArchiveItem("m7", "ডিবিসি নিউজ টকশো: উপকূলীয় দ্বীপ অঞ্চলের জলবায়ু ঝুঁকি ও সমাধান", "DBC News Talk Show on Coastal Climate Resilience", "টকশো ও আলোচনা", "ডিবিসি নিউজ", "১২ নভেম্বর, ২০২৪", "জলবায়ু পরিবর্তন মোকাবিলায় মহেশখালী ও কুতুবদিয়ার বেড়িবাঁধ এবং সাইক্লোন শেল্টার নির্মাণ নিয়ে বিশিষ্টজনদের আলোচনা।", "https://www.dbcnews.tv", "https://www.youtube.com", "দুর্যোগ ও জলবায়ু", "মহেশখালী", "সকল এলাকা", "ডিবিসি নিউজ", true),
        MediaArchiveItem("m8", "জনকল্যাণ ও উন্নয়ন প্রকল্প সফল করায় বিশেষ প্রেস রিলিজ", "Official Press Release on Successful Completion of Mega Projects", "প্রেস রিলিজ", "এমপি প্রেস উইং", "৩০ ডিসেম্বর, ২০২৪", "মহেশখালী ও কুতুবদিয়ায় বিগত এক দশকে বাস্তবায়িত মেগা প্রকল্পসমূহের বিবরণী ও জনকল্যাণমূলক প্রেস রিলিজ।", "https://ashequllahrafiq.org", "", "প্রেস বিজ্ঞপ্তি", "মহেশখালী-কুতুবদিয়া", "সকল এলাকা", "প্রেস উইং", true)
    )
    private val _mediaArchiveItems = MutableStateFlow(defaultMediaArchiveItems)
    val mediaArchiveItems: StateFlow<List<MediaArchiveItem>> = _mediaArchiveItems.asStateFlow()

    // 8.1 Observe Media Archive Items (Auto-seed if empty)
    private fun setupMediaArchiveListener(db: FirebaseFirestore) {
        db.collection("media_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Media archive listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultMediaArchiveItems.forEach { m ->
                                        val doc = db.collection("media_items").document(m.id)
                                        batch.set(doc, m)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed media archive: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseMediaArchiveItem(doc)
                        }.filter { it.isPublished }
                        _mediaArchiveItems.value = list
                    }
                }
            }
    }

    // 8.1.1 Observe Govt Reports & Gazettes (Auto-seed if empty)
    private fun setupGovtReportsListener(db: FirebaseFirestore) {
        db.collection("government_reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Govt reports listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultGovtReports.forEach { rep ->
                                        val doc = db.collection("government_reports").document(rep.id)
                                        batch.set(doc, rep)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed govt reports: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val reports = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseGovtReport(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _govtReports.value = reports
                    }
                }
            }
    }

    // 8.3 Important External Links & Wikipedia (Synced with Firestore collection: important_links)
    val defaultImportantLinks = listOf(
        ImportantExternalLink(
            id = "link1",
            titleBn = "উইকিপিডিয়া প্রোফাইল: আশেক উল্লাহ রফিক",
            titleEn = "Wikipedia Profile: Asheq Ullah Rafiq",
            url = "https://bn.wikipedia.org/wiki/%E0%A6%86%E0%A6%B6%E0%A7%87%E0%A6%95_%E0%A6%89%E0%A6%B2%E0%A7%8D%E0%A6%B2%E0%A6%BE%E0%A6%B9_%E0%A6%B0%E0%A6%AB%E0%A6%BF%E0%A6%95",
            categoryBn = "উইকিপিডিয়া",
            categoryEn = "Wikipedia",
            descriptionBn = "বাংলাদেশি রাজনীতিবিদ, ব্যবসায়ী এবং কক্সবাজার-২ আসনের সাবেক সংসদ সদস্য সম্পর্কিত মুক্ত বিশ্বকোষীয় নিবন্ধ।",
            descriptionEn = "Open encyclopedia article on former Member of Parliament for Cox's Bazar-2.",
            sourceName = "Wikipedia"
        ),
        ImportantExternalLink(
            id = "link2",
            titleBn = "বাংলাদেশ জাতীয় সংসদ - সংসদ সদস্য পরিচিতি",
            titleEn = "Bangladesh National Parliament - MP Profile",
            url = "http://www.parliament.gov.bd",
            categoryBn = "জাতীয় সংসদ",
            categoryEn = "National Parliament",
            descriptionBn = "দশম, একাদশ ও দ্বাদশ জাতীয় সংসদের সদস্য হিসেবে দায়িত্ব, সংসদীয় স্থায়ী কমিটির বিবরণ ও কার্যক্রম।",
            descriptionEn = "Parliamentary duties, standing committee assignments, and official records.",
            sourceName = "Parliament"
        ),
        ImportantExternalLink(
            id = "link3",
            titleBn = "মহেশখালী-মাতারবাড়ি আল্ট্রা সুপার ক্রিটিক্যাল বিদ্যুৎ প্রকল্প",
            titleEn = "Matarbari Ultra Super Critical Power Project",
            url = "https://cpgcbl.gov.bd",
            categoryBn = "মেগা প্রকল্প",
            categoryEn = "Mega Project",
            descriptionBn = "কোল পাওয়ার জেনারেশন কোম্পানি বাংলাদেশ লিমিটেড (CPGCBL) এর মাতারবাড়ি ১২০০ মেগাওয়াট বিদ্যুৎ প্রকল্পের তথ্যাদি।",
            descriptionEn = "Official updates on Matarbari 1200 MW Power Project by CPGCBL.",
            sourceName = "CPGCBL"
        ),
        ImportantExternalLink(
            id = "link4",
            titleBn = "কক্সবাজার জেলা প্রশাসন ও সরকারি তথ্য বাতায়ন",
            titleEn = "Cox's Bazar District Web Portal",
            url = "https://www.coxsbazar.gov.bd",
            categoryBn = "অফিসিয়াল পোর্টাল",
            categoryEn = "Official Portal",
            descriptionBn = "কক্সবাজার জেলা, মহেশখালী ও কুতুবদিয়া উপজেলার প্রশাসনিক সেবা, গেজেট ও নাগরিক তথ্য বাতায়ন।",
            descriptionEn = "Administrative portal for Cox's Bazar, Moheshkhali and Kutubdia.",
            sourceName = "Govt Portal"
        )
    )
    private val _importantLinks = MutableStateFlow(defaultImportantLinks)
    val importantLinks: StateFlow<List<ImportantExternalLink>> = _importantLinks.asStateFlow()

    // 8.4 Special Days & Events Ticker (Synced with Firestore collection: special_events)
    val defaultSpecialEvents = listOf(
        SpecialEventItem(
            id = "se1",
            titleBn = "২১ ফেব্রুয়ারি - মহান শহীদ দিবস ও আন্তর্জাতিক মাতৃভাষা দিবস",
            titleEn = "21 February - Language Martyrs' Day & Int'l Mother Language Day",
            eventDateIso = "2027-02-21",
            dateDisplayBn = "২১ ফেব্রুয়ারি, ২০২৭",
            dateDisplayEn = "21 February 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৯৫২ সালের মহান ভাষা আন্দোলনে আত্মোৎসর্গকারী বীর শহীদদের স্মরণে শহীদ বেদীতে পুষ্পস্তবক অর্পণ ও গভীর শ্রদ্ধা নিবেদন।",
            descriptionEn = "Honoring the language martyrs of 1952 and celebrating linguistic diversity worldwide.",
            locationOrTagBn = "জাতীয়",
            themeType = "MOURNING",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se2",
            titleBn = "১৭ মার্চ - জাতির পিতার জন্মবার্ষিকী ও জাতীয় শিশু দিবস",
            titleEn = "17 March - Bangabandhu Birthday & National Children's Day",
            eventDateIso = "2027-03-17",
            dateDisplayBn = "১৭ মার্চ, ২০২৭",
            dateDisplayEn = "17 March 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "জাতির পিতা বঙ্গবন্ধু শেখ মুজিবুর রহমানের শুভ জন্মবার্ষিকী এবং জাতীয় শিশু দিবস উদ্‌যাপন।",
            descriptionEn = "Commemorating the birth anniversary of Bangabandhu and celebrating National Children's Day.",
            locationOrTagBn = "জাতীয়",
            themeType = "FESTIVE",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se3",
            titleBn = "২৬ মার্চ - মহান স্বাধীনতা ও জাতীয় দিবস",
            titleEn = "26 March - Independence and National Day",
            eventDateIso = "2027-03-26",
            dateDisplayBn = "২৬ মার্চ, ২০২৭",
            dateDisplayEn = "26 March 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৯৭১ সালের মহান স্বাধীনতার ঘোষণা ও বীর মুক্তিযোদ্ধাদের অসীম আত্মত্যাগের প্রতি বিনম্র শ্রদ্ধা।",
            descriptionEn = "Commemorating the declaration of independence and honoring freedom fighters.",
            locationOrTagBn = "জাতীয়",
            themeType = "PATRIOTIC",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se4",
            titleBn = "১৪ এপ্রিল - পহেলা বৈশাখ ও বাংলা নববর্ষ",
            titleEn = "14 April - Pahela Baishakh & Bangla New Year",
            eventDateIso = "2027-04-14",
            dateDisplayBn = "১৪ এপ্রিল, ২০২৭",
            dateDisplayEn = "14 April 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "বাংলা নববর্ষের শুভ সূচনা, মঙ্গল শোভাযাত্রা ও ঐতিহ্যবাহী বাঙালি সংস্কৃতি উদ্‌যাপন।",
            descriptionEn = "Welcoming Bangla New Year with cultural rallies and festivities.",
            locationOrTagBn = "জাতীয়",
            themeType = "FESTIVE",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se5",
            titleBn = "১ মে - আন্তর্জাতিক মে দিবস ও শ্রমিক অধিকার দিবস",
            titleEn = "1 May - International Workers' Day",
            eventDateIso = "2027-05-01",
            dateDisplayBn = "১ মে, ২০২৭",
            dateDisplayEn = "1 May 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "শ্রমজীবী মানুষের ন্যায়সঙ্গত অধিকার অর্জন ও মেহনতি মানুষের সংহতি স্মরণে আন্তর্জাতিক মে দিবস।",
            descriptionEn = "Honoring labor rights and worker solidarity worldwide.",
            locationOrTagBn = "জাতীয়",
            themeType = "WORKERS",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se6",
            titleBn = "১৫ আগস্ট - জাতীয় শোক দিবস",
            titleEn = "15 August - National Mourning Day",
            eventDateIso = "2027-08-15",
            dateDisplayBn = "১৫ আগস্ট, ২০২৭",
            dateDisplayEn = "15 August 2027",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৯৭৫ সালের ১৫ই আগস্ট শাহাদাত বরণকারী জাতির পিতা ও তাঁর পরিবারের শহীদদের স্মরণে গভীর শোক নিবেদন।",
            descriptionEn = "Paying solemn tribute to the martyrs of 15 August 1975.",
            locationOrTagBn = "জাতীয়",
            themeType = "MOURNING",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se7",
            titleBn = "২১ নভেম্বর - সশস্ত্র বাহিনী দিবস",
            titleEn = "21 November - Armed Forces Day",
            eventDateIso = "2026-11-21",
            dateDisplayBn = "২১ নভেম্বর, ২০২৬",
            dateDisplayEn = "21 November 2026",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৯৭১ সালের মুক্তিযুদ্ধে সেনাবো বাহিনী, নৌবাহিনী ও বিমান বাহিনীর সম্মিলিত আক্রমণ ও বীরত্বের প্রতীক দিবস।",
            descriptionEn = "Honoring the joint operations of army, navy and air force during Liberation War.",
            locationOrTagBn = "জাতীয়",
            themeType = "WORKERS",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se8",
            titleBn = "১৪ ডিসেম্বর - শহীদ বুদ্ধিজীবী দিবস",
            titleEn = "14 December - Martyred Intellectuals Day",
            eventDateIso = "2026-12-14",
            dateDisplayBn = "১৪ ডিসেম্বর, ২০২৬",
            dateDisplayEn = "14 December 2026",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৯৭১ সালের বিজয় প্রাক্কালে শাহাদাত বরণকারী দেশের শ্রেষ্ঠ সন্তান শহীদ বুদ্ধিজীবীদের স্মরণে গভীর শ্রদ্ধা।",
            descriptionEn = "Commemorating the intellects martyred prior to national liberation.",
            locationOrTagBn = "জাতীয়",
            themeType = "MOURNING",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se9",
            titleBn = "১৬ ডিসেম্বর - মহান বিজয় দিবস",
            titleEn = "16 December - Victory Day of Bangladesh",
            eventDateIso = "2026-12-16",
            dateDisplayBn = "১৬ ডিসেম্বর, ২০২৬",
            dateDisplayEn = "16 December 2026",
            categoryBn = "জাতীয় দিবস",
            categoryEn = "National Day",
            descriptionBn = "১৬ই ডিসেম্বর গৌরবময় স্বাধীনতা বিজয় অর্জন, ৩০ লাখ শহীদ ও বীরদের আত্মত্যাগের প্রতি বিনম্র শ্রদ্ধা।",
            descriptionEn = "National celebration of liberation war victory and freedom.",
            locationOrTagBn = "জাতীয়",
            themeType = "PATRIOTIC",
            isFeaturedCountdown = true
        ),
        SpecialEventItem(
            id = "se10",
            titleBn = "কুতুবদিয়া সাগরের তলদেশ দিয়ে সাবমেরিন ক্যাবল শতভাগ বিদ্যুতায়ন দিবস",
            titleEn = "Kutubdia Undersea Submarine Cable Electrification Milestone",
            eventDateIso = "2026-11-25",
            dateDisplayBn = "২৫ নভেম্বর, ২০২৬",
            dateDisplayEn = "25 November 2026",
            categoryBn = "স্থানীয় উন্নয়ন দিবস",
            categoryEn = "Local Milestone",
            descriptionBn = "স্বাধীনতার ৫০ বছর পর কুতুবদিয়া দ্বীপে সাগরের তলদেশ দিয়ে শতভাগ গ্রিড বিদ্যুৎ পৌঁছে দেওয়ার মাইলফলক।",
            descriptionEn = "Historic 100% island electrification milestone via undersea cable.",
            locationOrTagBn = "কুতুবদিয়া",
            themeType = "LOCAL_DEV",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se11",
            titleBn = "মাতারবাড়ি গভীর সমুদ্র বন্দর ও ১৮.৫ মি. ড্রাফট জেটি বাণিজ্যিক সূচনা",
            titleEn = "Matarbari Deep Sea Port Commercial Berth Milestone",
            eventDateIso = "2026-12-28",
            dateDisplayBn = "২৮ ডিসেম্বর, ২০২৬",
            dateDisplayEn = "28 December 2026",
            categoryBn = "স্থানীয় উন্নয়ন দিবস",
            categoryEn = "Local Milestone",
            descriptionBn = "দক্ষিণ এশিয়ার শীর্ষ বাণিজ্যিক হাব মাতারবাড়ি বন্দর চ্যানেল ও ১২০০ মেগাওয়াট বিদ্যুৎ কেন্দ্রের অগ্রগতি স্মারক।",
            descriptionEn = "Matarbari port commercial channel and power generation landmark.",
            locationOrTagBn = "মহেশখালী",
            themeType = "LOCAL_DEV",
            isFeaturedCountdown = false
        ),
        SpecialEventItem(
            id = "se12",
            titleBn = "মহেশখালী-কুতুবদিয়া জলবায়ু-সহনশীল সিসি ব্লক বেড়িবাঁধ সুরক্ষা দিবস",
            titleEn = "Moheshkhali-Kutubdia Coastal Embankment Protection Milestone",
            eventDateIso = "2027-01-15",
            dateDisplayBn = "১৫ জানুয়ারি, ২০২৭",
            dateDisplayEn = "15 January 2027",
            categoryBn = "স্থানীয় উন্নয়ন দিবস",
            categoryEn = "Local Milestone",
            descriptionBn = "উপকূলীয় জোয়ার ও জলোচ্ছ্বাস থেকে দ্বীপবাসীকে রক্ষায় স্থায়ী উঁচু সিসি ব্লক বেড়িবাঁধ সুরক্ষা প্রকল্প।",
            descriptionEn = "Permanent climate-resilient CC-block coastal embankment protection.",
            locationOrTagBn = "মহেশখালী-কুতুবদিয়া",
            themeType = "LOCAL_DEV",
            isFeaturedCountdown = false
        )
    )
    private val _specialEvents = MutableStateFlow(defaultSpecialEvents)
    val specialEvents: StateFlow<List<SpecialEventItem>> = _specialEvents.asStateFlow()

    // 9. Emergency Contacts (Synced with Firestore collection: emergency_contacts)
    val defaultEmergencyContacts = listOf(
        EmergencyContact("E1", "মহেশখালী থানা (ডিউটি অফিসার)", "Moheshkhali Police Station", "POLICE", "01713373671", "01320108350", "গোরকঘাটা, মহেশখালী", "Gorakghata, Moheshkhali"),
        EmergencyContact("E2", "কুতুবদিয়া থানা (ডিউটি অফিসার)", "Kutubdia Police Station", "POLICE", "01713373672", "01320108365", "বড়ঘোপ, কুতুবদিয়া", "Baraghop, Kutubdia"),
        EmergencyContact("E3", "মহেশখালী উপজেলা স্বাস্থ্য কমপ্লেক্স", "Moheshkhali Health Complex", "HOSPITAL", "01730324785", null, "হাসপাতাল রোড, মহেশখালী", "Hospital Road, Moheshkhali"),
        EmergencyContact("E4", "কুতুবদিয়া উপজেলা স্বাস্থ্য কমপ্লেক্স", "Kutubdia Health Complex", "HOSPITAL", "01730324786", null, "ধুরুং বাজার সড়ক, কুতুবদিয়া", "Dhurung Bazar Rd, Kutubdia"),
        EmergencyContact("E5", "ফায়ার সার্ভিস ও সিভিল ডিফেন্স মহেশখালী", "Fire Service Moheshkhali", "FIRE_SERVICE", "01725555412", "01901020304", "নতুন ব্রিজ মোড়, মহেশখালী", "New Bridge, Moheshkhali"),
        EmergencyContact("E6", "বাংলাদেশ কোস্ট গার্ড (উপকূলীয় কন্ট্রোল)", "Bangladesh Coast Guard Control", "COAST_GUARD", "01766690777", null, "উপকূলীয় পর্যবেক্ষণ কেন্দ্র", "Coastal Command Base"),
        EmergencyContact("E7", "ঘূর্ণিঝড় প্রস্তুতি কর্মসূচি (সিপিপি কুতুবদিয়া)", "Cyclone Preparedness Programme (CPP)", "CPP", "01712000000", null, "উপজেলা নিয়ন্ত্রণ কক্ষ", "Disaster Emergency Cell"),
        EmergencyContact("E8", "জাতীয় জরুরি সেবা (পুলিশ, অ্যাম্বুলেন্স, ফায়ার)", "National Emergency Service (999)", "NATIONAL", "999", null, "টোল ফ্রি ২৪ ঘণ্টা", "Toll Free 24/7"),
        EmergencyContact("E9", "দুর্যোগের আগাম বার্তা ও সংকেত (১০৯০)", "Disaster Warning Hotline (1090)", "DISASTER", "1090", null, "টোল ফ্রি আবহাওয়া ও জলোচ্ছ্বাস বার্তা", "Toll Free 24/7 Warning"),
        EmergencyContact("E10", "জাতীয় তথ্য ও নাগরিক সেবা (৩৩৩)", "National Citizen Helpdesk (333)", "NATIONAL", "333", null, "সরকারি তথ্য, সেবা ও অভিযোগ", "Government Services & Info")
    )
    private val _emergencyContacts = MutableStateFlow(defaultEmergencyContacts)
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()

    // 10. Blood Donors (Synced with Firestore collection: blood_donors)
    val defaultBloodDonors = listOf(
        BloodDonor(id = "B1", nameBn = "মোহাম্মদ রাশেদুল ইসলাম", nameEn = "Md. Rashedul Islam", bloodGroup = "O+", upazila = "Moheshkhali", areaBn = "গোরকঘাটা সদর", areaEn = "Gorakghata Sadar", phone = "01819123456", lastDonatedBn = "২ মাস আগে", lastDonatedEn = "2 months ago", isAvailable = true),
        BloodDonor(id = "B2", nameBn = "তানভীর আহমেদ সাকিব", nameEn = "Tanveer Ahmed Sakib", bloodGroup = "A+", upazila = "Moheshkhali", areaBn = "ছোট মহেশখালী", areaEn = "Chhota Moheshkhali", phone = "01822334455", lastDonatedBn = "৩ মাস আগে", lastDonatedEn = "3 months ago", isAvailable = true),
        BloodDonor(id = "B3", nameBn = "কাজী মোহাম্মদ নুরুল হুদা", nameEn = "Kazi Nurul Huda", bloodGroup = "B+", upazila = "Kutubdia", areaBn = "বড়ঘোপ ইউনিয়ন", areaEn = "Baraghop Union", phone = "01711223344", lastDonatedBn = "৪ মাস আগে", lastDonatedEn = "4 months ago", isAvailable = true),
        BloodDonor(id = "B4", nameBn = "সৈয়দ আবদুল্লাহ আল মামুন", nameEn = "Syed Abdullah Mamun", bloodGroup = "AB+", upazila = "Kutubdia", areaBn = "আলী আকবর ডেইল", areaEn = "Ali Akbar Deil", phone = "01844556677", lastDonatedBn = "১ মাস আগে", lastDonatedEn = "1 month ago", isAvailable = true)
    )
    private val _bloodDonors = MutableStateFlow(defaultBloodDonors)
    val bloodDonors: StateFlow<List<BloodDonor>> = _bloodDonors.asStateFlow()

    // 11. Notices (Synced with Firestore collection: notices)
    val defaultNotices = listOf(
        NoticeFeedItem("N1", "কুতুবদিয়া-মহেশখালী নৌ-রুটে লাইফ জ্যাকেট ব্যবহার বাধ্যতামূলক", "Mandatory Life Jacket Usage on Sea Routes", "২৮ অক্টোবর, ২০২৬", "Oct 28, 2026", "নৌ-নিরাপত্তা", "Maritime Safety", "সকল স্পিডবোট ও সী-ট্রাক যাত্রার পূর্বে লাইফ জ্যাকেট পরিধান আবশ্যক।", "Mandatory life jacket usage for all passengers.", true),
        NoticeFeedItem("N2", "মাতারবাড়ি গভীর সমুদ্র বন্দর ও অর্থনৈতিক অঞ্চলে যুব প্রশিক্ষণ ক্যাম্প", "Youth Training Camp for Matarbari Port & EZ", "২৫ অক্টোবর, ২০২৬", "Oct 25, 2026", "কর্মসংস্থান", "Employment", "স্থানীয় তরুণদের বন্দর পরিচালনা ও টেকনিক্যাল স্কিলে বিশেষ প্রশিক্ষণ।", "Technical training program for local youth.", false)
    )
    private val _notices = MutableStateFlow(defaultNotices)
    val notices: StateFlow<List<NoticeFeedItem>> = _notices.asStateFlow()

    // 11.1 FAQs (Synced with Firestore collection: faqs)
    val defaultFaqs = listOf(
        FaqItem("faq1", "কিভাবে এমপি মহোদয়ের সাথে সরাসরি সাক্ষাত করব?", "How to meet the MP?", "অ্যাপের 'যোগাযোগ' পাতা থেকে 'আবেদন ও সাক্ষাতের ফর্ম' পূরণ করুন। ফর্ম জমা দেওয়ার পর একটি ট্র্যাকিং আইডি পাবেন। আবেদন পর্যালোচনা করে নির্ধারিত তারিখ ও সময় ট্র্যাকিং আইডির মাধ্যমে বা মোবাইলে জানিয়ে দেওয়া হবে।", "Fill out the 'Petition & Appointment Form' on the Contact page. Submit to receive a Tracking ID and monitor status for scheduled time.", order = 1),
        FaqItem("faq2", "আবেদনের কতদিনের মধ্যে সাড়া পাওয়া যায়?", "Response time for petitions?", "সাধারণত ৩-৭ কার্যদিবসের মধ্যে আপনার আবেদনের পর্যালোচনা সম্পন্ন করা হয়।", "Petitions are typically reviewed within 3-7 working days.", order = 2),
        FaqItem("faq3", "মহেশখালী ও কুতুবদিয়া প্রধান কার্যালয় কোথায় অবস্থিত?", "Where are the offices located?", "মহেশখালী কার্যালয়: নতুন বাজার রোড, মহেশখালী পৌরসভা। কুতুবদিয়া কার্যালয়: বড়ঘোপ ইউনিয়ন পরিষদ রোড, কুতুবদিয়া, কক্সবাজার।", "Moheshkhali Office: Notun Bazar Road, Moheshkhali Municipality. Kutubdia Office: Boroghop Union Parishad Road, Kutubdia.", order = 3),
        FaqItem("faq4", "কোনো সমস্যা বা উন্নয়ন প্রস্তাব কোথায় পাঠাব?", "Where to send development proposals?", "আমাদের যোগাযোগ পাতার বার্তা বিভাগে 'উন্নয়ন প্রস্তাব' ক্যাটাগরি বেছে নিয়ে সচিত্র বিবরণ পাঠাতে পারেন।", "Select 'Development Proposal' in the message category on the Contact page to submit local issues and proposals.", order = 4)
    )
    private val _faqs = MutableStateFlow(defaultFaqs)
    val faqs: StateFlow<List<FaqItem>> = _faqs.asStateFlow()

    // 12. Citizen Grievances & Feedback Petitions (Synced with Firestore collection: citizen_grievances)
    private val _citizenGrievances = MutableStateFlow<List<CitizenGrievance>>(emptyList())
    val citizenGrievances: StateFlow<List<CitizenGrievance>> = _citizenGrievances.asStateFlow()

    // 13. Digital Services & CMS Modules
    private val _digitalServices = MutableStateFlow(DefaultPortalContent.defaultDigitalServices)
    val digitalServices: StateFlow<List<DigitalServiceItem>> = _digitalServices.asStateFlow()

    private val _educationResources = MutableStateFlow(DefaultPortalContent.defaultEducationResources)
    val educationResources: StateFlow<List<EducationResourceItem>> = _educationResources.asStateFlow()

    private val _economyOpportunities = MutableStateFlow(DefaultPortalContent.defaultEconomyOpportunities)
    val economyOpportunities: StateFlow<List<EconomyOpportunityItem>> = _economyOpportunities.asStateFlow()

    private val _welfarePrograms = MutableStateFlow(DefaultPortalContent.defaultWelfarePrograms)
    val welfarePrograms: StateFlow<List<WelfareProgramItem>> = _welfarePrograms.asStateFlow()

    private val _unionServices = MutableStateFlow(DefaultPortalContent.defaultUnionServices)
    val unionServices: StateFlow<List<UnionServiceHub>> = _unionServices.asStateFlow()

    private val _developmentCategories = MutableStateFlow(DefaultPortalContent.defaultDevelopmentCategories)
    val developmentCategories: StateFlow<List<DevelopmentCategoryItem>> = _developmentCategories.asStateFlow()

    private val _homepageCategories = MutableStateFlow(DefaultPortalContent.defaultHomepageCategories)
    val homepageCategories: StateFlow<List<HomepageCategoryItem>> = _homepageCategories.asStateFlow()

    private val _citizenPoll = MutableStateFlow(DefaultPortalContent.defaultCitizenPoll)
    val citizenPoll: StateFlow<CitizenPollItem> = _citizenPoll.asStateFlow()

    private val _userVotedOption = MutableStateFlow<Int?>(null)
    val userVotedOption: StateFlow<Int?> = _userVotedOption.asStateFlow()

    private val _pollVotes = MutableStateFlow(rebuildPollVotes(DefaultPortalContent.defaultCitizenPoll, null))
    val pollVotes: StateFlow<Map<Int, Int>> = _pollVotes.asStateFlow()

    private fun rebuildPollVotes(poll: CitizenPollItem, userVote: Int?): Map<Int, Int> {
        val base = poll.options.mapIndexed { idx, opt -> idx to opt.votes }.toMap().toMutableMap()
        if (userVote != null && userVote in poll.options.indices) {
            base[userVote] = (base[userVote] ?: 0) + 1
        }
        return base
    }

    // 13.1 Union Representatives (Synced with Firestore collection: union_representatives)
    val defaultUnionRepresentatives = listOf(
        UnionRepresentative("u1", "মহেশখালী", "মাতারবাড়ি ইউনিয়ন", "Matarbari Union", "মো. সরওয়ার আলম", "01811111111", "মো. জয়নাল আবেদীন", "01822222222", 1),
        UnionRepresentative("u2", "মহেশখালী", "ধলঘাটা ইউনিয়ন", "Dhalghata Union", "কামরুল ইসলাম", "01811111112", "মো. রফিকুল ইসলাম", "01822222223", 2),
        UnionRepresentative("u3", "মহেশখালী", "কালারমারছড়া ইউনিয়ন", "Kalarmarchhara Union", "তারেক বিন ওসমান", "01811111113", "আব্দুল মান্নান", "01822222224", 3),
        UnionRepresentative("u4", "মহেশখালী", "শাপলাপুর ইউনিয়ন", "Shaplapur Union", "আব্দুল খালেক", "01811111114", "প্রদীপ কুমার দে", "01822222225", 4),
        UnionRepresentative("u5", "মহেশখালী", "হোয়ানক ইউনিয়ন", "Hoanak Union", "মো. মোস্তফা কামাল", "01811111115", "মো. ইউনুস", "01822222226", 5),
        UnionRepresentative("u6", "মহেশখালী", "বড় মহেশখালী ইউনিয়ন", "Boror Moheshkhali Union", "এনাম মিয়া", "01811111116", "অনুপম বড়ুয়া", "01822222227", 6),
        UnionRepresentative("u7", "মহেশখালী", "ছোট মহেশখালী ইউনিয়ন", "Choto Moheshkhali Union", "জিহাদ বিন আলী", "01811111117", "মোহাম্মদ ইসমাইল", "01822222228", 7),
        UnionRepresentative("u8", "মহেশখালী", "কুতুবজোম ইউনিয়ন", "Kutubjom Union", "শেখ কামাল", "01811111118", "মো. কাইয়ুম", "01822222229", 8),
        UnionRepresentative("u9", "কুতুবদিয়া", "বড়ঘোপ ইউনিয়ন", "Boroghop Union", "আ.ন.ম শহীদুল ইসলাম", "01833333331", "রতন কান্তি দাশ", "01844444441", 9),
        UnionRepresentative("u10", "কুতুবদিয়া", "আলী আকবর ডেইল ইউনিয়ন", "Ali Akbar Deil Union", "জাহেদুল ইসলাম", "01833333332", "মোহাম্মদ আলী", "01844444442", 10),
        UnionRepresentative("u11", "কুতুবদিয়া", "কৈয়ারবিল ইউনিয়ন", "Koiyarbill Union", "আজমগীর হায়দার", "01833333333", "মো. জাবেদ", "01844444443", 11),
        UnionRepresentative("u12", "কুতুবদিয়া", "লেমশীখালী ইউনিয়ন", "Lemshikhali Union", "আকতার হোসাইন", "01833333334", "সুজন বড়ুয়া", "01844444444", 12),
        UnionRepresentative("u13", "কুতুবদিয়া", "উত্তর ধুরুং ইউনিয়ন", "Uttar Dhurung Union", "আছাদ উল্লাহ", "01833333335", "মো. ইসমাইল", "01844444445", 13),
        UnionRepresentative("u14", "কুতুবদিয়া", "দক্ষিণ ধুরুং ইউনিয়ন", "Dakkhin Dhurung Union", "আলাউদ্দিন", "01833333336", "বিধান চন্দ্র দে", "01844444446", 14)
    )
    private val _unionRepresentatives = MutableStateFlow(defaultUnionRepresentatives)
    val unionRepresentatives: StateFlow<List<UnionRepresentative>> = _unionRepresentatives.asStateFlow()

    // 14. Daily Schedule
    val defaultDailySchedules = listOf(
        DailyScheduleItem(
            id = "sched_1",
            eventTitleBn = "মাতারবাড়ি গভীর সমুদ্রবন্দর ও মেগা সংযোগ সড়ক ক্ষেত্র পরিদর্শন",
            eventTitleEn = "Inspection of Matarbari Deep Sea Port & Access Road Site",
            categoryBn = "উন্নয়ন পরিদর্শন",
            categoryEn = "Development Inspection",
            eventDateBn = "১০ নভেম্বর, ২০২৬",
            eventDateEn = "Nov 10, 2026",
            timeBn = "সকাল ১০:৩০ মি.",
            timeEn = "10:30 AM",
            locationBn = "মাতারবাড়ি, মহেশখালী",
            locationEn = "Matarbari, Moheshkhali",
            statusBn = "নির্ধারিত",
            statusEn = "Scheduled",
            timestamp = 1794217800000L
        ),
        DailyScheduleItem(
            id = "sched_2",
            eventTitleBn = "উপকূলীয় দুর্যোগ ব্যবস্থাপনা ও দ্বীপাঞ্চলের জনগণের সাথে মতবিনিময়",
            eventTitleEn = "Coastal Disaster Relief & Community Engagement Meeting",
            categoryBn = "জনসভা ও গণসংযোগ",
            categoryEn = "Public Meetings & Engagement",
            eventDateBn = "১২ নভেম্বর, ২০২৬",
            eventDateEn = "Nov 12, 2026",
            timeBn = "বিকাল ০৩:৩০ মি.",
            timeEn = "03:30 PM",
            locationBn = "দরবার ঘাট চত্বর, কুতুবদিয়া",
            locationEn = "Darbar Ghat, Kutubdia",
            statusBn = "নির্ধারিত",
            statusEn = "Scheduled",
            timestamp = 1794390600000L
        ),
        DailyScheduleItem(
            id = "sched_3",
            eventTitleBn = "কুতুবদিয়া সাবমেরিন ক্যাবল বিদ্যুৎ সংযোগ ও সুপেয় পানি প্রকল্পের অগ্রগতি তদারকি",
            eventTitleEn = "Kutubdia Submarine Electricity & Safe Water Supply Progress Monitoring",
            categoryBn = "জনসেবা সম্পর্কিত হালনাগাদ",
            categoryEn = "Public Service Updates",
            eventDateBn = "১৫ নভেম্বর, ২০২৬",
            eventDateEn = "Nov 15, 2026",
            timeBn = "সকাল ১১:০০ মি.",
            timeEn = "11:00 AM",
            locationBn = "ধলঘাটা ও কুতুবদিয়া",
            locationEn = "Dhalghata & Kutubdia",
            statusBn = "চলমান",
            statusEn = "In Progress",
            timestamp = 1794649200000L
        )
    )
    private val _dailySchedules = MutableStateFlow(defaultDailySchedules)
    val dailySchedules: StateFlow<List<DailyScheduleItem>> = _dailySchedules.asStateFlow()

    // 14.1 Government Reports & Gazettes (Synced with Firestore collection: government_reports)
    val defaultGovtReports = listOf(
        GovtReportItem(
            id = "rep_1",
            titleBn = "মাতারবাড়ি আল্ট্রা সুপার ক্রিটিক্যাল বিদ্যুৎ প্রকল্প জাতীয় গেজেট ও মূল্যায়ন প্রতিবেদন",
            titleEn = "Matarbari Ultra Super Critical Power Project National Gazette & Evaluation",
            categoryBn = "সরকারি গেজেট",
            categoryEn = "Official Gazette",
            publishDateBn = "১২ অক্টোবর, ২০২৪",
            publishDateEn = "Oct 12, 2024",
            year = 2024,
            pdfUrl = "https://cpgcbl.gov.bd/report_matarbari_power.pdf",
            mediaUrl = "drawable:moheshkhali_banner",
            mediaType = "PDF",
            ministryOrDeptBn = "বিদ্যুৎ, জ্বালানি ও খনিজ সম্পদ মন্ত্রণালয়",
            ministryOrDeptEn = "Ministry of Power, Energy & Mineral Resources",
            fileSizeMb = "4.2 MB",
            downloadCount = 1420
        ),
        GovtReportItem(
            id = "rep_2",
            titleBn = "কুতুবদিয়া দ্বীপে সাবমেরিন ক্যাবল বিদ্যুৎ সংযোগ সমাপ্তি ও সুফলভোগী সমীক্ষা",
            titleEn = "Kutubdia Island Submarine Cable Electrification Completion Survey",
            categoryBn = "প্রকল্প প্রতিবেদন",
            categoryEn = "Project Report",
            publishDateBn = "১৫ আগস্ট, ২০২৪",
            publishDateEn = "Aug 15, 2024",
            year = 2024,
            pdfUrl = "https://bpdb.gov.bd/kutubdia_cable_survey.pdf",
            mediaUrl = "drawable:moheshkhali_banner",
            mediaType = "PDF",
            ministryOrDeptBn = "বাংলাদেশ বিদ্যুৎ উন্নয়ন বোর্ড (বিপিডিবি)",
            ministryOrDeptEn = "Bangladesh Power Development Board (BPDB)",
            fileSizeMb = "3.1 MB",
            downloadCount = 980
        ),
        GovtReportItem(
            id = "rep_3",
            titleBn = "মহেশখালী বিশেষ অর্থনৈতিক অঞ্চল (ইজেড) মহাপরিকল্পনা ও পরিবেশ সমীক্ষা গেজেট",
            titleEn = "Moheshkhali Special Economic Zone (SEZ) Master Plan & Environmental Gazette",
            categoryBn = "পরিবেশ সমীক্ষা",
            categoryEn = "Environmental Survey",
            publishDateBn = "২২ নভেম্বর, ২০২৩",
            publishDateEn = "Nov 22, 2023",
            year = 2023,
            pdfUrl = "https://beza.gov.bd/moheshkhali_sez_masterplan.pdf",
            mediaUrl = "drawable:moheshkhali_banner",
            mediaType = "PDF",
            ministryOrDeptBn = "বাংলাদেশ অর্থনৈতিক অঞ্চল কর্তৃপক্ষ (বেজা)",
            ministryOrDeptEn = "Bangladesh Economic Zones Authority (BEZA)",
            fileSizeMb = "5.8 MB",
            downloadCount = 2150
        ),
        GovtReportItem(
            id = "rep_4",
            titleBn = "কুতুবদিয়া ও মহেশখালী উপকূলীয় সিসি ব্লক বেড়িবাঁধ সুরক্ষা প্রযুক্তিগত নিরীক্ষা",
            titleEn = "Coastal CC Block Embankment Technical Audit Report",
            categoryBn = "অডিট ও মূল্যায়ন",
            categoryEn = "Audit & Evaluation",
            publishDateBn = "০৫ জুলাই, ২০২৩",
            publishDateEn = "Jul 05, 2023",
            year = 2023,
            pdfUrl = "https://bwdb.gov.bd/coastal_embankment_audit.pdf",
            mediaUrl = "drawable:moheshkhali_banner",
            mediaType = "PDF",
            ministryOrDeptBn = "বাংলাদেশ পানি উন্নয়ন বোর্ড (পাউবো)",
            ministryOrDeptEn = "Bangladesh Water Development Board (BWDB)",
            fileSizeMb = "2.9 MB",
            downloadCount = 870
        )
    )
    private val _govtReports = MutableStateFlow(defaultGovtReports)
    val govtReports: StateFlow<List<GovtReportItem>> = _govtReports.asStateFlow()

    // 15. Leader Profile Info (Synced with Firestore collection: profile_info, doc: main)
    private val _profileInfo = MutableStateFlow(ProfileInfo())
    val profileInfo: StateFlow<ProfileInfo> = _profileInfo.asStateFlow()

    init {
        // Restore persistent media files from local internal storage if available
        context?.let { ctx ->
            val localBannerVideo = MediaStorageManager.getPersistedFile(ctx, MediaStorageManager.BANNER_VIDEO_NAME)
            val localBannerPoster = MediaStorageManager.getPersistedFile(ctx, MediaStorageManager.BANNER_POSTER_NAME)
            val localProfileAvatar = MediaStorageManager.getPersistedFile(ctx, MediaStorageManager.PROFILE_AVATAR_NAME)

            if (localBannerVideo != null || localBannerPoster != null || localProfileAvatar != null) {
                _dynamicMediaAssets.update { curr ->
                    curr.copy(
                        bannerVideoSource = localBannerVideo?.absolutePath ?: curr.bannerVideoSource,
                        bannerPosterSource = localBannerPoster?.absolutePath ?: curr.bannerPosterSource,
                        profilePictureSource = localProfileAvatar?.absolutePath ?: curr.profilePictureSource
                    )
                }
                if (localBannerPoster != null) {
                    _appConfig.update { it.copy(heroImageUrl = localBannerPoster.absolutePath) }
                }
                if (localProfileAvatar != null) {
                    _appConfig.update { it.copy(avatarImageUrl = localProfileAvatar.absolutePath) }
                    _profileInfo.update { it.copy(photoUrl = localProfileAvatar.absolutePath) }
                }
            }
        }

        // Start public CMS Firestore real-time observation
        setupFirestoreListeners()
    }

    fun verifyAdminRole(onResult: (isAdmin: Boolean, role: String?) -> Unit) {
        val user = auth?.currentUser
        if (user == null) {
            currentAdminRole = null
            canEditCmsContent = false
            canManageApplications = false
            onResult(false, null)
            return
        }

        val db = firestore
        if (db == null) {
            currentAdminRole = null
            canEditCmsContent = false
            canManageApplications = false
            onResult(false, null)
            return
        }

        db.collection("admins").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val active = doc.getBoolean("active") == true
                val role = doc.getString("role")?.trim()
                if (doc.exists() && active && role != null && role in ADMIN_ROLES) {
                    currentAdminRole = role
                    canEditCmsContent = role in CONTENT_ADMIN_ROLES
                    canManageApplications = role in APP_MANAGER_ROLES
                    onResult(true, role)
                } else {
                    currentAdminRole = null
                    canEditCmsContent = false
                    canManageApplications = false
                    onResult(false, null)
                }
            }
            .addOnFailureListener { e ->
                Log.w("PortalRepository", "Admin role verification failed: ${e.message}")
                currentAdminRole = null
                canEditCmsContent = false
                canManageApplications = false
                onResult(false, null)
            }
    }

    fun refreshProtectedListeners(uid: String?, isAdmin: Boolean, adminRole: String?) {
        applicationsListener?.remove()
        applicationsListener = null
        appointmentsListener?.remove()
        appointmentsListener = null
        grievancesListener?.remove()
        grievancesListener = null
        pollVoteListener?.remove()
        pollVoteListener = null

        currentAuthUid = uid
        val verifiedRole = if (isAdmin && adminRole in ADMIN_ROLES) adminRole else null
        currentAdminRole = verifiedRole
        canEditCmsContent = verifiedRole in CONTENT_ADMIN_ROLES
        canManageApplications = verifiedRole in APP_MANAGER_ROLES

        val db = firestore
        if (uid.isNullOrBlank() || db == null) {
            _appointmentRequests.value = emptyList()
            _citizenGrievances.value = emptyList()
            _userVotedOption.value = null
            _pollVotes.value = rebuildPollVotes(_citizenPoll.value, null)
            return
        }

        setupAppointmentListener(db, canManageApplications, uid)
        setupGrievancesListener(db, canManageApplications, uid)
        setupPollVoteListener(db, uid)
    }

    private fun setupGrievancesListener(db: FirebaseFirestore, isAppAdmin: Boolean, uid: String) {
        val query = if (isAppAdmin) {
            db.collection("citizen_grievances")
        } else {
            db.collection("citizen_grievances").whereEqualTo("userId", uid)
        }
        grievancesListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("PortalRepository", "Grievances listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    FirestoreParsers.parseGrievance(doc)
                }
                _citizenGrievances.value = list
            }
        }
    }

    private fun setupFirestoreListeners() {
        val db = firestore ?: return

        // 1. Observe Profile Info (Collection: profile_info, Document: main)
        db.collection("profile_info").document("main")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Profile info listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val info = FirestoreParsers.safeToObject(snapshot, ProfileInfo::class.java)
                    if (info != null) {
                        _profileInfo.value = info
                    }
                }
            }

        // 2. Observe App Config
        db.collection("config").document("app")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Config listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val config = FirestoreParsers.safeToObject(snapshot, AppConfig::class.java)
                    if (config != null) {
                        _appConfig.value = config
                    }
                }
            }

        // Published packages are public; drafts remain hidden by Firestore Rules.
        db.collection("app_releases")
            .whereEqualTo("status", "published")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "App release listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val release = snapshot?.documents
                    ?.mapNotNull { document ->
                        val versionCode = document.getLong("versionCode")?.toInt()
                            ?: return@mapNotNull null
                        val downloadUrl = document.getString("downloadUrl").orEmpty()
                        val downloadUri = Uri.parse(downloadUrl)
                        if (
                            versionCode <= BuildConfig.VERSION_CODE ||
                            downloadUri.scheme != "https" ||
                            downloadUri.host != "firebasestorage.googleapis.com"
                        ) {
                            return@mapNotNull null
                        }
                        AppRelease(
                            versionCode = versionCode,
                            versionName = document.getString("versionName").orEmpty(),
                            releaseNotesBn = document.getString("releaseNotesBn").orEmpty(),
                            releaseNotesEn = document.getString("releaseNotesEn").orEmpty(),
                            downloadUrl = downloadUrl,
                            status = "published"
                        )
                    }
                    ?.maxByOrNull { it.versionCode }
                _latestAppRelease.value = release
            }

        // 3. Observe Dynamic Filter Chips
        db.collection("filter_chips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Filter chips listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val chips = snapshot.documents.mapNotNull { doc ->
                        FirestoreParsers.parseFilterChip(doc)
                    }.filter { it.isPublished }.sortedBy { it.order }
                    _filterChips.value = chips
                }
            }

        // 3.1 Media Archive & Portal CMS Listeners
        setupMediaArchiveListener(db)
        setupGovtReportsListener(db)
        setupSocialMediaListener(db)
        setupTourismSpotsListener(db)
        setupCmsModuleListeners(db)

        // 4. Observe Development Projects (Auto-seed if empty)
        db.collection("development_projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Projects listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultDevelopmentProjects.forEach { proj ->
                                        val doc = db.collection("development_projects").document(proj.id)
                                        batch.set(doc, proj)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed default projects: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val projects = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseDevelopmentProject(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _developmentProjects.value = projects
                    }
                }
            }

        // 5. Observe Gallery Items (Auto-seed if empty & content admin)
        db.collection("gallery_items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Gallery listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultGalleryItems.forEach { g ->
                                        val doc = db.collection("gallery_items").document(g.id)
                                        batch.set(doc, g)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed gallery: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val items = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseGalleryItem(doc)
                        }.filter { it.isPublished }
                        _galleryItems.value = items
                    }
                }
            }

        // 8. Observe Digital Services (Auto-seed if empty & content admin)
        db.collection("digital_services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Digital services listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    DefaultPortalContent.defaultDigitalServices.forEach { item ->
                                        batch.set(db.collection("digital_services").document(item.id), item)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed digital services: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseDigitalService(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _digitalServices.value = list
                    }
                }
            }

        // 9. Observe Daily Schedules
        db.collection("daily_schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Daily schedules listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultDailySchedules.forEach { s ->
                                        val doc = db.collection("daily_schedules").document(s.id)
                                        batch.set(doc, s)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed daily schedules: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseDailySchedule(doc)
                        }.filter { it.isPublished }.sortedBy { it.timestamp }
                        _dailySchedules.value = list
                    }
                }
            }

        // 7. Observe Dynamic Watercraft Schedules (Auto-seed if empty & content admin)
        db.collection("watercraft_schedules")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Watercraft schedules listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultWatercraftSchedules.forEach { s ->
                                        val doc = db.collection("watercraft_schedules").document(s.id)
                                        batch.set(doc, s)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed schedules: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseWatercraftSchedule(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _dynamicWatercraftSchedules.value = list
                    }
                }
            }

        // 8. Observe Dynamic Tide Schedule
        db.collection("tide_schedules").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Tide schedule listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    FirestoreParsers.safeToObject(snapshot, DynamicTideSchedule::class.java)?.let {
                        _dynamicTideSchedule.value = it
                    }
                } else if (snapshot != null && !snapshot.exists() && canEditCmsContent) {
                    db.collection("tide_schedules").document("current").set(DynamicTideSchedule())
                }
            }

        // 8.1 Observe Sea Truck Updates
        db.collection("sea_truck_updates").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Sea truck updates listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    FirestoreParsers.safeToObject(snapshot, SeaTruckUpdate::class.java)?.let {
                        _seaTruckUpdate.value = it
                    }
                }
            }

        // 9. Observe Dynamic Media Assets
        db.collection("media_assets").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Media assets listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    FirestoreParsers.safeToObject(snapshot, DynamicMediaAssets::class.java)?.let { assets ->
                        _dynamicMediaAssets.value = assets
                        if (assets.bannerPosterSource.isNotBlank()) {
                            _appConfig.update { it.copy(heroImageUrl = assets.bannerPosterSource) }
                        }
                        if (assets.profilePictureSource.isNotBlank()) {
                            _appConfig.update { it.copy(avatarImageUrl = assets.profilePictureSource) }
                            _profileInfo.update { it.copy(photoUrl = assets.profilePictureSource) }
                        }
                    }
                } else if (snapshot != null && !snapshot.exists() && canEditCmsContent) {
                    db.collection("media_assets").document("current").set(DynamicMediaAssets())
                }
            }

        setupFaqListener(db)
        setupUnionRepresentativeListener(db)

        // 10. Observe Notices (Auto-seed if empty & content admin)
        db.collection("notices")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Notices listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultNotices.forEach { n ->
                                        val doc = db.collection("notices").document(n.id)
                                        batch.set(doc, noticeToMap(n))
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed notices: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseNotice(doc)
                        }.filter { it.isPublished }.sortedByDescending { it.createdAt }
                        _notices.value = list
                    }
                }
            }

        // 11. Observe Emergency Contacts (Auto-seed if empty & content admin)
        db.collection("emergency_contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Emergency contacts listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultEmergencyContacts.forEach { c ->
                                        val doc = db.collection("emergency_contacts").document(c.id)
                                        batch.set(doc, c)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed emergency contacts: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseEmergencyContact(doc)
                        }.filter { it.isPublished }
                        _emergencyContacts.value = list
                    }
                }
            }

        // 12. Observe Blood Donors (Auto-seed if empty & content admin)
        db.collection("blood_donors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Blood donors listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultBloodDonors.forEach { b ->
                                        val doc = db.collection("blood_donors").document(b.id)
                                        batch.set(doc, b)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed blood donors: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseBloodDonor(doc)
                        }.filter { it.isPublished }
                        _bloodDonors.value = list
                    }
                }
            }

        // 13. Observe Weather Data (Document: current)
        db.collection("weather_data").document("current")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Weather listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    FirestoreParsers.safeToObject(snapshot, WeatherData::class.java)?.let {
                        _weatherData.value = it
                    }
                } else if (snapshot != null && !snapshot.exists() && canEditCmsContent) {
                    db.collection("weather_data").document("current").set(WeatherData())
                }
            }

        // 14. Observe Important External Links (Auto-seed if empty & content admin)
        db.collection("important_links")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Important links listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultImportantLinks.forEach { l ->
                                        val doc = db.collection("important_links").document(l.id)
                                        batch.set(doc, l)
                                    }
                                    batch.commit()
                                } catch (e: Exception) {
                                    Log.w("PortalRepository", "Failed to seed links: ${e.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseImportantLink(doc)
                        }.filter { it.isPublished }
                        _importantLinks.value = list
                    }
                }
            }

        // 15. Observe Special Days & Events Ticker (Auto-seed if empty & content admin)
        db.collection("special_events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Special events listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultSpecialEvents.forEach { e ->
                                        val doc = db.collection("special_events").document(e.id)
                                        batch.set(doc, e)
                                    }
                                    batch.commit()
                                } catch (err: Exception) {
                                    Log.w("PortalRepository", "Failed to seed special events: ${err.message}")
                                }
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseSpecialEvent(doc)
                        }.filter { it.isPublished }
                        _specialEvents.value = list
                    }
                }
            }
    }

    // Submit Citizen Grievance & Petition to Firestore
    fun submitCitizenGrievance(
        fullName: String,
        phone: String,
        upazila: String,
        subject: String,
        details: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid.isNullOrBlank()) {
            onError("Authentication required to submit grievance")
            return
        }

        val grievance = CitizenGrievance(
            id = "G_${System.currentTimeMillis()}",
            userId = uid ?: "local_preview",
            fullName = fullName,
            phone = phone,
            upazila = upazila,
            subject = subject,
            details = details,
            status = "Pending",
            createdAtFormatted = "আজ"
        )

        if (db != null) {
            db.collection("citizen_grievances").document(grievance.id)
                .set(grievance)
                .addOnSuccessListener {
                    _citizenGrievances.update { listOf(grievance) + it.filterNot { item -> item.id == grievance.id } }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Grievance write failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save")
                }
        } else {
            _citizenGrievances.update { listOf(grievance) + it }
            onSuccess()
        }
    }

    fun refreshWeatherFromCloud(
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val db = firestore
        if (db == null) {
            onSuccess()
            return
        }
        db.collection("weather_data").document("current").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && snapshot.exists()) {
                    FirestoreParsers.safeToObject(snapshot, WeatherData::class.java)?.let {
                        _weatherData.value = it
                    }
                }
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w("PortalRepository", "Weather cloud refresh failed: ${e.message}")
                onError(e.localizedMessage ?: "Failed to refresh weather")
            }
    }

    // --- Weather & Maritime Signals CRUD ---
    fun updateWeatherData(
        weather: WeatherData,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _weatherData.value = weather
        val db = firestore
        if (db != null) {
            db.collection("weather_data").document("current")
                .set(weather)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Weather update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update weather")
                }
        } else {
            onSuccess()
        }
    }

    fun updateWeatherMaritimeSignal(signal: Int, descBn: String, descEn: String) {
        val updated = _weatherData.value.copy(
            maritimeSignal = signal,
            signalDescBn = descBn,
            signalDescEn = descEn,
            lastUpdatedBn = "এখনই হালনাগাদকৃত",
            lastUpdatedEn = "Just updated"
        )
        updateWeatherData(updated)
    }

    fun updateSeaTruckNotice(
        noticeBn: String,
        noticeEn: String,
        isNormal: Boolean,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val updated = _seaTruckUpdate.value.copy(
            noticeBn = noticeBn,
            noticeEn = noticeEn,
            generalStatusBn = if (isNormal) "চলাচল স্বাভাবিক" else "সতর্কতা: চলাচল নিয়ন্ত্রিত",
            generalStatusEn = if (isNormal) "Service Operational" else "Notice: Restricted Operations"
        )
        _seaTruckUpdate.value = updated
        val db = firestore
        if (db != null) {
            db.collection("sea_truck_updates").document("current")
                .set(updated)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Sea truck update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update sea-truck notice")
                }
        } else {
            onSuccess()
        }
    }

    // --- Development Projects CRUD ---
    fun saveDevelopmentProject(
        project: DevelopmentProject,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (project.id.isNotBlank()) project.id else "PROJ_${System.currentTimeMillis()}"
        val toSave = if (project.id.isBlank()) project.copy(id = docId) else project

        _developmentProjects.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("development_projects").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save project failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save project")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteDevelopmentProject(
        projectId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _developmentProjects.update { list -> list.filterNot { it.id == projectId } }
        val db = firestore
        if (db != null) {
            db.collection("development_projects").document(projectId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete project failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete project")
                }
        } else {
            onSuccess()
        }
    }

    // --- Government Reports & Gazettes CRUD ---
    fun saveGovtReport(
        item: GovtReportItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "rep_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _govtReports.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().also { it[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("government_reports").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save report failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save report")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteGovtReport(
        reportId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _govtReports.update { list -> list.filterNot { it.id == reportId } }
        val db = firestore
        if (db != null) {
            db.collection("government_reports").document(reportId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete report failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete report")
                }
        } else {
            onSuccess()
        }
    }

    // --- Gallery Items CRUD ---
    fun saveGalleryItem(
        item: GalleryItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "G_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _galleryItems.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("gallery_items").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save gallery item failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save gallery item")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteGalleryItem(
        galleryId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _galleryItems.update { list -> list.filterNot { it.id == galleryId } }
        val db = firestore
        if (db != null) {
            db.collection("gallery_items").document(galleryId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete gallery item failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete gallery item")
                }
        } else {
            onSuccess()
        }
    }

    // --- Media Archive Items CRUD ---
    fun saveMediaArchiveItem(
        item: MediaArchiveItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "M_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _mediaArchiveItems.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("media_items").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save media archive item failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save media archive item")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteMediaArchiveItem(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _mediaArchiveItems.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("media_items").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete media archive item failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete media archive item")
                }
        } else {
            onSuccess()
        }
    }

    // --- Important External Links & Wikipedia CRUD ---
    fun saveImportantLink(
        item: ImportantExternalLink,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "LINK_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _importantLinks.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("important_links").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to save important link")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteImportantLink(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _importantLinks.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("important_links").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to delete important link")
                }
        } else {
            onSuccess()
        }
    }

    // --- Special Events & Days CRUD ---
    fun saveSpecialEvent(
        item: SpecialEventItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "SE_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _specialEvents.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("special_events").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to save special event")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteSpecialEvent(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _specialEvents.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("special_events").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onError(e.localizedMessage ?: "Failed to delete special event")
                }
        } else {
            onSuccess()
        }
    }

    private fun noticeToMap(notice: NoticeFeedItem): Map<String, Any> = mapOf(
        "id" to notice.id,
        "titleBn" to notice.titleBn,
        "titleEn" to notice.titleEn,
        "dateBn" to notice.dateBn,
        "dateEn" to notice.dateEn,
        "categoryBn" to notice.categoryBn,
        "categoryEn" to notice.categoryEn,
        "detailsBn" to notice.detailsBn,
        "detailsEn" to notice.detailsEn,
        "isUrgent" to notice.isUrgent,
        "isPublished" to notice.isPublished,
        "published" to notice.isPublished,
        "createdAt" to notice.createdAt
    )

    // --- Notices CRUD ---
    fun saveNotice(
        notice: NoticeFeedItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val docId = if (notice.id.isNotBlank()) notice.id else "N_${now}"
        val toSave = notice.copy(
            id = docId,
            createdAt = if (notice.createdAt > 0L) notice.createdAt else now
        )

        _notices.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("notices").document(docId)
                .set(noticeToMap(toSave))
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save notice failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save notice")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteNotice(
        noticeId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _notices.update { list -> list.filterNot { it.id == noticeId } }
        val db = firestore
        if (db != null) {
            db.collection("notices").document(noticeId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete notice failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete notice")
                }
        } else {
            onSuccess()
        }
    }

    fun addNotice(notice: NoticeFeedItem) {
        saveNotice(notice)
    }

    // --- Emergency Contacts CRUD ---
    fun saveEmergencyContact(
        contact: EmergencyContact,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (contact.id.isNotBlank()) contact.id else "E_${System.currentTimeMillis()}"
        val toSave = if (contact.id.isBlank()) contact.copy(id = docId) else contact

        _emergencyContacts.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                listOf(toSave) + list
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("emergency_contacts").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save contact failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save emergency contact")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteEmergencyContact(
        contactId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _emergencyContacts.update { list -> list.filterNot { it.id == contactId } }
        val db = firestore
        if (db != null) {
            db.collection("emergency_contacts").document(contactId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete contact failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete emergency contact")
                }
        } else {
            onSuccess()
        }
    }

    // --- Blood Donors CRUD ---
    fun saveBloodDonor(
        donor: BloodDonor,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid.isNullOrBlank() && !canEditCmsContent) {
            onError("Authentication required to register as blood donor")
            return
        }

        val docId = if (donor.id.isNotBlank()) donor.id else "B_${System.currentTimeMillis()}"
        val effectiveUserId = if (!canEditCmsContent && !uid.isNullOrBlank()) uid else donor.userId.ifBlank { uid ?: "" }
        val toSave = donor.copy(id = docId, userId = effectiveUserId)

        if (db != null) {
            db.collection("blood_donors").document(docId)
                .set(toSave)
                .addOnSuccessListener {
                    _bloodDonors.update { list ->
                        val index = list.indexOfFirst { it.id == toSave.id }
                        if (index >= 0) {
                            list.toMutableList().apply { this[index] = toSave }
                        } else {
                            listOf(toSave) + list
                        }
                    }
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Save donor failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save donor")
                }
        } else {
            _bloodDonors.update { list ->
                val index = list.indexOfFirst { it.id == toSave.id }
                if (index >= 0) {
                    list.toMutableList().apply { this[index] = toSave }
                } else {
                    listOf(toSave) + list
                }
            }
            onSuccess()
        }
    }

    fun deleteBloodDonor(
        donorId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _bloodDonors.update { list -> list.filterNot { it.id == donorId } }
        val db = firestore
        if (db != null) {
            db.collection("blood_donors").document(donorId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete donor failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete donor")
                }
        } else {
            onSuccess()
        }
    }

    fun addBloodDonor(donor: BloodDonor) {
        saveBloodDonor(donor)
    }

    // --- Profile Info CRUD ---
    fun updateProfileInfo(
        newProfile: ProfileInfo,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _profileInfo.value = newProfile
        val db = firestore
        if (db != null) {
            db.collection("profile_info").document("main")
                .set(newProfile, SetOptions.merge())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Profile info update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update profile")
                }
        } else {
            onSuccess()
        }
    }

    // --- App Config & Contact Info CRUD ---
    fun updateAppConfig(
        newConfig: AppConfig,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _appConfig.value = newConfig
        val db = firestore
        if (db != null) {
            db.collection("config").document("app")
                .set(newConfig, SetOptions.merge())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "App config update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update config")
                }
        } else {
            onSuccess()
        }
    }

    // --- Dynamic Watercraft Schedules CRUD ---
    fun saveWatercraftSchedule(
        schedule: DynamicWatercraftSchedule,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (schedule.id.isNotBlank()) schedule.id else "SCHED_${System.currentTimeMillis()}"
        val toSave = if (schedule.id.isBlank()) schedule.copy(id = docId) else schedule

        _dynamicWatercraftSchedules.update { list ->
            val existing = list.indexOfFirst { it.id == toSave.id }
            if (existing >= 0) {
                list.toMutableList().apply { this[existing] = toSave }
            } else {
                list + toSave
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("watercraft_schedules").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Watercraft schedule save failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to save watercraft schedule")
                }
        } else {
            onSuccess()
        }
    }

    fun deleteWatercraftSchedule(
        scheduleId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _dynamicWatercraftSchedules.update { list -> list.filterNot { it.id == scheduleId } }
        val db = firestore
        if (db != null) {
            db.collection("watercraft_schedules").document(scheduleId)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Delete schedule failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to delete schedule")
                }
        } else {
            onSuccess()
        }
    }

    fun resetWatercraftSchedulesToDefault(
        onSuccess: () -> Unit = {}
    ) {
        _dynamicWatercraftSchedules.value = defaultWatercraftSchedules
        val db = firestore
        if (db != null) {
            coroutineScope.launch {
                try {
                    val batch = db.batch()
                    defaultWatercraftSchedules.forEach { s ->
                        val doc = db.collection("watercraft_schedules").document(s.id)
                        batch.set(doc, s)
                    }
                    batch.commit().addOnSuccessListener { onSuccess() }
                } catch (e: Exception) {
                    onSuccess()
                }
            }
        } else {
            onSuccess()
        }
    }

    // --- Dynamic Tide Schedule CRUD ---
    fun updateTideSchedule(
        tide: DynamicTideSchedule,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _dynamicTideSchedule.value = tide
        _weatherData.update {
            it.copy(
                highTideBn = tide.highTide2Bn,
                highTideEn = tide.highTide2En,
                lowTideBn = tide.lowTide2Bn,
                lowTideEn = tide.lowTide2En,
                advisoryBn = tide.safetyMarqueeBn,
                advisoryEn = tide.safetyMarqueeEn
            )
        }
        val db = firestore
        if (db != null) {
            db.collection("tide_schedules").document("current")
                .set(tide)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Tide schedule update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update tide schedule")
                }
        } else {
            onSuccess()
        }
    }

    // --- Dynamic Media Assets CRUD ---
    fun updateMediaAssets(
        mediaAssets: DynamicMediaAssets,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _dynamicMediaAssets.value = mediaAssets
        _appConfig.update {
            it.copy(
                heroImageUrl = mediaAssets.bannerPosterSource,
                avatarImageUrl = mediaAssets.profilePictureSource
            )
        }
        _profileInfo.update {
            it.copy(
                photoUrl = mediaAssets.profilePictureSource,
                bannerUrl = mediaAssets.bannerPosterSource
            )
        }
        val db = firestore
        if (db != null) {
            db.collection("media_assets").document("current")
                .set(mediaAssets)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Media assets update failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to update media assets")
                }
        } else {
            onSuccess()
        }
    }

    // CRUD for Digital Services
    fun saveDigitalService(item: DigitalServiceItem, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val targetId = item.id.ifBlank { "DS_${System.currentTimeMillis()}" }
        val toSave = if (item.id.isBlank()) item.copy(id = targetId) else item
        firestore?.collection("digital_services")?.document(targetId)
            ?.set(toSave)?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { onError(it.message ?: "Unknown error") }
    }

    fun deleteDigitalService(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        firestore?.collection("digital_services")?.document(id)?.delete()?.addOnSuccessListener { onSuccess() }?.addOnFailureListener { onError(it.message ?: "Unknown error") }
    }

    // CRUD for Daily Schedule
    fun saveDailySchedule(item: DailyScheduleItem, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val targetId = item.id.ifEmpty { "sched_${System.currentTimeMillis()}" }
        val targetItem = item.copy(
            id = targetId,
            timestamp = if (item.timestamp != 0L) item.timestamp else System.currentTimeMillis()
        )
        _dailySchedules.update { list ->
            val index = list.indexOfFirst { it.id == targetId }
            if (index >= 0) {
                list.toMutableList().also { it[index] = targetItem }
            } else {
                list + targetItem
            }
        }
        val db = firestore
        if (db != null) {
            db.collection("daily_schedules").document(targetId)
                .set(targetItem)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { err -> onError(err.message ?: "Unknown error") }
        } else {
            onSuccess()
        }
    }

    fun deleteDailySchedule(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        _dailySchedules.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("daily_schedules").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { err -> onError(err.message ?: "Unknown error") }
        } else {
            onSuccess()
        }
    }

    // 16. Appointment Requests
    private val _appointmentRequests = MutableStateFlow<List<AppointmentRequest>>(emptyList())
    val appointmentRequests: StateFlow<List<AppointmentRequest>> = _appointmentRequests.asStateFlow()

    private fun setupAppointmentListener(db: FirebaseFirestore, isAppAdmin: Boolean, uid: String) {
        val query = if (isAppAdmin) {
            db.collection("applications")
        } else {
            db.collection("applications").whereEqualTo("userId", uid)
        }
        applicationsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("PortalRepository", "Applications listener error: ${error.message}")
                setupAppointmentsFallbackListener(db, isAppAdmin, uid)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    FirestoreParsers.parseAppointment(doc)
                }.sortedByDescending { it.timestamp }
                _appointmentRequests.value = list
            } else {
                setupAppointmentsFallbackListener(db, isAppAdmin, uid)
            }
        }
    }

    private fun setupAppointmentsFallbackListener(db: FirebaseFirestore, isAppAdmin: Boolean, uid: String) {
        appointmentsListener?.remove()
        val query = if (isAppAdmin) {
            db.collection("appointments")
        } else {
            db.collection("appointments").whereEqualTo("userId", uid)
        }
        appointmentsListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("PortalRepository", "Appointments fallback listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    FirestoreParsers.parseAppointment(doc)
                }.sortedByDescending { it.timestamp }
                _appointmentRequests.value = list
            }
        }
    }

    fun submitAppointmentRequest(
        request: AppointmentRequest,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid.isNullOrBlank()) {
            onError("Authentication required to submit an application")
            return
        }

        val charPool = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val randomCode = (1..6).map { charPool.random() }.joinToString("")
        val trackingNum = "APP-$randomCode"
        val currentTime = System.currentTimeMillis()
        val docId = request.id.ifBlank { "APP_${currentTime}_${(100..999).random()}" }

        val finalRequest = request.copy(
            id = docId,
            userId = uid ?: request.userId,
            trackingId = trackingNum,
            trackingNumber = trackingNum,
            name = request.name,
            phone = request.phone.ifBlank { request.mobile },
            mobile = request.mobile.ifBlank { request.phone },
            upazila = request.upazila,
            union = request.union,
            category = request.category.ifBlank { request.type },
            type = request.type.ifBlank { request.category },
            message = request.message,
            attachmentUrl = request.attachmentUrl,
            status = "Submitted",
            adminNote = "",
            scheduledDate = "",
            createdAt = if (request.createdAt != 0L) request.createdAt else currentTime,
            timestamp = if (request.timestamp != 0L) request.timestamp else currentTime
        )

        if (db != null) {
            val appDocRef = db.collection("applications").document(docId)
            val apptDocRef = db.collection("appointments").document(docId)

            val docMap = mapOf(
                "id" to docId,
                "userId" to finalRequest.userId,
                "trackingNumber" to trackingNum,
                "trackingId" to trackingNum,
                "name" to finalRequest.name,
                "mobile" to finalRequest.mobile,
                "phone" to finalRequest.phone,
                "upazila" to finalRequest.upazila,
                "union" to finalRequest.union,
                "type" to finalRequest.type,
                "category" to finalRequest.category,
                "message" to finalRequest.message,
                "attachmentUrl" to finalRequest.attachmentUrl,
                "status" to finalRequest.status,
                "adminNote" to "",
                "scheduledDate" to "",
                "timestamp" to finalRequest.timestamp,
                "createdAt" to finalRequest.createdAt
            )

            appDocRef.set(docMap)
                .addOnSuccessListener {
                    _appointmentRequests.update { listOf(finalRequest) + it.filterNot { item -> item.id == docId } }
                    apptDocRef.set(docMap)
                    onSuccess(trackingNum)
                }
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Applications submit failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to submit application")
                }
        } else {
            _appointmentRequests.update { listOf(finalRequest) + it.filterNot { item -> item.id == docId } }
            onSuccess(trackingNum)
        }
    }

    fun getAppointmentByTrackingId(
        trackingId: String,
        onResult: (AppointmentRequest?) -> Unit
    ) {
        val queryStr = trackingId.trim()
        if (queryStr.isBlank()) {
            onResult(null)
            return
        }

        val uid = auth?.currentUser?.uid
        val localMatch = _appointmentRequests.value.firstOrNull { req ->
            (req.trackingNumber.equals(queryStr, ignoreCase = true) ||
                req.trackingId.equals(queryStr, ignoreCase = true)) &&
                (canManageApplications || (!uid.isNullOrBlank() && req.userId == uid))
        }

        val db = firestore
        if (db == null) {
            onResult(localMatch)
            return
        }
        if (uid.isNullOrBlank()) {
            onResult(null)
            return
        }

        val appCol = db.collection("applications")
        val apptCol = db.collection("appointments")
        val trackingQuery1 = if (canManageApplications) {
            appCol.whereEqualTo("trackingNumber", queryStr)
        } else {
            appCol.whereEqualTo("userId", uid).whereEqualTo("trackingNumber", queryStr)
        }
        val trackingQuery2 = if (canManageApplications) {
            appCol.whereEqualTo("trackingId", queryStr)
        } else {
            appCol.whereEqualTo("userId", uid).whereEqualTo("trackingId", queryStr)
        }
        val apptFallbackQuery = if (canManageApplications) {
            apptCol.whereEqualTo("trackingId", queryStr)
        } else {
            apptCol.whereEqualTo("userId", uid).whereEqualTo("trackingId", queryStr)
        }

        trackingQuery1.get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()?.let { FirestoreParsers.parseAppointment(it) }
                if (doc != null) {
                    onResult(doc)
                } else {
                    trackingQuery2.get()
                        .addOnSuccessListener { snap2 ->
                            val doc2 = snap2.documents.firstOrNull()?.let { FirestoreParsers.parseAppointment(it) }
                            if (doc2 != null) {
                                onResult(doc2)
                            } else {
                                apptFallbackQuery.get()
                                    .addOnSuccessListener { snap3 ->
                                        val doc3 = snap3.documents.firstOrNull()?.let { FirestoreParsers.parseAppointment(it) }
                                        onResult(doc3 ?: localMatch)
                                    }
                                    .addOnFailureListener { onResult(localMatch) }
                            }
                        }
                        .addOnFailureListener { onResult(localMatch) }
                }
            }
            .addOnFailureListener {
                apptFallbackQuery.get()
                    .addOnSuccessListener { snap ->
                        val doc = snap.documents.firstOrNull()?.let { FirestoreParsers.parseAppointment(it) }
                        onResult(doc ?: localMatch)
                    }
                    .addOnFailureListener { onResult(localMatch) }
            }
    }

    fun updateAppointmentStatus(
        id: String,
        status: String,
        adminNote: String = "",
        scheduledDate: String = "",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val db = firestore ?: return
        val updates = mapOf(
            "status" to status,
            "adminNote" to adminNote,
            "scheduledDate" to scheduledDate
        )
        db.collection("applications").document(id).update(updates)
            .addOnSuccessListener {
                db.collection("appointments").document(id).update(updates)
                onSuccess()
            }
            .addOnFailureListener { err ->
                db.collection("appointments").document(id).update(updates)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e2 ->
                        onError(e2.localizedMessage ?: err.localizedMessage ?: "Failed to update status")
                    }
            }
    }

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
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db == null) {
            onSuccess()
            return
        }
        if (uid.isNullOrBlank()) {
            onError("Authentication required to submit feedback")
            return
        }
        val feedback = mapOf(
            "userId" to uid,
            "name" to name,
            "phone" to phone,
            "union" to union,
            "category" to category,
            "message" to message,
            "attachmentUrl" to attachmentUrl,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("general_feedback").add(feedback)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to submit feedback") }
    }

    private fun setupFaqListener(db: FirebaseFirestore) {
        db.collection("faqs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultFaqs.forEach { item ->
                                        val doc = db.collection("faqs").document(item.id)
                                        batch.set(doc, item)
                                    }
                                    batch.commit()
                                } catch (_: Exception) {}
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseFaqItem(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _faqs.value = list
                    }
                }
            }
    }

    fun saveFaq(
        item: FaqItem,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "FAQ_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _faqs.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                list + toSave
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("faqs").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to save FAQ") }
        } else {
            onSuccess()
        }
    }

    fun deleteFaq(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _faqs.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("faqs").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to delete FAQ") }
        } else {
            onSuccess()
        }
    }

    fun deleteAppointmentRequest(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _appointmentRequests.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("applications").document(id)
                .delete()
                .addOnSuccessListener {
                    db.collection("appointments").document(id).delete()
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    db.collection("appointments").document(id)
                        .delete()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e2 ->
                            onError(e2.localizedMessage ?: e.localizedMessage ?: "Failed to delete appointment")
                        }
                }
        } else {
            onSuccess()
        }
    }

    private fun setupUnionRepresentativeListener(db: FirebaseFirestore) {
        db.collection("union_representatives")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultUnionRepresentatives.forEach { item ->
                                        val doc = db.collection("union_representatives").document(item.id)
                                        batch.set(doc, item)
                                    }
                                    batch.commit()
                                } catch (_: Exception) {}
                            }
                        }
                    } else {
                        val list = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseUnionRepresentative(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                        _unionRepresentatives.value = list
                    }
                }
            }
    }

    fun saveUnionRepresentative(
        item: UnionRepresentative,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val docId = if (item.id.isNotBlank()) item.id else "UR_${System.currentTimeMillis()}"
        val toSave = if (item.id.isBlank()) item.copy(id = docId) else item

        _unionRepresentatives.update { list ->
            val index = list.indexOfFirst { it.id == toSave.id }
            if (index >= 0) {
                list.toMutableList().apply { this[index] = toSave }
            } else {
                list + toSave
            }
        }

        val db = firestore
        if (db != null) {
            db.collection("union_representatives").document(docId)
                .set(toSave)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to save") }
        } else {
            onSuccess()
        }
    }

    fun deleteUnionRepresentative(
        id: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        _unionRepresentatives.update { list -> list.filterNot { it.id == id } }
        val db = firestore
        if (db != null) {
            db.collection("union_representatives").document(id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to delete") }
        } else {
            onSuccess()
        }
    }

    // --- Citizen Poll Vote Sync ---
    private fun setupPollVoteListener(db: FirebaseFirestore, uid: String) {
        val pollId = _citizenPoll.value.id.ifBlank { "default" }
        pollVoteListener = db.collection("poll_votes").document("${pollId}_${uid}")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Poll vote listener error: ${error.message}")
                    return@addSnapshotListener
                }
                val optionIdx = if (snapshot != null && snapshot.exists()) {
                    (snapshot.get("optionIndex") as? Number)?.toInt()
                } else {
                    null
                }
                _userVotedOption.value = optionIdx
                _pollVotes.value = rebuildPollVotes(_citizenPoll.value, optionIdx)
            }
    }

    fun submitPollVote(optionIndex: Int, onError: (String) -> Unit = {}) {
        val poll = _citizenPoll.value
        if (!poll.isPublished || optionIndex !in poll.options.indices || _userVotedOption.value != null) {
            return
        }
        val db = firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid.isNullOrBlank()) {
            onError("Authentication required to vote")
            return
        }
        _userVotedOption.value = optionIndex
        _pollVotes.value = rebuildPollVotes(poll, optionIndex)
        if (db != null && !uid.isNullOrBlank()) {
            val pollId = poll.id.ifBlank { "default" }
            val payload = mapOf(
                "pollId" to pollId,
                "userId" to uid,
                "optionIndex" to optionIndex,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.collection("poll_votes").document("${pollId}_${uid}")
                .set(payload)
                .addOnFailureListener { e ->
                    Log.w("PortalRepository", "Poll vote write failed: ${e.message}")
                    onError(e.localizedMessage ?: "Failed to record vote")
                }
        }
    }

    // --- Tourism & Dynamic CMS Collections ---
    private fun setupTourismSpotsListener(db: FirebaseFirestore) {
        db.collection("tourism_spots")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("PortalRepository", "Tourism spots listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.isEmpty) {
                        if (canEditCmsContent) {
                            coroutineScope.launch {
                                try {
                                    val batch = db.batch()
                                    defaultTourismSpots.forEach { spot ->
                                        batch.set(db.collection("tourism_spots").document(spot.spotId), spot)
                                    }
                                    batch.commit()
                                } catch (_: Exception) {}
                            }
                        }
                    } else {
                        _tourismSpots.value = snapshot.documents.mapNotNull { doc ->
                            FirestoreParsers.parseTourismSpot(doc)
                        }.filter { it.isPublished }.sortedBy { it.order }
                    }
                }
            }
    }

    private fun setupCmsModuleListeners(db: FirebaseFirestore) {
        db.collection("education_resources").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultEducationResources.forEach {
                                    batch.set(db.collection("education_resources").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _educationResources.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseEducationResource(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("economy_opportunities").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultEconomyOpportunities.forEach {
                                    batch.set(db.collection("economy_opportunities").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _economyOpportunities.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseEconomyOpportunity(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("welfare_programs").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultWelfarePrograms.forEach {
                                    batch.set(db.collection("welfare_programs").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _welfarePrograms.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseWelfareProgram(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("union_services").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultUnionServices.forEach {
                                    batch.set(db.collection("union_services").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _unionServices.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseUnionServiceHub(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("development_categories").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultDevelopmentCategories.forEach {
                                    batch.set(db.collection("development_categories").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _developmentCategories.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseDevelopmentCategory(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("homepage_categories").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        coroutineScope.launch {
                            try {
                                val batch = db.batch()
                                DefaultPortalContent.defaultHomepageCategories.forEach {
                                    batch.set(db.collection("homepage_categories").document(it.id), it)
                                }
                                batch.commit()
                            } catch (_: Exception) {}
                        }
                    }
                } else {
                    _homepageCategories.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseHomepageCategory(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }

        db.collection("citizen_polls").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        val defaultPoll = DefaultPortalContent.defaultCitizenPoll
                        db.collection("citizen_polls").document(defaultPoll.id).set(defaultPoll)
                    }
                } else {
                    val allPolls = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseCitizenPoll(it)
                    }.sortedBy { it.order }
                    val active = allPolls.firstOrNull { it.id == "default" } ?: allPolls.firstOrNull()
                    if (active != null) {
                        val prevId = _citizenPoll.value.id
                        _citizenPoll.value = active
                        _pollVotes.value = rebuildPollVotes(active, _userVotedOption.value)
                        val uid = currentAuthUid
                        if (prevId != active.id && !uid.isNullOrBlank()) {
                            pollVoteListener?.remove()
                            setupPollVoteListener(db, uid)
                        }
                    }
                }
            }
        }
    }

    // --- Social Media CRUD ---
    private val _socialMediaLinks = MutableStateFlow<List<SocialMediaLink>>(emptyList())
    val socialMediaLinks: StateFlow<List<SocialMediaLink>> = _socialMediaLinks.asStateFlow()

    private fun setupSocialMediaListener(db: FirebaseFirestore) {
        db.collection("social_media_links").addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
                val defaults = listOf(
                    SocialMediaLink(
                        id = "SM_FB",
                        platformNameBn = "ফেসবুক পেজ",
                        platformNameEn = "Facebook Page",
                        url = "https://facebook.com/asheq.ullah.rafiq.mp",
                        iconType = "FACEBOOK",
                        order = 1
                    ),
                    SocialMediaLink(
                        id = "SM_YT",
                        platformNameBn = "ইউটিউব চ্যানেল",
                        platformNameEn = "YouTube Channel",
                        url = "https://youtube.com/@ashequllahrafiq",
                        iconType = "YOUTUBE",
                        order = 2
                    ),
                    SocialMediaLink(
                        id = "SM_WA",
                        platformNameBn = "হোয়াটসঅ্যাপ হেল্পলাইন",
                        platformNameEn = "WhatsApp Helpline",
                        url = "https://wa.me/8801819000000",
                        iconType = "WHATSAPP",
                        order = 3
                    ),
                    SocialMediaLink(
                        id = "SM_TW",
                        platformNameBn = "এক্স (টুইটার)",
                        platformNameEn = "X (Twitter)",
                        url = "https://twitter.com/AsheqRafiqMP",
                        iconType = "TWITTER",
                        order = 4
                    )
                )
                if (snapshot.isEmpty) {
                    if (canEditCmsContent) {
                        defaults.forEach { link ->
                            db.collection("social_media_links").document(link.id).set(link)
                        }
                    }
                    _socialMediaLinks.value = defaults
                } else {
                    _socialMediaLinks.value = snapshot.documents.mapNotNull {
                        FirestoreParsers.parseSocialMediaLink(it)
                    }.filter { it.isPublished }.sortedBy { it.order }
                }
            }
        }
    }

    fun saveSocialMediaLink(item: SocialMediaLink, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val targetId = item.id.ifBlank { "SM_${System.currentTimeMillis()}" }
        val toSave = if (item.id.isBlank()) item.copy(id = targetId) else item
        firestore?.collection("social_media_links")?.document(targetId)?.set(toSave)
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { e -> onError(e.localizedMessage ?: "Save failed") }
    }

    fun deleteSocialMediaLink(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        firestore?.collection("social_media_links")?.document(id)?.delete()
            ?.addOnSuccessListener { onSuccess() }
            ?.addOnFailureListener { e -> onError(e.localizedMessage ?: "Delete failed") }
    }

    // --- Tide Refresh ---
    fun refreshTideSchedule(onComplete: () -> Unit = {}) {
        firestore?.collection("tide_schedules")?.document("current")?.get()?.addOnSuccessListener { snapshot ->
            FirestoreParsers.safeToObject(snapshot, DynamicTideSchedule::class.java)?.let {
                _dynamicTideSchedule.value = it
            }
            onComplete()
        }?.addOnFailureListener { onComplete() }
    }
}

