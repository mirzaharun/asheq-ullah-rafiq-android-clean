package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

enum class AppLanguage {
    BN, EN;

    val toggleLabel: String
        get() = if (this == BN) "English (A)" else "বাংলা (অ)"

    val currentSymbol: String
        get() = if (this == BN) "অ" else "A"
}

@IgnoreExtraProperties
data class AppConfig(
    val appNameBn: String = "আশেক উল্লাহ রফিক",
    val appNameEn: String = "Asheq Ullah Rafiq",
    val subTitleBn: String = "উন্নয়ন ও জনসংযোগ পোর্টাল",
    val subTitleEn: String = "Development & Public Relations",
    val constituencyBn: String = "কক্সবাজার-২ (মহেশখালী–কুতুবদিয়া)",
    val constituencyEn: String = "Cox's Bazar-2 (Moheshkhali–Kutubdia)",
    val profileNameBn: String = "আশেক উল্লাহ রফিক",
    val profileNameEn: String = "Asheq Ullah Rafiq",
    val profileTitleBn: String = "সাবেক জাতীয় সংসদ সদস্য",
    val profileTitleEn: String = "Former Member of Parliament",
    val heroTitleBn: String = "মহেশখালী-কুতুবদিয়ার উন্নয়ন ও অগ্রযাত্রা",
    val heroTitleEn: String = "Moheshkhali-Kutubdia Development & Progress",
    val heroSubtitleBn: String = "সামাজিক, অর্থনৈতিক ও অবকাঠামোগত উন্নয়নের এক দশক",
    val heroSubtitleEn: String = "A decade of social, economic & infrastructure progress",
    val decadeBadgeBn: String = "২০১৪ – ২০২৪",
    val decadeBadgeEn: String = "2014 – 2024",
    val quoteTextBn: String = "উন্নয়ন মানে শুধু অবকাঠামো নয়, উন্নয়ন মানে মানুষের জীবনমানের পরিবর্তন।",
    val quoteTextEn: String = "Development is not just infrastructure; development is the transformation of human lives.",
    val quoteAuthorBn: String = "— আশেক উল্লাহ রফিক",
    val quoteAuthorEn: String = "— Asheq Ullah Rafiq",
    val heroImageUrl: String = "drawable:banner_video_poster",
    val avatarImageUrl: String = "drawable:asheq_profile_custom",
    val copyrightBn: String = "© আশেক উল্লাহ রফিক • সর্বস্বত্ব সংরক্ষিত",
    val copyrightEn: String = "© Asheq Ullah Rafiq • All Rights Reserved",
    val policyTextBn: String = "গোপনীয়তা নীতি • ব্যবহারের শর্তাবলী",
    val policyTextEn: String = "Privacy Policy • Terms of Service",
    val officeAddressCoxsBazarBn: String = "কক্সবাজার অফিস- হোটেল সৈকত নিচ তলা, মেইন রোড়, কক্সবাজার।",
    val officeAddressCoxsBazarEn: String = "Cox's Bazar Office: Hotel Saikat Ground Floor, Main Road, Cox's Bazar.",
    val officeAddressMoheshkhaliBn: String = "মহেশখালী কার্যালয়: নতুন বাজার রোড, মহেশখালী পৌরসভা, কক্সবাজার",
    val officeAddressMoheshkhaliEn: String = "Moheshkhali Office: Notun Bazar Road, Moheshkhali Municipality, Cox's Bazar",
    val officeAddressKutubdiaBn: String = "কুতুবদিয়া কার্যালয়: বড়ঘোপ ইউনিয়ন পরিষদ রোড, কুতুবদিয়া, কক্সবাজার",
    val officeAddressKutubdiaEn: String = "Kutubdia Office: Boroghop Union Parishad Road, Kutubdia, Cox's Bazar",
    val hotlinePhone: String = "01800000000",
    val contactEmail: String = "Asheqrafiq@gmail.com",
    val facebookUrl: String = "https://facebook.com/asheqmp",
    val instagramUrl: String = "https://instagram.com/asheqmp",
    val twitterUrl: String = "https://x.com/asheqmp",
    val youtubeUrl: String = "https://youtube.com/asheqmp",
    val availabilityType: String = "OFFICE_PRESENT", // "OFFICE_PRESENT", "OFFICIAL_PROGRAM", "PARLIAMENT_SESSION"
    val meetingScheduleStatusBn: String = "আজ কক্সবাজার কার্যালয়ে উপস্থিত আছেন—সরাসরি সাক্ষাত ও পরামর্শের সুযোগ রয়েছে (বিকাল ৪:০০ - সন্ধ্যা ৭:০০)।",
    val meetingScheduleStatusEn: String = "Present today at Cox's Bazar office—available for meeting & consultation (4:00 PM - 7:00 PM).",
    val noticeMarqueeBn: String = "⚠️ বিশেষ ঘোষণা: মহেশখালী-কুতুবদিয়ার সকল উন্নয়ন প্রকল্পে স্বচ্ছতা ও জনসেবা নিশ্চিত করতে এমপি মহোদয়ের তদারকি অব্যাহত রয়েছে।",
    val noticeMarqueeEn: String = "⚠️ Special Announcement: Continuous monitoring by the MP is ongoing to ensure transparency and public service across all development projects in Moheshkhali-Kutubdia.",
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val forceUpdate: Boolean = false,
    val updateMessageBn: String = "",
    val updateMessageEn: String = ""
)

@IgnoreExtraProperties
data class AppRelease(
    val versionCode: Int = 0,
    val versionName: String = "",
    val releaseNotesBn: String = "",
    val releaseNotesEn: String = "",
    val downloadUrl: String = "",
    val status: String = "draft"
)

@IgnoreExtraProperties
data class DynamicWatercraftSchedule(
    val id: String = "",
    val routeGroup: String = "KUTUBDIA_MOGNAMA", // "KUTUBDIA_MOGNAMA" or "MAHESHKHALI_COXSBAZAR"
    val routeTitleBn: String = "",
    val routeTitleEn: String = "",
    val isOriginDeparture: Boolean = true,
    val officialNoticeTextBn: String = "",
    val officialNoticeTextEn: String = "",
    val timeSlotsBn: List<String> = emptyList(),
    val timeSlotsEn: List<String> = emptyList(),
    val vesselTypeBn: String = "সি-ট্রাক",
    val vesselTypeEn: String = "Sea-Truck",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class DynamicTideSchedule(
    val highTide1Bn: String = "ভোর ০৫:২০ মি.",
    val highTide1En: String = "05:20 AM",
    val highTide2Bn: String = "বিকাল ০৫:৪৫ মি.",
    val highTide2En: String = "05:45 PM",
    val lowTide1Bn: String = "সকাল ১১:৩০ মি.",
    val lowTide1En: String = "11:30 AM",
    val lowTide2Bn: String = "রাত ১১:৫৫ মি.",
    val lowTide2En: String = "11:55 PM",
    val safetyMarqueeBn: String = "⚠️ বিশেষ সতর্কতা: ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা অবশ্যই মেনে চলুন।   •   🌊 ভাটার সময় জেটিতে বাড়তি সতর্কতা অবলম্বন করুন।",
    val safetyMarqueeEn: String = "⚠️ Safety Alert: Follow security guidelines strictly when using jetties during low tide.   •   🌊 Exercise caution at landing ghats during ebb tide.",
    val statusBn: String = "সাগর শান্ত",
    val statusEn: String = "Calm Tide",
    val lastUpdated: String = "আজ"
)

@IgnoreExtraProperties
data class DynamicMediaAssets(
    val appIconName: String = "1176.jpg",
    val appIconDrawable: String = "drawable:ic_app_icon_art",
    val bannerVideoSource: String = "raw:banner_video",
    val bannerPosterSource: String = "drawable:banner_video_poster",
    val profilePictureSource: String = "drawable:asheq_profile_custom",
    val lastUpdated: String = "আজ"
)

@IgnoreExtraProperties
data class FilterChipItem(
    val id: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val categoryKey: String = "",
    val order: Int = 0,
    val isActive: Boolean = true,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class WeatherData(
    val temperatureC: Double = 28.5,
    val conditionBn: String = "আংশিক মেঘলা ও শান্ত সাগর",
    val conditionEn: String = "Partly Cloudy & Calm Sea",
    val windSpeedKmH: Int = 14,
    val seaStateBn: String = "শান্ত (চলাচলের অনুকূল)",
    val seaStateEn: String = "Calm (Favorable for Voyage)",
    val maritimeSignal: Int = 1,
    val signalDescBn: String = "১ নম্বর দূরবর্তী সতর্ক সংকেত",
    val signalDescEn: String = "Distant Cautionary Signal No. 1",
    val humidity: Int = 78,
    val highTideBn: String = "দুপুর ০১:২০ মি.",
    val highTideEn: String = "01:20 PM",
    val lowTideBn: String = "সন্ধ্যা ০৭:৪৫ মি.",
    val lowTideEn: String = "07:45 PM",
    val advisoryBn: String = "বঙ্গোপসাগরে ট্রলার ও সী-ট্রাক চলাচল স্বাভাবিক রয়েছে। জেলেরা উপকূলে সতর্ক থাকুন।",
    val advisoryEn: String = "Sea-trucks and fishing trawlers operating normally in the Bay of Bengal. Stay cautious near deep water.",
    val lastUpdatedBn: String = "আজ সকাল ০৮:০০ মি.",
    val lastUpdatedEn: String = "Today, 08:00 AM"
)

@IgnoreExtraProperties
data class SeaTruckRoute(
    val id: String = "",
    val fromBn: String = "",
    val fromEn: String = "",
    val toBn: String = "",
    val toEn: String = "",
    val vesselNameBn: String = "",
    val vesselNameEn: String = "",
    val departureTimeBn: String = "",
    val departureTimeEn: String = "",
    val fareBdt: Int = 0,
    val statusBn: String = "",
    val statusEn: String = "",
    val isDelayed: Boolean = false
)

@IgnoreExtraProperties
data class SeaTruckUpdate(
    val generalStatusBn: String = "সকল রুটে সী-ট্রাক ও স্পিডবোট চলাচল স্বাভাবিক",
    val generalStatusEn: String = "Sea-truck & speedboat services operational on all routes",
    val noticeBn: String = "ভাটার সময় জেটি ব্যবহারের ক্ষেত্রে নিরাপত্তা কর্মীদের নির্দেশনা মেনে চলুন।",
    val noticeEn: String = "Please adhere to safety staff instructions when boarding during low tide.",
    val routes: List<SeaTruckRoute> = emptyList()
)

@IgnoreExtraProperties
data class PublicServiceItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val iconKey: String = "",
    val subTextBn: String = "",
    val subTextEn: String = "",
    val highlightColorHex: Long = 0xFF0D47A1
)

@IgnoreExtraProperties
data class EmergencyContact(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val category: String = "",
    val phone: String = "",
    val secondaryPhone: String? = null,
    val addressBn: String = "",
    val addressEn: String = "",
    val is24Hours: Boolean = true,
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class BloodDonor(
    val id: String = "",
    val userId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val bloodGroup: String = "",
    val upazila: String = "",
    val areaBn: String = "",
    val areaEn: String = "",
    val phone: String = "",
    val lastDonatedBn: String = "",
    val lastDonatedEn: String = "",
    val isAvailable: Boolean = true,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class TourismSpot(
    val spotId: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val category: String = "",
    val coverImageUrl: String = "",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val travelRouteDetailsBn: String = "",
    val travelRouteDetailsEn: String = "",
    val transportTypes: List<String> = emptyList(),
    val approxFareBdt: String = "",
    val recommendedHotelsBn: List<String> = emptyList(),
    val recommendedHotelsEn: List<String> = emptyList(),
    val seafoodRestaurantsBn: List<String> = emptyList(),
    val seafoodRestaurantsEn: List<String> = emptyList(),
    val googleMapsUrl: String = "",
    val navigationStepsBn: List<String> = emptyList(),
    val navigationStepsEn: List<String> = emptyList(),
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class DevelopmentProject(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val categoryBn: String = "",
    val categoryEn: String = "",
    val budgetBn: String = "",
    val budgetEn: String = "",
    val completionPercentage: Int = 0,
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val impactBn: String = "",
    val impactEn: String = "",
    val timelineBn: String = "২০১৪ – ২০২৪",
    val timelineEn: String = "2014 – 2024",
    val imageUrl: String = "drawable:moheshkhali_banner",
    val status: String = "Ongoing",
    val upazila: String = "Moheshkhali", // "Moheshkhali", "Kutubdia", or "All"
    val union: String = "All",
    val year: Int = 2024,
    val beforeImageUrl: String = "",
    val afterImageUrl: String = "",
    val pdfUrl: String = "",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class NoticeFeedItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val dateBn: String = "",
    val dateEn: String = "",
    val categoryBn: String = "",
    val categoryEn: String = "",
    val detailsBn: String = "",
    val detailsEn: String = "",
    @get:PropertyName("isUrgent")
    @PropertyName("isUrgent")
    val isUrgent: Boolean = false,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true,
    val createdAt: Long = 0L
)

enum class InAppNotificationType {
    NOTICE,
    APPLICATION_STATUS,
    GRIEVANCE_STATUS
}

data class InAppNotification(
    val id: String,
    val sourceId: String,
    val type: InAppNotificationType,
    val titleBn: String,
    val titleEn: String,
    val bodyBn: String,
    val bodyEn: String,
    val badgeBn: String,
    val badgeEn: String,
    val dateBn: String,
    val dateEn: String,
    val timestamp: Long,
    val isUrgent: Boolean = false,
    val trackingId: String = "",
    val statusNote: String = "",
    val scheduledDate: String = ""
)

@IgnoreExtraProperties
data class GalleryItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val categoryBn: String = "উন্নয়নচিত্র",
    val categoryEn: String = "Development Pictures",
    val imageUrl: String = "",
    val mediaType: String = "PHOTO", // "PHOTO" or "VIDEO"
    val videoEmbedUrl: String = "",
    val dateBn: String = "",
    val dateEn: String = "",
    val descriptionBn: String = "",
    val captionBn: String = "",
    val locationBn: String = "মহেশখালী-কুতুবদিয়া",
    val unionBn: String = "সকল এলাকা",
    val upazila: String = "মহেশখালী",
    val year: String = "২০২৪",
    val verifiedSource: String = "সরকারি তথ্যভাণ্ডার ও প্রেস উইং",
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class MediaArchiveItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val categoryBn: String = "সংবাদ প্রতিবেদন", // সংবাদ প্রতিবেদন, ভিডিও প্রতিবেদন, সাক্ষাৎকার, বক্তব্য ও বিবৃতি, সংবাদ সম্মেলন, পত্রিকার কাটিং, টকশো ও আলোচনা, প্রেস রিলিজ
    val mediaOutletBn: String = "",
    val publicationDateBn: String = "",
    val summaryBn: String = "",
    val externalUrl: String = "",
    val videoOrFileUrl: String = "",
    val subjectCategory: String = "",
    val upazila: String = "মহেশখালী",
    val unionBn: String = "সকল এলাকা",
    val verifiedSource: String = "গণমাধ্যম আর্কাইভ",
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class NewspaperClipping(
    val id: String = "",
    val headlineBn: String = "",
    val headlineEn: String = "",
    val newspaperNameBn: String = "",
    val newspaperNameEn: String = "",
    val publicationDateBn: String = "",
    val publicationDateEn: String = "",
    val imageUrl: String = "",
    val summaryBn: String = "",
    val summaryEn: String = "",
    val verifiedSource: String = "পত্রিকা আর্কাইভ"
)

@IgnoreExtraProperties
data class VideoNewsReport(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val tvChannelNameBn: String = "",
    val tvChannelNameEn: String = "",
    val broadcastDateBn: String = "",
    val broadcastDateEn: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val descriptionBn: String = ""
)

@IgnoreExtraProperties
data class ImportantExternalLink(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val url: String = "",
    val categoryBn: String = "উইকিপিডিয়া", // "উইকিপিডিয়া", "জাতীয় সংসদ", "মেগা প্রকল্প", "অফিসিয়াল পোর্টাল"
    val categoryEn: String = "Wikipedia",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val sourceName: String = "Wikipedia",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class SpecialEventItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val eventDateIso: String = "", // e.g. "2026-12-16"
    val dateDisplayBn: String = "",
    val dateDisplayEn: String = "",
    val categoryBn: String = "জাতীয় দিবস", // "জাতীয় দিবস" or "স্থানীয় উন্নয়ন দিবস"
    val categoryEn: String = "National Day",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val locationOrTagBn: String = "জাতীয়", // "জাতীয়", "মহেশখালী", "কুতুবদিয়া"
    val themeType: String = "PATRIOTIC", // "PATRIOTIC", "MOURNING", "FESTIVE", "WORKERS", "LOCAL_DEV"
    val isFeaturedCountdown: Boolean = false,
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class CitizenGrievance(
    val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val upazila: String = "",
    val subject: String = "",
    val details: String = "",
    val status: String = "Pending",
    val createdAtFormatted: String = ""
)

@IgnoreExtraProperties
data class ProfileInfo(
    val id: String = "main",
    val nameBn: String = "আশেক উল্লাহ রফিক",
    val nameEn: String = "Asheq Ullah Rafiq",
    val titleBn: String = "সাবেক সংসদ সদস্য, কক্সবাজার-২ (মহেশখালী-কুতুবদিয়া)",
    val titleEn: String = "Former Member of Parliament, Cox's Bazar-2 (Moheshkhali-Kutubdia)",
    val birthDateBn: String = "জন্ম: ২৪ জুলাই ১৯৭১",
    val birthDateEn: String = "Born: July 24, 1971",
    val birthPlaceBn: String = "জন্মস্থান: কক্সবাজার",
    val birthPlaceEn: String = "Birthplace: Cox's Bazar",
    val photoUrl: String = "drawable:asheq_profile_custom",
    val bannerUrl: String = "drawable:banner_video_poster",
    
    // Brief Intro Section (সংক্ষিপ্ত পরিচিতি)
    val briefIntroBn: String = "আশেক উল্লাহ রফিক একজন বাংলাদেশি রাজনীতিবিদ, ব্যবসায়ী এবং কক্সবাজার-২ (মহেশখালী-কুতুবদিয়া) আসনের সাবেক সংসদ সদস্য। তিনি বাংলাদেশ আওয়ামী লীগের প্রার্থী হিসেবে দশম, একাদশ ও দ্বাদশ জাতীয় সংসদ নির্বাচনে টানা তিনবার বিপুল ভোটে সংসদ সদস্য নির্বাচিত হন। দ্বীপ উপজেলা মহেশখালী ও কুতুবদিয়াকে জাতীয় অর্থনীতির মূল চালিকাশক্তিতে রূপান্তর, গভীর সমুদ্রবন্দর স্থাপন ও উপকূলীয় অবকাঠামোর মেগা উন্নয়নে তাঁর অবদান অবিস্মরণীয়।",
    val briefIntroEn: String = "Asheq Ullah Rafiq is a prominent Bangladeshi political leader, entrepreneur, and former Member of Parliament representing the Cox's Bazar-2 (Moheshkhali-Kutubdia) constituency. Elected across the 10th, 11th, and 12th National Parliaments, he has spearheaded monumental socio-economic advancements, deep sea port logistics, and 100% island electrification.",
    
    // 4 Milestones / Interactive Grid Cards
    val earlyLifeTitleBn: String = "প্রারম্ভিক জীবন ও শিক্ষা",
    val earlyLifeTitleEn: String = "Early Life & Education",
    val earlyLifeContentBn: String = "২৪ জুলাই ১৯৭১ সালে কক্সবাজারের ঐতিহ্যবাহী এক সম্ভ্রান্ত পরিবারে জন্মগ্রহণ করেন। তিনি চট্টগ্রাম সরকারি কমার্স কলেজ থেকে হিসাববিজ্ঞানে বিকম (অনার্স) ও সফলতার সাথে এমকম ডিগ্রি লাভ করেন। ছাত্রজীবন থেকেই তিনি উচ্চ নৈতিক মূল্যবোধ ও জনকল্যাণমুখী কর্মকাণ্ডে যুক্ত হন।",
    val earlyLifeContentEn: String = "Born on July 24, 1971, into a venerable family in Cox's Bazar. He completed his B.Com (Honours) and M.Com in Accounting from the prestigious Chittagong Government Commerce College, exhibiting distinguished academic and leadership qualities.",
    
    val studentPoliticsTitleBn: String = "ছাত্ররাজনীতি ও সাংগঠনিক দায়িত্ব",
    val studentPoliticsTitleEn: String = "Student Politics & Organizational Roles",
    val studentPoliticsContentBn: String = "ছাত্রজীবনে তিনি চট্টগ্রাম সরকারি কমার্স কলেজ ছাত্র সংসদের ভিপি (সহ-সভাপতি) এবং জিএস (সাধারণ সম্পাদক) নির্বাচিত হন। পরবর্তীতে চট্টগ্রাম বিশ্ববিদ্যালয় সিনেট সদস্য হিসেবে শিক্ষা সংস্কার ও শিক্ষার্থীদের অধিকার রক্ষায় গুরুত্বপূর্ণ অবদান রাখেন। এরপর কক্সবাজার জেলা আওয়ামী লীগের বিভিন্ন গুরুত্বপূর্ণ সাংগঠনিক পদে বিশ্বস্ততার সাথে দায়িত্ব পালন করেন।",
    val studentPoliticsContentEn: String = "Demonstrated charismatic student leadership as both Vice President (VP) and General Secretary (GS) of Chittagong Govt Commerce College Student Union. Later served with distinction as an elected Senate Member of the University of Chittagong.",
    
    val parliamentDutiesTitleBn: String = "জাতীয় সংসদে দায়িত্ব পালন",
    val parliamentDutiesTitleEn: String = "Parliamentary Duties (10th, 11th, 12th)",
    val parliamentDutiesContentBn: String = "২০১৪ সালের ৫ জানুয়ারি দশম, ২০১৮ সালের ৩০ ডিসেম্বর একাদশ এবং ২০২৪ সালের ৭ জানুয়ারি দ্বাদশ জাতীয় সংসদ নির্বাচনে বাংলাদেশ আওয়ামী লীগের নৌকা প্রতীকে কক্সবাজার-২ আসন থেকে টানা ৩ বার সংসদ সদস্য নির্বাচিত হন। জাতীয় সংসদে বিদ্যুৎ, জ্বালানি ও খনিজ সম্পদ এবং পরিকল্পনা বিষয়ক স্থায়ী কমিটিতে গুরুত্বপূর্ণ ভূমিকা পালন করেন।",
    val parliamentDutiesContentEn: String = "Elected to the 10th, 11th, and 12th National Parliaments representing Cox's Bazar-2. Actively served on key parliamentary standing committees advocating national coastal security, energy infrastructure, and maritime connectivity.",
    
    val publicActivitiesTitleBn: String = "জনসংযোগ ও প্রাতিষ্ঠানিক কর্মকাণ্ড",
    val publicActivitiesTitleEn: String = "Public Relations & Institutional Work",
    val publicActivitiesContentBn: String = "মহেশখালী ও কুতুবদিয়ার চরাঞ্চল ও দ্বীপবাসীদের জন্য মাতারবাড়ি গভীর সমুদ্রবন্দর, কয়লাভিত্তিক বিদ্যুৎ কেন্দ্র, সাবমেরিন ক্যাবল বিদ্যুৎ সংযোগ, সাইক্লোন শেল্টার এবং বন্যা নিয়ন্ত্রণ বেড়িবাঁধ বাস্তবায়নে অগ্রণী ভূমিকা পালন করেন। এছাড়াও অসংখ্য স্কুল, কলেজ, মাদ্রাসা ও জনকল্যাণমূলক প্রতিষ্ঠানের প্রতিষ্ঠাতা ও পৃষ্ঠপোষক।",
    val publicActivitiesContentEn: String = "Spearheaded monumental socio-economic projects including the Matarbari Deep Sea Port, submarine power cables, disaster shelters, climate dykes, and patronized dozens of colleges, schools, and charitable foundations.",

    // Table / Grid of Personal & Professional Info (Key-Values)
    val educationBn: String = "বি.কম (অনার্স), এম.কম (হিসাববিজ্ঞান), চট্টগ্রাম সরকারি কমার্স কলেজ",
    val educationEn: String = "B.Com (Hons), M.Com (Accounting), Chittagong Govt Commerce College",
    val professionBn: String = "রাজনীতিবিদ ও ব্যবসায়ী",
    val professionEn: String = "Politician & Business Professional",
    val politicalPartyBn: String = "বাংলাদেশ আওয়ামী লীগ",
    val politicalPartyEn: String = "Bangladesh Awami League",
    val constituencyBn: String = "কক্সবাজার-২ (মহেশখালী–কুতুবদিয়া)",
    val constituencyEn: String = "Cox's Bazar-2 (Moheshkhali–Kutubdia)",
    val parliamentTermsBn: String = "১০ম, ১১শ ও ১২শ জাতীয় সংসদ (২০১৪ – ২০২৪)",
    val parliamentTermsEn: String = "10th, 11th & 12th National Parliament (2014 – 2024)",
    val collegeUnionBn: String = "সাবেক ভিপি (সহ-সভাপতি) ও জিএস, চট্টগ্রাম সরকারি কমার্স কলেজ ছাত্র সংসদ",
    val collegeUnionEn: String = "Former VP & GS, Chittagong Govt Commerce College Student Union",
    val universitySenateBn: String = "সাবেক সিনেট সদস্য, চট্টগ্রাম বিশ্ববিদ্যালয়",
    val universitySenateEn: String = "Former Senate Member, University of Chittagong",
    
    // Full Biography text for dialog
    val fullBiographyBn: String = "আশেক উল্লাহ রফিক (জন্ম: ২৪ জুলাই ১৯৭১) একজন বাংলাদেশি রাজনীতিবিদ ও সাবেক সংসদ সদস্য। তিনি বাংলাদেশ আওয়ামী লীগের প্রার্থী হিসেবে কক্সবাজার-২ (মহেশখালী-কুতুবদিয়া) আসন থেকে ২০১৪, ২০১৮ এবং ২০২৪ সালের জাতীয় সংসদ নির্বাচনে টানা তিন মেয়াদে সংসদ সদস্য নির্বাচিত হন।\n\nপ্রারম্ভিক জীবন ও শিক্ষাজীবন:\nতিনি ১৯৭১ সালের ২৪ জুলাই কক্সবাজারের এক ঐতিহ্যবাহী রাজনৈতিক পরিবারে জন্মগ্রহণ করেন। তিনি চট্টগ্রাম সরকারি কমার্স কলেজ থেকে হিসাববিজ্ঞানে স্নাতক (সম্মান) ও স্নাতকোত্তর ডিগ্রি লাভ করেন।\n\nরাজনৈতিক জীবন ও জনসেবা:\nছাত্রজীবন থেকেই তিনি বাংলাদেশ ছাত্রলীগের রাজনীতির সাথে যুক্ত হন এবং চট্টগ্রাম সরকারি কমার্স কলেজ ছাত্র সংসদের জিএস ও পরবর্তীতে ভিপি নির্বাচিত হন। তিনি চট্টগ্রাম বিশ্ববিদ্যালয় সিনেটের নির্বাচিত সদস্য ছিলেন। ২০১৪ সালের দশম জাতীয় সংসদ নির্বাচনে তিনি কক্সবাজার-২ আসন থেকে প্রথমবার সংসদ সদস্য নির্বাচিত হন এবং পরবর্তীতে একাদশ ও দ্বাদশ সংসদে পুনঃনির্বাচিত হন।\n\nদ্বীপবাসীর উন্নয়ন ও অবদান:\nমহেশখালী ও কুতুবদিয়া উপজেলার দ্বীপবাসীর জীবনমান উন্নয়নে তিনি বিশেষ ভূমিকা পালন করেন। মাতারবাড়ি গভীর সমুদ্রবন্দর, মাতারবাড়ি ১২০০ মেগাওয়াট কয়লা বিদ্যুৎ কেন্দ্র, সাবমেরিন ক্যাবলের মাধ্যমে কুতুবদিয়া দ্বীপে শতভাগ বিদ্যুৎ সরবরাহ, সিসি ব্লক বেড়িবাঁধ নির্মাণ এবং নতুন সড়ক ও সেতু নির্মাণে তাঁর অবদান ঐতিহাসিক। এছাড়া উপকূলীয় দুর্যোগে মানুষের পাশে দাঁড়ানো ও সামাজিক কল্যাণমূলক কর্মকাণ্ডের মাধ্যমে তিনি গণমানুষের নেতা হিসেবে পরিচিতি লাভ করেন।",
    val fullBiographyEn: String = "Asheq Ullah Rafiq (born July 24, 1971) is a prominent Bangladeshi politician and former Member of Parliament for the Cox's Bazar-2 constituency comprising Moheshkhali and Kutubdia upazilas.\n\nEarly Life & Education:\nBorn on July 24, 1971, in Cox's Bazar. He completed his Bachelor of Commerce (Honours) and Master of Commerce in Accounting from Chittagong Government Commerce College.\n\nPolitical Career:\nStarting in student politics, he served with distinction as both General Secretary (GS) and Vice President (VP) of the Chittagong Government Commerce College Student Union. He was an elected member of the Senate of the University of Chittagong. He was elected as MP in the 10th (2014), 11th (2018), and 12th (2024) National Parliaments.\n\nKey Contributions to the Coastal Islands:\nDuring his tenure, the coastal islands underwent massive economic transformation, hosting the Matarbari Deep Sea Port, ultra-supercritical coal power generation, 100% submarine cable electrification for Kutubdia, modern disaster-resilient CC-block embankments, and extensive school, health, and transport infrastructure."
)

@IgnoreExtraProperties
data class DigitalServiceItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val authorityBn: String = "",
    val authorityEn: String = "",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val iconKey: String = "badge",
    val subTextBn: String = "",
    val subTextEn: String = "",
    val linkUrl: String = "",
    val portalUrl: String = "",
    val category: String = "CITIZEN",
    val colorHex: Long = 0xFF1976D2,
    val actionType: String = "WEB_BROWSER", // WEB_BROWSER, APP_INTENT
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class DailyScheduleItem(
    val id: String = "",
    val eventTitleBn: String = "",
    val eventTitleEn: String = "",
    val eventDateBn: String = "",
    val eventDateEn: String = "",
    val locationBn: String = "",
    val locationEn: String = "",
    val categoryBn: String = "উন্নয়ন পরিদর্শন",
    val categoryEn: String = "Development Inspection",
    val timeBn: String = "সকাল ১০:০০ মি.",
    val timeEn: String = "10:00 AM",
    val statusBn: String = "নির্ধারিত",
    val statusEn: String = "Scheduled",
    val timestamp: Long = 0L, // For sorting
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class AppointmentRequest(
    val id: String = "",
    val userId: String = "",
    val trackingId: String = "",
    val trackingNumber: String = "",
    val name: String = "",
    val phone: String = "",
    val mobile: String = "",
    val upazila: String = "",
    val union: String = "",
    val category: String = "General", // "General", "Development", "Public Service", "Appointment"
    val type: String = "",
    val message: String = "",
    val attachmentUrl: String = "",
    val status: String = "Pending", // "Pending" (আবেদন গ্রহণ), "REVIEWING" (পর্যালোচনাধীন), "SCHEDULED" (সময় নির্ধারিত), "COMPLETED" (সম্পন্ন), "CANCELLED" (বাতিল)
    val adminNote: String = "",
    val scheduledDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class GovtReportItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val categoryBn: String = "সরকারি গেজেট", // "সরকারি গেজেট", "প্রকল্প প্রতিবেদন", "অডিট ও মূল্যায়ন", "পরিবেশ সমীক্ষা"
    val categoryEn: String = "Official Gazette",
    val publishDateBn: String = "অক্টোবর ২০২৪",
    val publishDateEn: String = "October 2024",
    val year: Int = 2024,
    val pdfUrl: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "PDF", // "PDF", "IMAGE", "VIDEO"
    val ministryOrDeptBn: String = "পরিকল্পনা মন্ত্রণালয় ও স্থানীয় সরকার বিভাগ",
    val ministryOrDeptEn: String = "Ministry of Planning & LGRD",
    val fileSizeMb: String = "2.5 MB",
    val downloadCount: Int = 0,
    val isVerified: Boolean = true,
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class FaqItem(
    val id: String = "",
    val questionBn: String = "",
    val questionEn: String = "",
    val answerBn: String = "",
    val answerEn: String = "",
    val categoryBn: String = "সাধারণ জিজ্ঞাসা",
    val categoryEn: String = "General FAQ",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class UnionRepresentative(
    val id: String = "",
    val upazila: String = "মহেশখালী", // "মহেশখালী" or "কুতুবদিয়া"
    val unionNameBn: String = "",
    val unionNameEn: String = "",
    val chairmanNameBn: String = "",
    val chairmanPhone: String = "01800000000",
    val secretaryNameBn: String = "",
    val secretaryPhone: String = "01800000000",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class SocialMediaLink(
    val id: String = "",
    val platformNameBn: String = "",
    val platformNameEn: String = "",
    val url: String = "",
    val iconType: String = "FACEBOOK", // "FACEBOOK", "INSTAGRAM", "TWITTER", "YOUTUBE", "LINKEDIN"
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class EducationResourceItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val categoryBn: String = "",
    val categoryEn: String = "",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val url: String = "",
    val documentUrl: String = "",
    val type: String = "SCHOLARSHIP", // "SCHOLARSHIP", "RESULTS", "BOARD"
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class EconomyOpportunityItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val sectorBn: String = "",
    val sectorEn: String = "",
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val actionTextBn: String = "",
    val actionTextEn: String = "",
    val url: String? = null,
    val circularPdfUrl: String = "",
    val tabKey: String = "MEGA", // "MEGA", "COASTAL", "JOBS"
    val badgeColorHex: Long = 0xFF1976D2,
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class WelfareProgramItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val targetBn: String = "",
    val targetEn: String = "",
    val eligibilityBn: String = "",
    val eligibilityEn: String = "",
    val rateBn: String = "",
    val rateEn: String = "",
    val applyUrl: String = "",
    val documentUrl: String = "",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class UnionServiceHub(
    val id: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val upazilaBn: String = "",
    val upazilaEn: String = "",
    val chairmanBn: String = "",
    val chairmanPhone: String = "",
    val udcNameBn: String = "",
    val udcPhone: String = "",
    val webUrl: String = "",
    val servicesBn: String = "",
    val servicesEn: String = "",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class DevelopmentCategoryItem(
    val id: String = "",
    val labelBn: String = "",
    val labelEn: String = "",
    val iconKey: String = "category",
    val matchKeywords: List<String> = emptyList(),
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class HomepageCategoryItem(
    val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val subtitleBn: String = "",
    val subtitleEn: String = "",
    val badgeBn: String = "",
    val badgeEn: String = "",
    val iconKey: String = "badge",
    val colorHex: Long = 0xFF1976D2,
    val placement: String = "GRID", // "GRID" or "FEATURED_BANNER"
    val actionTarget: String = "",
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)

@IgnoreExtraProperties
data class CitizenPollOption(
    val labelBn: String = "",
    val labelEn: String = "",
    val votes: Int = 0
)

@IgnoreExtraProperties
data class CitizenPollItem(
    val id: String = "default",
    val titleBn: String = "জনমত ও নাগরিক অংশগ্রহণ",
    val titleEn: String = "Citizen Poll & Voice",
    val questionBn: String = "",
    val questionEn: String = "",
    val options: List<CitizenPollOption> = emptyList(),
    val order: Int = 0,
    @get:PropertyName("isPublished")
    @PropertyName("isPublished")
    val isPublished: Boolean = true
)
