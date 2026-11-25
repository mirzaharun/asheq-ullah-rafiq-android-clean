package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.time.Instant
import java.util.Date

/**
 * Crash-safe Firestore parsing and legacy-vs-canonical field normalization utilities.
 * Handles mixed Timestamp, Date, Number, Map({_seconds/_nanoseconds}), and ISO/numeric String values
 * without throwing RuntimeException during real-time snapshot updates.
 */
object FirestoreParsers {
    private const val TAG = "FirestoreParsers"

    fun parseTimestampMillis(value: Any?, default: Long = 0L): Long {
        if (value == null) return default
        return when (value) {
            is Timestamp -> value.toDate().time
            is Date -> value.time
            is Number -> value.toLong()
            is Map<*, *> -> {
                val seconds = (value["seconds"] as? Number ?: value["_seconds"] as? Number)?.toLong()
                    ?: return default
                val nanos = (value["nanoseconds"] as? Number ?: value["_nanoseconds"] as? Number)?.toLong()
                    ?: 0L
                seconds * 1000L + (nanos / 1_000_000L)
            }
            is String -> {
                val trimmed = value.trim()
                trimmed.toLongOrNull() ?: try {
                    Instant.parse(trimmed).toEpochMilli()
                } catch (_: Exception) {
                    default
                }
            }
            else -> default
        }
    }

    fun readString(data: Map<String, Any?>, vararg keys: String, default: String = ""): String {
        for (key in keys) {
            val raw = data[key] ?: continue
            val str = raw.toString().trim()
            if (str.isNotEmpty() && str != "null") return str
        }
        return default
    }

    fun readBoolean(data: Map<String, Any?>, vararg keys: String, default: Boolean = false): Boolean {
        for (key in keys) {
            val raw = data[key] ?: continue
            return when (raw) {
                is Boolean -> raw
                is String -> raw.equals("true", ignoreCase = true)
                is Number -> raw.toInt() != 0
                else -> default
            }
        }
        return default
    }

    fun readInt(data: Map<String, Any?>, vararg keys: String, default: Int = 0): Int {
        for (key in keys) {
            val raw = data[key] ?: continue
            when (raw) {
                is Number -> return raw.toInt()
                is String -> raw.trim().toIntOrNull()?.let { return it }
            }
        }
        return default
    }

    fun readLong(data: Map<String, Any?>, vararg keys: String, default: Long = 0L): Long {
        for (key in keys) {
            val raw = data[key] ?: continue
            val parsed = parseTimestampMillis(raw, Long.MIN_VALUE)
            if (parsed != Long.MIN_VALUE) return parsed
        }
        return default
    }

    fun readColorHex(data: Map<String, Any?>, vararg keys: String, default: Long = 0xFF1976D2): Long {
        for (key in keys) {
            val raw = data[key] ?: continue
            when (raw) {
                is Number -> return raw.toLong()
                is String -> {
                    val clean = raw.trim()
                    if (clean.isEmpty()) continue
                    val parsed = when {
                        clean.startsWith("#") -> {
                            val hex = clean.removePrefix("#")
                            val fullHex = if (hex.length == 6) "FF$hex" else hex
                            fullHex.toLongOrNull(16)
                        }
                        clean.startsWith("0x", ignoreCase = true) -> {
                            clean.substring(2).toLongOrNull(16)
                        }
                        else -> clean.toLongOrNull()
                    }
                    if (parsed != null) return parsed
                }
            }
        }
        return default
    }

    fun readStringList(data: Map<String, Any?>, vararg keys: String): List<String> {
        for (key in keys) {
            val raw = data[key] ?: continue
            if (raw is List<*>) {
                return raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() && s != "null" } }
            }
            if (raw is String && raw.isNotBlank()) {
                return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
        return emptyList()
    }

    fun readPublished(data: Map<String, Any?>): Boolean {
        return readBoolean(data, "isPublished", "published", default = true)
    }

    fun <T> safeToObject(doc: DocumentSnapshot, clazz: Class<T>): T? {
        return try {
            doc.toObject(clazz)
        } catch (e: Exception) {
            Log.w(TAG, "Skipping/fallback malformed doc ${doc.id} for ${clazz.simpleName}: ${e.message}")
            null
        }
    }

    fun parseAppointmentMap(docId: String, data: Map<String, Any?>): AppointmentRequest {
        val resolvedTracking = readString(data, "trackingNumber", "trackingId")
        val resolvedPhone = readString(data, "phone", "mobile")
        val resolvedCategory = readString(data, "category", "type", default = "General")
        val resolvedCreatedAt = readLong(data, "createdAt", "serverCreatedAt", "timestamp", default = 0L)
        val resolvedTimestamp = readLong(data, "timestamp", "serverUpdatedAt", "serverCreatedAt", "createdAt", default = resolvedCreatedAt)

        return AppointmentRequest(
            id = docId.ifBlank { readString(data, "id") },
            userId = readString(data, "userId"),
            trackingId = resolvedTracking,
            trackingNumber = resolvedTracking,
            name = readString(data, "name", "fullName"),
            phone = resolvedPhone,
            mobile = resolvedPhone,
            upazila = readString(data, "upazila"),
            union = readString(data, "union"),
            category = resolvedCategory,
            type = readString(data, "type", "category", default = resolvedCategory),
            message = readString(data, "message", "details"),
            attachmentUrl = readString(data, "attachmentUrl"),
            status = readString(data, "status", default = "Submitted"),
            adminNote = readString(data, "adminNote"),
            scheduledDate = readString(data, "scheduledDate"),
            createdAt = resolvedCreatedAt,
            timestamp = resolvedTimestamp
        )
    }

    fun parseAppointment(doc: DocumentSnapshot): AppointmentRequest? {
        val data = doc.data ?: return null
        return parseAppointmentMap(doc.id, data)
    }

    fun parseNoticeMap(docId: String, data: Map<String, Any?>): NoticeFeedItem {
        val created = readLong(data, "createdAt", "timestamp", "serverCreatedAt", default = 0L)
        return NoticeFeedItem(
            id = docId.ifBlank { readString(data, "id") },
            titleBn = readString(data, "titleBn", "title"),
            titleEn = readString(data, "titleEn", "titleBn", "title"),
            dateBn = readString(data, "dateBn", "date"),
            dateEn = readString(data, "dateEn", "dateBn", "date"),
            categoryBn = readString(data, "categoryBn", "category"),
            categoryEn = readString(data, "categoryEn", "categoryBn", "category"),
            detailsBn = readString(data, "detailsBn", "details", "descriptionBn"),
            detailsEn = readString(data, "detailsEn", "detailsBn", "details"),
            isUrgent = readBoolean(data, "isUrgent", "urgent", default = false),
            isPublished = readPublished(data),
            createdAt = created
        )
    }

    fun parseNotice(doc: DocumentSnapshot): NoticeFeedItem? {
        val data = doc.data ?: return null
        return parseNoticeMap(doc.id, data)
    }

    fun parseGrievanceMap(docId: String, data: Map<String, Any?>): CitizenGrievance {
        return CitizenGrievance(
            id = docId.ifBlank { readString(data, "id") },
            userId = readString(data, "userId"),
            fullName = readString(data, "fullName", "name"),
            phone = readString(data, "phone", "mobile"),
            upazila = readString(data, "upazila"),
            subject = readString(data, "subject", "category"),
            details = readString(data, "details", "message"),
            status = readString(data, "status", default = "Pending"),
            createdAtFormatted = readString(data, "createdAtFormatted", "dateBn", default = "আজ")
        )
    }

    fun parseGrievance(doc: DocumentSnapshot): CitizenGrievance? {
        val data = doc.data ?: return null
        return parseGrievanceMap(doc.id, data)
    }

    fun parseDailySchedule(doc: DocumentSnapshot): DailyScheduleItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, DailyScheduleItem::class.java)?.copy(id = doc.id, isPublished = published)
            ?: DailyScheduleItem(
                id = doc.id,
                eventTitleBn = readString(data, "eventTitleBn", "titleBn"),
                eventTitleEn = readString(data, "eventTitleEn", "titleEn"),
                eventDateBn = readString(data, "eventDateBn", "dateBn"),
                eventDateEn = readString(data, "eventDateEn", "dateEn"),
                locationBn = readString(data, "locationBn"),
                locationEn = readString(data, "locationEn"),
                categoryBn = readString(data, "categoryBn", default = "উন্নয়ন পরিদর্শন"),
                categoryEn = readString(data, "categoryEn", default = "Development Inspection"),
                timeBn = readString(data, "timeBn", default = "সকাল ১০:০০ মি."),
                timeEn = readString(data, "timeEn", default = "10:00 AM"),
                statusBn = readString(data, "statusBn", default = "নির্ধারিত"),
                statusEn = readString(data, "statusEn", default = "Scheduled"),
                timestamp = readLong(data, "timestamp", "createdAt", default = 0L),
                order = readInt(data, "order", default = 0),
                isPublished = published
            )
    }

    fun parseDigitalServiceMap(docId: String, data: Map<String, Any?>): DigitalServiceItem {
        val resolvedUrl = readString(data, "portalUrl", "linkUrl", "url")
        return DigitalServiceItem(
            id = docId.ifBlank { readString(data, "id") },
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            authorityBn = readString(data, "authorityBn"),
            authorityEn = readString(data, "authorityEn", "authorityBn"),
            descriptionBn = readString(data, "descriptionBn"),
            descriptionEn = readString(data, "descriptionEn", "descriptionBn"),
            iconKey = readString(data, "iconKey", default = "badge"),
            subTextBn = readString(data, "subTextBn"),
            subTextEn = readString(data, "subTextEn", "subTextBn"),
            linkUrl = readString(data, "linkUrl", "portalUrl", default = resolvedUrl),
            portalUrl = readString(data, "portalUrl", "linkUrl", default = resolvedUrl),
            category = readString(data, "category", default = "CITIZEN"),
            colorHex = readColorHex(data, "colorHex", default = 0xFF1976D2),
            actionType = readString(data, "actionType", default = "WEB_BROWSER"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseDigitalService(doc: DocumentSnapshot): DigitalServiceItem? {
        val data = doc.data ?: return null
        return parseDigitalServiceMap(doc.id, data)
    }

    fun parseTourismSpotMap(docId: String, data: Map<String, Any?>): TourismSpot {
        val id = docId.ifBlank { readString(data, "spotId", "id") }
        return TourismSpot(
            spotId = id,
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            category = readString(data, "category", default = "Moheshkhali"),
            coverImageUrl = readString(data, "coverImageUrl", "imageUrl"),
            descriptionBn = readString(data, "descriptionBn"),
            descriptionEn = readString(data, "descriptionEn", "descriptionBn"),
            travelRouteDetailsBn = readString(data, "travelRouteDetailsBn"),
            travelRouteDetailsEn = readString(data, "travelRouteDetailsEn", "travelRouteDetailsBn"),
            transportTypes = readStringList(data, "transportTypes"),
            approxFareBdt = readString(data, "approxFareBdt"),
            recommendedHotelsBn = readStringList(data, "recommendedHotelsBn"),
            recommendedHotelsEn = readStringList(data, "recommendedHotelsEn"),
            seafoodRestaurantsBn = readStringList(data, "seafoodRestaurantsBn"),
            seafoodRestaurantsEn = readStringList(data, "seafoodRestaurantsEn"),
            googleMapsUrl = readString(data, "googleMapsUrl"),
            navigationStepsBn = readStringList(data, "navigationStepsBn"),
            navigationStepsEn = readStringList(data, "navigationStepsEn"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseTourismSpot(doc: DocumentSnapshot): TourismSpot? {
        val data = doc.data ?: return null
        return parseTourismSpotMap(doc.id, data)
    }

    fun parseDevelopmentProject(doc: DocumentSnapshot): DevelopmentProject? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, DevelopmentProject::class.java)?.copy(id = doc.id, isPublished = published)
            ?: DevelopmentProject(
                id = doc.id,
                titleBn = readString(data, "titleBn"),
                titleEn = readString(data, "titleEn", "titleBn"),
                categoryBn = readString(data, "categoryBn"),
                categoryEn = readString(data, "categoryEn", "categoryBn"),
                budgetBn = readString(data, "budgetBn"),
                budgetEn = readString(data, "budgetEn"),
                completionPercentage = readInt(data, "completionPercentage", default = 0),
                descriptionBn = readString(data, "descriptionBn"),
                descriptionEn = readString(data, "descriptionEn", "descriptionBn"),
                impactBn = readString(data, "impactBn"),
                impactEn = readString(data, "impactEn", "impactBn"),
                timelineBn = readString(data, "timelineBn", default = "২০১৪ – ২০২৪"),
                timelineEn = readString(data, "timelineEn", default = "2014 – 2024"),
                imageUrl = readString(data, "imageUrl", default = "drawable:moheshkhali_banner"),
                status = readString(data, "status", default = "Ongoing"),
                upazila = readString(data, "upazila", default = "Moheshkhali"),
                union = readString(data, "union", default = "All"),
                year = readInt(data, "year", default = 2024),
                beforeImageUrl = readString(data, "beforeImageUrl"),
                afterImageUrl = readString(data, "afterImageUrl"),
                pdfUrl = readString(data, "pdfUrl"),
                order = readInt(data, "order", default = 0),
                isPublished = published
            )
    }

    fun parseGalleryItem(doc: DocumentSnapshot): GalleryItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, GalleryItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseMediaArchiveItem(doc: DocumentSnapshot): MediaArchiveItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, MediaArchiveItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseImportantLink(doc: DocumentSnapshot): ImportantExternalLink? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, ImportantExternalLink::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseSpecialEvent(doc: DocumentSnapshot): SpecialEventItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, SpecialEventItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseEmergencyContact(doc: DocumentSnapshot): EmergencyContact? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, EmergencyContact::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseBloodDonor(doc: DocumentSnapshot): BloodDonor? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, BloodDonor::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseWatercraftSchedule(doc: DocumentSnapshot): DynamicWatercraftSchedule? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, DynamicWatercraftSchedule::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseFilterChip(doc: DocumentSnapshot): FilterChipItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, FilterChipItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseGovtReport(doc: DocumentSnapshot): GovtReportItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, GovtReportItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseFaqItem(doc: DocumentSnapshot): FaqItem? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, FaqItem::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseUnionRepresentative(doc: DocumentSnapshot): UnionRepresentative? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, UnionRepresentative::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseSocialMediaLink(doc: DocumentSnapshot): SocialMediaLink? {
        val data = doc.data ?: return null
        val published = readPublished(data)
        return safeToObject(doc, SocialMediaLink::class.java)?.copy(id = doc.id, isPublished = published)
    }

    fun parseEducationResourceMap(docId: String, data: Map<String, Any?>): EducationResourceItem {
        return EducationResourceItem(
            id = docId.ifBlank { readString(data, "id") },
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            categoryBn = readString(data, "categoryBn"),
            categoryEn = readString(data, "categoryEn", "categoryBn"),
            descriptionBn = readString(data, "descriptionBn"),
            descriptionEn = readString(data, "descriptionEn", "descriptionBn"),
            url = readString(data, "url", "portalUrl", "documentUrl"),
            documentUrl = readString(data, "documentUrl"),
            type = readString(data, "type", default = "SCHOLARSHIP"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseEducationResource(doc: DocumentSnapshot): EducationResourceItem? {
        val data = doc.data ?: return null
        return parseEducationResourceMap(doc.id, data)
    }

    fun parseEconomyOpportunityMap(docId: String, data: Map<String, Any?>): EconomyOpportunityItem {
        val resolvedUrl = readString(data, "url", "circularPdfUrl")
        return EconomyOpportunityItem(
            id = docId.ifBlank { readString(data, "id") },
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            sectorBn = readString(data, "sectorBn"),
            sectorEn = readString(data, "sectorEn", "sectorBn"),
            descriptionBn = readString(data, "descriptionBn"),
            descriptionEn = readString(data, "descriptionEn", "descriptionBn"),
            actionTextBn = readString(data, "actionTextBn", default = "বিস্তারিত দেখুন"),
            actionTextEn = readString(data, "actionTextEn", default = "View Details"),
            url = resolvedUrl.takeIf { it.isNotBlank() },
            circularPdfUrl = readString(data, "circularPdfUrl"),
            tabKey = readString(data, "tabKey", default = "MEGA"),
            badgeColorHex = readColorHex(data, "badgeColorHex", default = 0xFF1976D2),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseEconomyOpportunity(doc: DocumentSnapshot): EconomyOpportunityItem? {
        val data = doc.data ?: return null
        return parseEconomyOpportunityMap(doc.id, data)
    }

    fun parseWelfareProgramMap(docId: String, data: Map<String, Any?>): WelfareProgramItem {
        return WelfareProgramItem(
            id = docId.ifBlank { readString(data, "id") },
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            targetBn = readString(data, "targetBn"),
            targetEn = readString(data, "targetEn", "targetBn"),
            eligibilityBn = readString(data, "eligibilityBn"),
            eligibilityEn = readString(data, "eligibilityEn", "eligibilityBn"),
            rateBn = readString(data, "rateBn"),
            rateEn = readString(data, "rateEn", "rateBn"),
            applyUrl = readString(data, "applyUrl", "url", "documentUrl"),
            documentUrl = readString(data, "documentUrl"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseWelfareProgram(doc: DocumentSnapshot): WelfareProgramItem? {
        val data = doc.data ?: return null
        return parseWelfareProgramMap(doc.id, data)
    }

    fun parseUnionServiceHubMap(docId: String, data: Map<String, Any?>): UnionServiceHub {
        return UnionServiceHub(
            id = docId.ifBlank { readString(data, "id") },
            nameBn = readString(data, "nameBn"),
            nameEn = readString(data, "nameEn", "nameBn"),
            upazilaBn = readString(data, "upazilaBn"),
            upazilaEn = readString(data, "upazilaEn", "upazilaBn"),
            chairmanBn = readString(data, "chairmanBn"),
            chairmanPhone = readString(data, "chairmanPhone"),
            udcNameBn = readString(data, "udcNameBn"),
            udcPhone = readString(data, "udcPhone"),
            webUrl = readString(data, "webUrl", "url"),
            servicesBn = readString(data, "servicesBn"),
            servicesEn = readString(data, "servicesEn", "servicesBn"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseUnionServiceHub(doc: DocumentSnapshot): UnionServiceHub? {
        val data = doc.data ?: return null
        return parseUnionServiceHubMap(doc.id, data)
    }

    fun parseDevelopmentCategoryMap(docId: String, data: Map<String, Any?>): DevelopmentCategoryItem {
        return DevelopmentCategoryItem(
            id = docId.ifBlank { readString(data, "id") },
            labelBn = readString(data, "labelBn"),
            labelEn = readString(data, "labelEn", "labelBn"),
            iconKey = readString(data, "iconKey", default = "category"),
            matchKeywords = readStringList(data, "matchKeywords"),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseDevelopmentCategory(doc: DocumentSnapshot): DevelopmentCategoryItem? {
        val data = doc.data ?: return null
        return parseDevelopmentCategoryMap(doc.id, data)
    }

    fun parseHomepageCategoryMap(docId: String, data: Map<String, Any?>): HomepageCategoryItem {
        val resolvedId = docId.ifBlank { readString(data, "id") }
        return HomepageCategoryItem(
            id = resolvedId,
            titleBn = readString(data, "titleBn"),
            titleEn = readString(data, "titleEn", "titleBn"),
            subtitleBn = readString(data, "subtitleBn"),
            subtitleEn = readString(data, "subtitleEn", "subtitleBn"),
            badgeBn = readString(data, "badgeBn"),
            badgeEn = readString(data, "badgeEn", "badgeBn"),
            iconKey = readString(data, "iconKey", default = "badge"),
            colorHex = readColorHex(data, "colorHex", default = 0xFF1976D2),
            placement = readString(data, "placement", default = "GRID"),
            actionTarget = readString(data, "actionTarget", default = resolvedId),
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseHomepageCategory(doc: DocumentSnapshot): HomepageCategoryItem? {
        val data = doc.data ?: return null
        return parseHomepageCategoryMap(doc.id, data)
    }

    fun parseCitizenPollMap(docId: String, data: Map<String, Any?>): CitizenPollItem {
        val rawOptions = data["options"] as? List<*>
        val options = rawOptions?.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val typedMap = map.entries.associate { (k, v) -> k.toString() to v }
            CitizenPollOption(
                labelBn = readString(typedMap, "labelBn"),
                labelEn = readString(typedMap, "labelEn", "labelBn"),
                votes = readInt(typedMap, "votes", default = 0)
            )
        }.orEmpty()

        return CitizenPollItem(
            id = docId.ifBlank { readString(data, "id", default = "default") },
            titleBn = readString(data, "titleBn", default = "জনমত ও নাগরিক অংশগ্রহণ"),
            titleEn = readString(data, "titleEn", default = "Citizen Poll & Voice"),
            questionBn = readString(data, "questionBn"),
            questionEn = readString(data, "questionEn", "questionBn"),
            options = options,
            order = readInt(data, "order", default = 0),
            isPublished = readPublished(data)
        )
    }

    fun parseCitizenPoll(doc: DocumentSnapshot): CitizenPollItem? {
        val data = doc.data ?: return null
        return parseCitizenPollMap(doc.id, data)
    }
}
