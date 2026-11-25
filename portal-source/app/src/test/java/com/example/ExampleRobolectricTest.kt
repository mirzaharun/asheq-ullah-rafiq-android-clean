package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppLanguage
import com.example.data.repository.PortalRepository
import com.example.ui.viewmodel.PortalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Asheq Ullah Rafiq", appName)
  }

  @Test
  fun `verify portal repository data models for Moheshkhali and Kutubdia`() {
    val repo = PortalRepository()
    val spots = repo.tourismSpots.value
    assertTrue("Should have tourism spots", spots.isNotEmpty())

    val adinath = spots.find { it.spotId == "adinath_temple" }
    assertNotNull("Adinath Temple spot should exist", adinath)
    assertEquals("Moheshkhali", adinath?.category)

    val lighthouse = spots.find { it.spotId == "kutubdia_lighthouse" }
    assertNotNull("Kutubdia Lighthouse spot should exist", lighthouse)
    assertEquals("Kutubdia", lighthouse?.category)

    val emergencies = repo.emergencyContacts.value
    assertTrue("Should have emergency contacts", emergencies.isNotEmpty())

    val weather = repo.weatherData.value
    assertEquals(1, weather.maritimeSignal)
  }

  @Test
  fun `verify language toggle in ViewModel`() {
    val vm = PortalViewModel()
    assertEquals(AppLanguage.BN, vm.language.value)
    vm.toggleLanguage()
    assertEquals(AppLanguage.EN, vm.language.value)
    vm.toggleLanguage()
    assertEquals(AppLanguage.BN, vm.language.value)
  }

  @Test
  fun `verify profile info data model and repository integration`() {
    val repo = PortalRepository()
    val profile = repo.profileInfo.value
    assertNotNull("Profile info should not be null", profile)
    assertEquals("আশেক উল্লাহ রফিক", profile.nameBn)
    assertEquals("Asheq Ullah Rafiq", profile.nameEn)
    assertTrue("Should have DOB", profile.birthDateBn.contains("২৪ জুলাই ১৯৭১"))
    assertTrue("Should have birthplace", profile.birthPlaceBn.contains("কক্সবাজার"))
    assertTrue("Should have college union leadership", profile.collegeUnionBn.contains("ভিপি"))
    assertTrue("Should have university senate membership", profile.universitySenateBn.contains("চট্টগ্রাম বিশ্ববিদ্যালয়"))
    assertTrue("Should have full biography text", profile.fullBiographyBn.isNotEmpty())
  }

  @Test
  fun `verify sea-truck watercraft schedule navigation`() {
    val vm = PortalViewModel()
    assertEquals(com.example.ui.viewmodel.ActiveModal.None, vm.activeModal.value)
    vm.openModal(com.example.ui.viewmodel.ActiveModal.SeaTruckModal)
    assertEquals(com.example.ui.viewmodel.ActiveModal.SeaTruckModal, vm.activeModal.value)
    vm.closeModal()
    assertEquals(com.example.ui.viewmodel.ActiveModal.None, vm.activeModal.value)
  }

  @Test
  fun `verify watercraft schedule route filter states`() {
    val filters = com.example.ui.screens.RouteFilterState.values()
    assertEquals(3, filters.size)
    assertEquals("সকল রুট", com.example.ui.screens.RouteFilterState.ALL.titleBn)
    assertEquals("কুতুবদিয়া - মগনামা", com.example.ui.screens.RouteFilterState.KUTUBDIA_MOGNAMA.titleBn)
    assertEquals("মহেশখালী - কক্সবাজার", com.example.ui.screens.RouteFilterState.MAHESHKHALI_COXSBAZAR.titleBn)
  }

  @Test
  fun `verify ViewModel defaults unauthenticated and clears admin state on logout`() {
    val vm = PortalViewModel()
    assertEquals(false, vm.isAdminAuthenticated.value)
    assertEquals(null, vm.adminRole.value)
    assertEquals(null, vm.currentUserId.value)

    var verified = true
    var verifiedRole: String? = "unexpected"
    vm.verifyCurrentUserAdmin { isAdmin, role ->
      verified = isAdmin
      verifiedRole = role
    }
    assertEquals(false, verified)
    assertEquals(null, verifiedRole)

    vm.logoutAdmin()
    assertEquals(false, vm.isAdminAuthenticated.value)
    assertEquals(null, vm.adminRole.value)
    assertEquals(null, vm.currentUserId.value)
  }

  @Test
  fun `verify in-app notifications unread count, mark read, and mark all read`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("portal_in_app_notifications", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val repo = PortalRepository(firestore = null, context = context)
    val vm = PortalViewModel(repo)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val notifications = vm.inAppNotifications.value
    assertTrue("Default published notices should populate notifications", notifications.isNotEmpty())
    val initialUnread = vm.unreadNotificationCount.value
    assertEquals(notifications.size, initialUnread)

    val firstId = notifications.first().id
    vm.markNotificationRead(firstId)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(initialUnread - 1, vm.unreadNotificationCount.value)

    vm.markAllNotificationsRead()
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(0, vm.unreadNotificationCount.value)
  }

  @Test
  fun `verify CMS default content flows populate cleanly in repository`() {
    val repo = PortalRepository()
    assertEquals(9, repo.digitalServices.value.size)
    assertEquals(17, repo.educationResources.value.size)
    assertEquals(7, repo.economyOpportunities.value.size)
    assertEquals(5, repo.welfarePrograms.value.size)
    assertEquals(14, repo.unionServices.value.size)
    assertEquals(11, repo.developmentCategories.value.size)
    assertEquals(10, repo.homepageCategories.value.size)
    assertEquals(4, repo.citizenPoll.value.options.size)
  }

  @Test
  fun `verify IntentUtils handles blank inputs gracefully without throwing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.IntentUtils.safeDial(context, "", AppLanguage.BN)
    com.example.util.IntentUtils.safeSendEmail(context, "", AppLanguage.EN)
    com.example.util.IntentUtils.safeOpenUrl(context, "", AppLanguage.BN)
    com.example.util.IntentUtils.safeShareText(context, "", "subject", "Share", AppLanguage.EN)
  }
}
