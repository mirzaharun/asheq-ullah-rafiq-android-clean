package com.example

import com.example.data.model.DevelopmentProject
import com.example.ui.screens.STANDARD_DEVELOPMENT_CATEGORIES
import com.example.ui.screens.matches
import com.example.ui.screens.projectMatchesUpazila
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun developmentCategories_hasAllElevenRequiredCategories() {
    assertEquals(11, STANDARD_DEVELOPMENT_CATEGORIES.size)
    val expectedNames = listOf(
      "বিদ্যুৎ ও জ্বালানি",
      "সড়ক ও যোগাযোগ",
      "বেড়িবাঁধ ও দুর্যোগ ব্যবস্থাপনা",
      "শিক্ষা",
      "স্বাস্থ্যসেবা",
      "অবকাঠামো",
      "বন্দর ও নৌপথ",
      "পর্যটন",
      "পানি ও স্যানিটেশন",
      "কৃষি ও মৎস্য",
      "সামাজিক ও জনকল্যাণমূলক উন্নয়ন"
    )
    assertEquals(expectedNames, STANDARD_DEVELOPMENT_CATEGORIES.map { it.labelBn })
  }

  @Test
  fun developmentFilters_matchExistingFirestoreUpazilaAndCategoryFields() {
    val powerProj = DevelopmentProject(id = "power", upazila = "Moheshkhali", categoryBn = "বিদ্যুৎ", categoryEn = "Power & Energy")
    val kutubHealth = DevelopmentProject(id = "health", upazila = "Kutubdia", categoryBn = "স্বাস্থ্য", categoryEn = "Health")
    val decomposedRoad = DevelopmentProject(id = "road2", upazila = "কুতুবদিয়া", categoryBn = "সড়ক ও যোগাযোগ", categoryEn = "Roads & Connectivity")
    val embankmentProj = DevelopmentProject(id = "shield", upazila = "মহেশখালী", categoryBn = "বেড়িবাঁধ ও দুর্যোগ ব্যবস্থাপনা", categoryEn = "")

    assertTrue(projectMatchesUpazila(powerProj, "Moheshkhali"))
    assertFalse(projectMatchesUpazila(powerProj, "Kutubdia"))
    assertTrue(projectMatchesUpazila(kutubHealth, "Kutubdia"))
    assertTrue(projectMatchesUpazila(decomposedRoad, "Kutubdia"))
    assertTrue(projectMatchesUpazila(embankmentProj, "Moheshkhali"))

    val byId = STANDARD_DEVELOPMENT_CATEGORIES.associateBy { it.id }
    assertTrue(byId.getValue("power_energy").matches(powerProj))
    assertFalse(byId.getValue("education").matches(powerProj))
    assertTrue(byId.getValue("healthcare").matches(kutubHealth))
    assertTrue(byId.getValue("roads_comm").matches(decomposedRoad))
    assertTrue(byId.getValue("embankment_disaster").matches(embankmentProj))
  }

  @Test
  fun footerModals_andVerifiedProfile_areDistinctAndComplete() {
    val about: com.example.ui.viewmodel.ActiveModal = com.example.ui.viewmodel.ActiveModal.AboutModal
    val privacy: com.example.ui.viewmodel.ActiveModal = com.example.ui.viewmodel.ActiveModal.PrivacyModal
    val terms: com.example.ui.viewmodel.ActiveModal = com.example.ui.viewmodel.ActiveModal.TermsModal
    val feedback: com.example.ui.viewmodel.ActiveModal = com.example.ui.viewmodel.ActiveModal.FeedbackModal

    assertEquals(4, setOf(about, privacy, terms, feedback).size)

    val profile = com.example.data.model.ProfileInfo()
    assertTrue(profile.nameBn.contains("আশেক উল্লাহ রফিক"))
    assertTrue(profile.nameEn.contains("Asheq Ullah Rafiq"))
    assertTrue(profile.birthDateBn.contains("১৯৭১"))
    assertTrue(profile.fullBiographyBn.isNotBlank())
    assertTrue(profile.fullBiographyEn.isNotBlank())
  }

  @Test
  fun firestoreParsers_handlesMixedTimestampTypesAndLegacyFields() {
    val legacyTs = com.google.firebase.Timestamp(1_730_000_000L, 500_000_000)
    val parsedFromTimestamp = com.example.data.repository.FirestoreParsers.parseTimestampMillis(legacyTs)
    assertEquals(1_730_000_000_500L, parsedFromTimestamp)

    val parsedFromMap = com.example.data.repository.FirestoreParsers.parseTimestampMillis(
      mapOf("seconds" to 1_730_000_000L, "nanoseconds" to 250_000_000L)
    )
    assertEquals(1_730_000_000_250L, parsedFromMap)

    val parsedFromIso = com.example.data.repository.FirestoreParsers.parseTimestampMillis("2026-01-15T10:00:00Z")
    assertTrue(parsedFromIso > 0L)

    val legacyDocMap: Map<String, Any?> = mapOf(
      "userId" to "citizen_123",
      "trackingId" to "APP-9Z8Y7X",
      "mobile" to "01812345678",
      "type" to "জনসেবা আবেদন",
      "name" to "করিম উদ্দিন",
      "message" to "সড়ক সংস্কার আবেদন",
      "status" to "SCHEDULED",
      "createdAt" to legacyTs
    )
    val normalized = com.example.data.repository.FirestoreParsers.parseAppointmentMap("doc_1", legacyDocMap)
    assertEquals("doc_1", normalized.id)
    assertEquals("APP-9Z8Y7X", normalized.trackingNumber)
    assertEquals("APP-9Z8Y7X", normalized.trackingId)
    assertEquals("01812345678", normalized.phone)
    assertEquals("01812345678", normalized.mobile)
    assertEquals("জনসেবা আবেদন", normalized.category)
    assertEquals("জনসেবা আবেদন", normalized.type)
    assertEquals(1_730_000_000_500L, normalized.createdAt)
  }

  @Test
  fun noticeParserAndInAppNotificationBuilder_filterDraftsAndIsolateUserUpdates() {
    val publishedNotice = com.example.data.repository.FirestoreParsers.parseNoticeMap(
      "n_pub",
      mapOf("titleBn" to "প্রকাশিত নোটিশ", "isPublished" to true, "createdAt" to 2000L)
    )
    val draftNotice = com.example.data.repository.FirestoreParsers.parseNoticeMap(
      "n_draft",
      mapOf("titleBn" to "অপ্রকাশিত খসড়া", "isPublished" to false, "createdAt" to 3000L)
    )
    val webDraftUrgentNotice = com.example.data.repository.FirestoreParsers.parseNoticeMap(
      "n_web_draft",
      mapOf("titleBn" to "খসড়া বিজ্ঞপ্তি", "urgent" to true, "published" to false, "createdAt" to 3500L)
    )
    assertTrue(publishedNotice.isPublished)
    assertFalse(draftNotice.isPublished)
    assertTrue(webDraftUrgentNotice.isUrgent)
    assertFalse(webDraftUrgentNotice.isPublished)

    val aliceApp = com.example.data.model.AppointmentRequest(
      id = "app_a",
      userId = "alice_uid",
      trackingNumber = "APP-ALICE",
      status = "SCHEDULED",
      scheduledDate = "২০ মে",
      timestamp = 5000L
    )
    val bobApp = com.example.data.model.AppointmentRequest(
      id = "app_b",
      userId = "bob_uid",
      trackingNumber = "APP-BOB",
      status = "Resolved",
      timestamp = 6000L
    )

    val aliceFeed = com.example.util.InAppNotificationHelper.buildNotifications(
      notices = listOf(publishedNotice, draftNotice),
      appointments = listOf(aliceApp, bobApp),
      grievances = emptyList(),
      currentUserId = "alice_uid"
    )
    assertTrue(aliceFeed.any { it.sourceId == "n_pub" })
    assertFalse(aliceFeed.any { it.sourceId == "n_draft" })
    assertTrue(aliceFeed.any { it.trackingId == "APP-ALICE" })
    assertFalse(aliceFeed.any { it.trackingId == "APP-BOB" })

    val guestFeed = com.example.util.InAppNotificationHelper.buildNotifications(
      notices = listOf(publishedNotice, draftNotice),
      appointments = listOf(aliceApp, bobApp),
      grievances = emptyList(),
      currentUserId = null
    )
    assertEquals(1, guestFeed.size)
    assertEquals("n_pub", guestFeed.first().sourceId)
  }

  @Test
  fun defaultPortalContent_matchesExpectedRecordCounts() {
    assertEquals(9, com.example.data.repository.DefaultPortalContent.defaultDigitalServices.size)
    assertEquals(17, com.example.data.repository.DefaultPortalContent.defaultEducationResources.size)
    assertEquals(7, com.example.data.repository.DefaultPortalContent.defaultEconomyOpportunities.size)
    assertEquals(5, com.example.data.repository.DefaultPortalContent.defaultWelfarePrograms.size)
    assertEquals(14, com.example.data.repository.DefaultPortalContent.defaultUnionServices.size)
    assertEquals(11, com.example.data.repository.DefaultPortalContent.defaultDevelopmentCategories.size)
    assertEquals(10, com.example.data.repository.DefaultPortalContent.defaultHomepageCategories.size)
    assertEquals(4, com.example.data.repository.DefaultPortalContent.defaultCitizenPoll.options.size)
  }

  @Test
  fun newFirestoreParsers_handleUrlFallbackColorHexAndPublishFlags() {
    val ds = com.example.data.repository.FirestoreParsers.parseDigitalServiceMap(
      "ds_1",
      mapOf(
        "titleBn" to "ই-নামজারি",
        "linkUrl" to "https://mutation.land.gov.bd",
        "colorHex" to "#1976D2",
        "isPublished" to false
      )
    )
    assertEquals("https://mutation.land.gov.bd", ds.portalUrl)
    assertEquals("https://mutation.land.gov.bd", ds.linkUrl)
    assertEquals(0xFF1976D2, ds.colorHex)
    assertFalse(ds.isPublished)

    val edu = com.example.data.repository.FirestoreParsers.parseEducationResourceMap(
      "edu_1",
      mapOf("titleBn" to "উপবৃত্তি", "type" to "SCHOLARSHIP", "documentUrl" to "https://cdn.example.com/form.pdf")
    )
    assertEquals("https://cdn.example.com/form.pdf", edu.url)
    assertEquals("https://cdn.example.com/form.pdf", edu.documentUrl)
    assertTrue(edu.isPublished)

    val econ = com.example.data.repository.FirestoreParsers.parseEconomyOpportunityMap(
      "ec_1",
      mapOf("titleBn" to "প্রশিক্ষণ", "tabKey" to "JOBS", "circularPdfUrl" to "https://cdn.example.com/job.pdf", "badgeColorHex" to "0xFF2E7D32")
    )
    assertEquals("https://cdn.example.com/job.pdf", econ.url)
    assertEquals(0xFF2E7D32, econ.badgeColorHex)

    val poll = com.example.data.repository.FirestoreParsers.parseCitizenPollMap(
      "default",
      mapOf(
        "questionBn" to "অগ্রাধিকার কোন খাত?",
        "options" to listOf(
          mapOf("labelBn" to "বেড়িবাঁধ", "labelEn" to "Embankments", "votes" to 120)
        )
      )
    )
    assertEquals("default", poll.id)
    assertEquals(1, poll.options.size)
    assertEquals(120, poll.options.first().votes)
  }
}
