package com.example.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.InAppNotificationHelper
import com.example.util.IntentUtils
import com.example.util.MediaStorageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.credentials.CredentialManager
import com.example.util.AuthManager

// --- Tab 2: Gallery Screen ---
@Composable
fun GalleryTabScreen(
    items: List<GalleryItem>,
    mediaArchiveItems: List<MediaArchiveItem> = emptyList(),
    importantLinks: List<ImportantExternalLink> = emptyList(),
    language: AppLanguage,
    isAdminAuthenticated: Boolean = false,
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
    MediaGalleryScreen(
        galleryItems = items,
        mediaArchiveItems = mediaArchiveItems,
        importantLinks = importantLinks,
        language = language,
        isAdminAuthenticated = isAdminAuthenticated,
        onAdminLoginClick = onAdminLoginClick,
        onAdminLogoutClick = onAdminLogoutClick,
        onSaveGalleryItem = onSaveGalleryItem,
        onDeleteGalleryItem = onDeleteGalleryItem,
        onSaveMediaArchiveItem = onSaveMediaArchiveItem,
        onDeleteMediaArchiveItem = onDeleteMediaArchiveItem,
        onSaveImportantLink = onSaveImportantLink,
        onDeleteImportantLink = onDeleteImportantLink,
        appConfig = appConfig,
        onAboutClick = onAboutClick,
        onProfileClick = onProfileClick,
        onPolicyClick = onPolicyClick,
        onFeedbackClick = onFeedbackClick,
        onTermsClick = onTermsClick,
        modifier = modifier
    )
}

// --- Tab 4: Contact & Grievance Screen ---
@Composable
fun ContactTabScreen(
    language: AppLanguage,
    appConfig: AppConfig = AppConfig(),
    faqs: List<FaqItem> = emptyList(),
    appointmentRequests: List<AppointmentRequest> = emptyList(),
    unionRepresentatives: List<UnionRepresentative> = emptyList(),
    isAdminAuthenticated: Boolean = false,
    adminRole: String? = null,
    currentUserId: String? = null,
    onSaveConfig: (AppConfig) -> Unit = {},
    onSubmitAppointment: (name: String, phone: String, upazila: String, union: String, category: String, message: String, attachmentUrl: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onCheckStatus: (String, (AppointmentRequest?) -> Unit) -> Unit,
    onUpdateAppointment: (id: String, status: String, adminNote: String, scheduledDate: String) -> Unit = { _, _, _, _ -> },
    onDeleteAppointment: (String) -> Unit = {},
    onSaveFaq: (FaqItem) -> Unit = {},
    onDeleteFaq: (String) -> Unit = {},
    onSaveUnionRepresentative: (UnionRepresentative) -> Unit = {},
    onDeleteUnionRepresentative: (String) -> Unit = {},
    socialMediaLinks: List<SocialMediaLink> = emptyList(),
    onSaveSocialMediaLink: (SocialMediaLink) -> Unit = {},
    onDeleteSocialMediaLink: (String) -> Unit = {},
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedUpazila by remember { mutableStateOf("মহেশখালী") }
    var selectedUnion by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("সাধারণ জিজ্ঞাসা") }
    var message by remember { mutableStateOf("") }
    var attachmentUrl by remember { mutableStateOf("") }
    
    var trackingIdInput by remember { mutableStateOf("") }
    var trackedRequest by remember { mutableStateOf<AppointmentRequest?>(null) }
    var isCheckingStatus by remember { mutableStateOf(false) }
    var hasSearchedStatus by remember { mutableStateOf(false) }
    var isSubmittingAppointment by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    
    var lastSubmittedTrackingId by remember { mutableStateOf<String?>(null) }
    var showSuccessModal by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showOfficeModal by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }
    var editingFaq by remember { mutableStateOf<FaqItem?>(null) }
    var showAddFaqDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<AppointmentRequest?>(null) }
    var editingSocialLink by remember { mutableStateOf<SocialMediaLink?>(null) }
    var isAddingSocialLink by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val canEditCms = isAdminAuthenticated && (adminRole == "super_admin" || adminRole == "editor")
    val canManageApps = isAdminAuthenticated && (adminRole == "super_admin" || adminRole == "application_manager")
    val isSuperAdmin = isAdminAuthenticated && adminRole == "super_admin"

    val triggerGoogleSignIn: () -> Unit = {
        if (!isSigningIn) {
            isSigningIn = true
            AuthManager.signInWithGoogle(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = {
                    isSigningIn = false
                    android.widget.Toast.makeText(
                        context,
                        if (language == AppLanguage.BN) "Google সাইন-ইন সফল হয়েছে" else "Signed in with Google",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onAuthError = { err ->
                    isSigningIn = false
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                },
                onAuthCancelled = {
                    isSigningIn = false
                }
            )
        }
    }

    val moheshkhaliUnions = listOf("মাতারবাড়ি", "ধলঘাটা", "কালারমারছড়া", "শাপলাপুর", "হোয়ানক", "বড় মহেশখালী", "ছোট মহেশখালী", "কুতুবজোম")
    val kutubdiaUnions = listOf("বড়ঘোপ", "আলী আকবর ডেইল", "কৈয়ারবিল", "লেমশীখালী", "উত্তর ধুরুং", "দক্ষিণ ধুরুং")
    
    val categories = if (language == AppLanguage.BN) {
        listOf("সাধারণ জিজ্ঞাসা", "উন্নয়ন প্রস্তাব", "জনসেবা আবেদন", "সাক্ষাতের অনুরোধ", "অভিযোগ")
    } else {
        listOf("General Inquiry", "Development Proposal", "Public Service Request", "Appointment Request", "Grievance")
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        // 1. Contact Hero
        item {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                AsyncOrLocalImage("drawable:moheshkhali_banner", null, Modifier.fillMaxSize(), ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color(0xFF006A4E).copy(0.85f)))))
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(
                        text = if (language == AppLanguage.BN) "যোগাযোগ ও গণসংযোগ" else "Contact & Public Relations",
                        color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (language == AppLanguage.BN) "আপনার মতামত ও আবেদন সরাসরি এমপি মহোদয়ের দপ্তরে জানান" else "Direct communication with the MP's public service cell",
                        color = Color.White.copy(0.9f), fontSize = 13.sp
                    )
                }

                if (canEditCms) {
                    IconButton(
                        onClick = { showEditContactDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.White, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Contact Info", tint = ForestGreen)
                    }
                }
            }
        }

        // Admin Notification Banner
        if (canEditCms || canManageApps) {
            item {
                Surface(
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF1D4ED8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.BN) "অ্যাডমিন নিয়ন্ত্রণ সক্রিয় (${adminRole ?: "admin"})" else "Admin mode active (${adminRole ?: "admin"})",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF)
                        )
                    }
                }
            }
        }

        // 2. Information Cards
        item {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactInfoCard(
                    icon = Icons.Default.Call,
                    title = if (language == AppLanguage.BN) "ফোন করুন" else "Call Us",
                    value = appConfig.hotlinePhone,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f),
                    onClick = { IntentUtils.safeDial(context, appConfig.hotlinePhone, language) }
                )
                ContactInfoCard(
                    icon = Icons.Default.Email,
                    title = if (language == AppLanguage.BN) "ইমেইল" else "Email",
                    value = appConfig.contactEmail,
                    color = Color(0xFFE11D48),
                    modifier = Modifier.weight(1f),
                    onClick = { IntentUtils.safeSendEmail(context, appConfig.contactEmail, language) }
                )
            }
        }
        item {
            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ContactInfoCard(
                    icon = Icons.Default.LocationOn,
                    title = if (language == AppLanguage.BN) "অফিস" else "Office",
                    value = if (language == AppLanguage.BN) appConfig.officeAddressCoxsBazarBn else appConfig.officeAddressCoxsBazarEn,
                    color = Color(0xFF059669),
                    modifier = Modifier.weight(1f),
                    onClick = { showOfficeModal = true }
                )
                ContactInfoCard(
                    icon = Icons.Default.Schedule,
                    title = if (language == AppLanguage.BN) "সময় ও সাক্ষাত" else "Schedule & Time",
                    value = when (appConfig.availabilityType) {
                        "OFFICIAL_PROGRAM" -> if (language == AppLanguage.BN) "কর্মসূচিতে গমন" else "Official Program"
                        "PARLIAMENT_SESSION" -> if (language == AppLanguage.BN) "সংসদ অধিবেশন" else "Parliament Session"
                        else -> if (language == AppLanguage.BN) "অফিসে উপস্থিতি" else "Office Present"
                    },
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = { showScheduleModal = true }
                )
            }
        }

        // 2.5 Union Parishad Chairman & Secretary Directory Cards
        item {
            UnionRepresentativesSection(
                representatives = unionRepresentatives,
                isAdminAuthenticated = canEditCms,
                language = language,
                onSaveRepresentative = onSaveUnionRepresentative,
                onDeleteRepresentative = onDeleteUnionRepresentative
            )
        }

        // 3. Appointment & Message Form
        item {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (language == AppLanguage.BN) "আবেদন ও সাক্ষাতের ফর্ম" else "Petition & Appointment Form",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
                    )

                    if (currentUserId.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN)
                                        "আবেদন জমা দিতে ও ব্যক্তিগত তথ্য সুরক্ষিত রাখতে আপনার Google অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
                                    else
                                        "Sign in with Google to submit a petition or appointment request securely.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E3A8A)
                                )
                                OutlinedButton(
                                    onClick = triggerGoogleSignIn,
                                    enabled = !isSigningIn,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("contact_petition_google_signin_button")
                                ) {
                                    if (isSigningIn) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoyalBlue800)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (language == AppLanguage.BN) "Google দিয়ে সাইন-ইন করুন" else "Sign in with Google",
                                            fontWeight = FontWeight.SemiBold,
                                            color = RoyalBlue800
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text(if (language == AppLanguage.BN) "পূর্ণ নাম" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text(if (language == AppLanguage.BN) "মোবাইল নম্বর" else "Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UpazilaSelector(
                            label = if (language == AppLanguage.BN) "উপজেলা" else "Upazila",
                            options = listOf("মহেশখালী", "কুতুবদিয়া"),
                            selected = selectedUpazila,
                            onSelect = { 
                                selectedUpazila = it
                                selectedUnion = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        UpazilaSelector(
                            label = if (language == AppLanguage.BN) "ইউনিয়ন" else "Union",
                            options = if (selectedUpazila == "মহেশখালী") moheshkhaliUnions else kutubdiaUnions,
                            selected = selectedUnion,
                            onSelect = { selectedUnion = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    UpazilaSelector(
                        label = if (language == AppLanguage.BN) "আবেদনের ধরন" else "Category",
                        options = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = message, onValueChange = { message = it },
                        label = { Text(if (language == AppLanguage.BN) "বিস্তারিত বার্তা / দাবি" else "Message Details") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )

                    Button(
                        onClick = {
                            if (currentUserId.isNullOrBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "আবেদন জমা দিতে প্রথমে Google দিয়ে সাইন-ইন করুন" else "Please sign in with Google before submitting",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                triggerGoogleSignIn()
                                return@Button
                            }
                            val trimmedName = name.trim()
                            val trimmedPhone = phone.trim()
                            val trimmedMessage = message.trim()
                            if (trimmedName.isBlank() || trimmedPhone.length < 6 || trimmedMessage.isBlank()) {
                                android.widget.Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BN) "দয়া করে পূর্ণ নাম, সঠিক মোবাইল নম্বর (কমপক্ষে ৬ অঙ্ক) এবং বার্তা দিন" else "Please enter your name, valid phone (at least 6 chars), and message",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            isSubmittingAppointment = true
                            onSubmitAppointment(
                                trimmedName,
                                trimmedPhone,
                                selectedUpazila,
                                selectedUnion,
                                selectedCategory,
                                trimmedMessage,
                                attachmentUrl,
                                { tid ->
                                    isSubmittingAppointment = false
                                    lastSubmittedTrackingId = tid
                                    showSuccessModal = true
                                    submitted = true
                                    name = ""
                                    phone = ""
                                    message = ""
                                },
                                { err ->
                                    isSubmittingAppointment = false
                                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = !isSubmittingAppointment,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_appointment_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800)
                    ) {
                        if (isSubmittingAppointment) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(if (language == AppLanguage.BN) "আবেদন জমা দিন" else "Submit Application", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    if (submitted && lastSubmittedTrackingId != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "আবেদন সফলভাবে জমা হয়েছে!" else "Application submitted successfully!",
                                    color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                Text(
                                    text = "${if (language == AppLanguage.BN) "আপনার ট্র্যাকিং নম্বর:" else "Your Tracking Number:"} $lastSubmittedTrackingId",
                                    color = RoyalBlue900, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "এটি সংরক্ষণ করুন, স্ট্যাটাস চেক করতে প্রয়োজন হবে।" else "Save this ID to check your application status.",
                                    color = Color.DarkGray, fontSize = 11.sp
                                )
                            }
                        }
                    }

                }
            }
        }

        // 4. Application Status Check Section
        item {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ManageSearch, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (language == AppLanguage.BN) "আবেদনের অবস্থা পর্যবেক্ষণ করুন" else "Check Application Status",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
                        )
                    }
                    if (currentUserId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN)
                                        "নিরাপত্তার জন্য শুধুমাত্র সাইন-ইন করা আবেদনকারী নিজের আবেদনের অবস্থা দেখতে পারবেন।"
                                    else
                                        "For privacy, sign in with Google to check the status of your own applications.",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF334155)
                                )
                                OutlinedButton(
                                    onClick = triggerGoogleSignIn,
                                    enabled = !isSigningIn,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("contact_track_google_signin_button")
                                ) {
                                    Text(
                                        if (language == AppLanguage.BN) "Google দিয়ে সাইন-ইন করুন" else "Sign in with Google",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = trackingIdInput,
                            onValueChange = { 
                                trackingIdInput = it 
                                hasSearchedStatus = false
                            },
                            placeholder = { Text(if (language == AppLanguage.BN) "ট্র্যাকিং নম্বর দিন (যেমন: APP-8F3K9A)" else "Enter Tracking Number (e.g. APP-8F3K9A)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (currentUserId.isNullOrBlank()) {
                                    android.widget.Toast.makeText(
                                        context,
                                        if (language == AppLanguage.BN) "স্ট্যাটাস দেখতে প্রথমে Google দিয়ে সাইন-ইন করুন" else "Please sign in with Google first to track your application",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                    triggerGoogleSignIn()
                                    return@Button
                                }
                                if (trackingIdInput.isNotBlank()) {
                                    isCheckingStatus = true
                                    hasSearchedStatus = true
                                    onCheckStatus(trackingIdInput.trim()) { res ->
                                        trackedRequest = res
                                        isCheckingStatus = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.height(56.dp).testTag("track_status_search_button")
                        ) {
                            if (isCheckingStatus) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Search, null)
                            }
                        }
                    }
                    
                    if (hasSearchedStatus && !isCheckingStatus) {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (trackedRequest != null) {
                            TrackingStatusResult(trackedRequest!!, language)
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) 
                                            "'$trackingIdInput' ট্র্যাকিং নম্বরে কোনো আবেদন খুঁজে পাওয়া যায়নি।" 
                                            else "No application found for '$trackingIdInput'.",
                                        color = Color(0xFF991B1B), fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4.1 Signed-In Citizen's Own Submitted Applications History (User-Scoped Isolation)
        if (!currentUserId.isNullOrBlank()) {
            val myApplications = appointmentRequests.filter { it.userId == currentUserId }
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "আমার জমাকৃত আবেদন (${myApplications.size})" else "My Submitted Applications (${myApplications.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (myApplications.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.BN)
                                    "আপনার অ্যাকাউন্ট থেকে এখনো কোনো আবেদন বা সাক্ষাতের অনুরোধ জমা দেওয়া হয়নি।"
                                else
                                    "You have not submitted any applications or appointment requests yet.",
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                myApplications.take(5).forEach { req ->
                                    val code = req.trackingNumber.ifBlank { req.trackingId }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                trackingIdInput = code
                                                trackedRequest = req
                                                hasSearchedStatus = true
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(code, fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 12.5.sp)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = RoyalBlue800.copy(alpha = 0.1f)
                                                ) {
                                                    Text(
                                                        text = if (language == AppLanguage.BN)
                                                            InAppNotificationHelper.statusLabelBn(req.status)
                                                        else
                                                            InAppNotificationHelper.statusLabelEn(req.status),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RoyalBlue800,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = req.category.ifBlank { req.type },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            if (req.scheduledDate.isNotBlank()) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) "নির্ধারিত সময়: ${req.scheduledDate}" else "Scheduled: ${req.scheduledDate}",
                                                    fontSize = 11.sp,
                                                    color = ForestGreen
                                                )
                                            }
                                            if (req.adminNote.isNotBlank()) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) "দপ্তরের মন্তব্য: ${req.adminNote}" else "Office Note: ${req.adminNote}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Admin Petition & Appointments Management List (When Application Manager or Super Admin Logged In)
        if (canManageApps) {
            item {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) "আবেদন ও সাক্ষাত ব্যবস্থাপনা (অ্যাডমিন)" else "Petitions & Appointments (Admin)",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E)
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "মোট ${appointmentRequests.size} টি আবেদন প্রাপ্ত" else "Total ${appointmentRequests.size} requests received",
                                    fontSize = 12.sp, color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (appointmentRequests.isEmpty()) {
                            Text(
                                text = if (language == AppLanguage.BN) "কোনো আবেদন এখনো জমা হয়নি।" else "No appointment requests submitted yet.",
                                fontSize = 13.sp, color = Color.DarkGray
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                appointmentRequests.take(10).forEach { req ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(req.trackingId, fontWeight = FontWeight.Bold, color = ForestGreen, fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = when(req.status) {
                                                            "PENDING" -> Color.Gray
                                                            "REVIEWING" -> Color(0xFF0284C7)
                                                            "SCHEDULED" -> ForestGreen
                                                            "COMPLETED" -> ForestGreen
                                                            "CANCELLED" -> Color.Red
                                                            else -> Color.Gray
                                                        }.copy(alpha = 0.15f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            req.status,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(req.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${req.category} • ${req.phone} (${req.upazila})", fontSize = 11.sp, color = Color.Gray)
                                                if (req.message.isNotBlank()) {
                                                    Text(req.message, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.DarkGray)
                                                }
                                            }

                                            Row {
                                                IconButton(onClick = { editingAppointment = req }) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0284C7))
                                                }
                                                if (isSuperAdmin) {
                                                    IconButton(onClick = { onDeleteAppointment(req.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. FAQ Accordion & Admin FAQ Management
        item {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (language == AppLanguage.BN) "সাধারণ জিজ্ঞাসা (FAQ)" else "Frequently Asked Questions",
                                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
                            )
                            Text(
                                if (language == AppLanguage.BN) "জরুরি প্রশ্নের দ্রুত উত্তরসমূহ" else "Quick answers to common questions",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }

                        if (canEditCms) {
                            Button(
                                onClick = { 
                                    editingFaq = FaqItem(order = faqs.size + 1)
                                    showAddFaqDialog = true 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == AppLanguage.BN) "যোগ করুন" else "Add FAQ", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (faqs.isEmpty()) {
                        Text(
                            text = if (language == AppLanguage.BN) "কোনো প্রশ্ন সংরক্ষিত নেই।" else "No FAQ items available.",
                            fontSize = 13.sp, color = Color.Gray
                        )
                    } else {
                        faqs.forEach { faq ->
                            var expanded by remember { mutableStateOf(false) }
                            val question = if (language == AppLanguage.BN) faq.questionBn else faq.questionEn
                            val answer = if (language == AppLanguage.BN) faq.answerBn else faq.answerEn

                            Surface(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(question, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        
                                        if (canEditCms) {
                                            IconButton(
                                                onClick = {
                                                    editingFaq = faq
                                                    showAddFaqDialog = true
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit FAQ", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteFaq(faq.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete FAQ", tint = Color.Red, modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                    }
                                    if (expanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(answer, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Social Media Links
        item {
            SocialLinksSection(
                links = socialMediaLinks,
                isAdminAuthenticated = canEditCms,
                language = language,
                onEdit = { editingSocialLink = it },
                onDelete = onDeleteSocialMediaLink,
                onAddNew = { isAddingSocialLink = true }
            )
        }
    }

    if (showOfficeModal) {
        OfficeLocationsModalDialog(
            appConfig = appConfig,
            language = language,
            onDismiss = { showOfficeModal = false }
        )
    }

    if (showScheduleModal) {
        MeetingScheduleModalDialog(
            appConfig = appConfig,
            language = language,
            onDismiss = { showScheduleModal = false }
        )
    }

    if (showEditContactDialog) {
        AdminContactEditDialog(
            appConfig = appConfig,
            language = language,
            onSave = {
                onSaveConfig(it)
                showEditContactDialog = false
            },
            onDismiss = { showEditContactDialog = false }
        )
    }

    if (showAddFaqDialog && editingFaq != null) {
        AdminFaqEditDialog(
            faq = editingFaq!!,
            language = language,
            onSave = { item ->
                onSaveFaq(item)
                showAddFaqDialog = false
                editingFaq = null
            },
            onDismiss = {
                showAddFaqDialog = false
                editingFaq = null
            }
        )
    }

    if (editingAppointment != null) {
        AdminAppointmentEditDialog(
            request = editingAppointment!!,
            language = language,
            canDelete = isSuperAdmin,
            onSave = { status, note, date ->
                onUpdateAppointment(editingAppointment!!.id, status, note, date)
                editingAppointment = null
            },
            onDelete = {
                onDeleteAppointment(editingAppointment!!.id)
                editingAppointment = null
            },
            onDismiss = { editingAppointment = null }
        )
    }

    if (isAddingSocialLink || editingSocialLink != null) {
        SocialLinkEditDialog(
            link = editingSocialLink,
            isNew = isAddingSocialLink,
            language = language,
            onSave = { updated ->
                onSaveSocialMediaLink(updated)
                isAddingSocialLink = false
                editingSocialLink = null
            },
            onDismiss = {
                isAddingSocialLink = false
                editingSocialLink = null
            }
        )
    }
}



@Composable
private fun ContactInfoCard(icon: ImageVector, title: String, value: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpazilaSelector(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TrackingStatusResult(request: AppointmentRequest, language: AppLanguage) {
    val displayStatus = request.status.ifBlank { "Pending" }
    
    val (statusColor, statusBg, statusText) = when (displayStatus.uppercase()) {
        "PENDING", "SUBMITTED", "পেন্ডিং" -> Triple(
            Color(0xFFD97706),
            Color(0xFFFEF3C7),
            if (language == AppLanguage.BN) "গৃহীত / পেন্ডিং (পর্যালোচনার অপেক্ষায়)" else "Submitted / Pending"
        )
        "REVIEWING", "UNDER REVIEW", "ADDITIONAL INFORMATION REQUIRED", "FORWARDED TO RELEVANT OFFICE", "IN PROGRESS", "গৃহীত", "ACCEPTED" -> Triple(
            Color(0xFF0284C7),
            Color(0xFFE0F2FE),
            if (language == AppLanguage.BN) "পর্যালোচনাধীন / চলমান" else "Under Review / In Progress"
        )
        "SCHEDULED", "সময় নির্ধারিত" -> Triple(
            ForestGreen,
            Color(0xFFDCFCE7),
            if (language == AppLanguage.BN) "সময় নির্ধারিত" else "Scheduled"
        )
        "COMPLETED", "RESOLVED", "সম্পন্ন" -> Triple(
            ForestGreen,
            Color(0xFFDCFCE7),
            if (language == AppLanguage.BN) "সম্পন্ন / সমাধানকৃত" else "Completed / Resolved"
        )
        "CANCELLED", "REJECTED WITH EXPLANATION", "বাতিল" -> Triple(
            Color.Red,
            Color(0xFFFEE2E2),
            if (language == AppLanguage.BN) "বাতিল / প্রত্যাখ্যাত" else "Cancelled / Rejected"
        )
        else -> Triple(
            Color(0xFF0284C7),
            Color(0xFFE0F2FE),
            displayStatus
        )
    }

    val displayTracking = request.trackingNumber.ifBlank { request.trackingId }
    val displayName = request.name
    val displayPhone = request.mobile.ifBlank { request.phone }
    val displayType = request.type.ifBlank { request.category }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (language == AppLanguage.BN) "ট্র্যাকিং নম্বর:" else "Tracking #:"} $displayTracking",
                    fontWeight = FontWeight.Bold,
                    color = RoyalBlue900,
                    fontSize = 15.sp
                )
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = Color(0xFFF1F5F9))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${if (language == AppLanguage.BN) "আবেদনকারী:" else "Applicant:"} $displayName", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (displayPhone.isNotBlank()) {
                    Text("${if (language == AppLanguage.BN) "মোবাইল:" else "Mobile:"} $displayPhone", fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${if (language == AppLanguage.BN) "উপজেলা/ইউনিয়ন:" else "Location:"} ${request.upazila}${if (request.union.isNotBlank()) ", ${request.union}" else ""}", fontSize = 13.sp, color = Color.DarkGray)
                Text("${if (language == AppLanguage.BN) "ধরনের:" else "Type:"} $displayType", fontSize = 13.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
            }

            if (request.message.isNotBlank()) {
                Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "💬 ${request.message}",
                        fontSize = 12.sp, modifier = Modifier.padding(8.dp), color = Color(0xFF334155)
                    )
                }
            }

            if (request.scheduledDate.isNotBlank()) {
                Text("${if (language == AppLanguage.BN) "সাক্ষাতের সময়:" else "Schedule Date:"} ${request.scheduledDate}", fontWeight = FontWeight.Bold, color = RoyalBlue900, fontSize = 13.sp)
            }

            if (request.adminNote.isNotBlank()) {
                Surface(color = Color(0xFFFFF7ED), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "📝 ${if (language == AppLanguage.BN) "দপ্তর থেকে আপডেট/নোট:" else "Admin Note:"} ${request.adminNote}",
                        fontSize = 12.sp, modifier = Modifier.padding(10.dp), color = Color(0xFF9A3412)
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialLinksSection(
    links: List<SocialMediaLink>,
    isAdminAuthenticated: Boolean,
    language: AppLanguage,
    onEdit: (SocialMediaLink) -> Unit,
    onDelete: (String) -> Unit,
    onAddNew: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN) "সামাজিক যোগাযোগ মাধ্যম" else "Social Media Channels",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
                )
                Text(
                    text = if (language == AppLanguage.BN) "এমপি মহোদয়ের অফিসিয়াল পেজসমূহ" else "Official pages of the Hon. MP",
                    fontSize = 12.sp, color = Color.Gray
                )
            }

            if (isAdminAuthenticated) {
                Button(
                    onClick = onAddNew,
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.BN) "যোগ করুন" else "Add New", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val displayLinks = if (links.isEmpty()) {
            listOf(
                SocialMediaLink(
                    id = "DEFAULT_FB",
                    platformNameBn = "অফিসিয়াল ফেসবুক পেজ",
                    platformNameEn = "Official Facebook Page",
                    url = "https://facebook.com/asheq.ullah.rafiq.mp",
                    iconType = "FACEBOOK",
                    order = 1
                ),
                SocialMediaLink(
                    id = "DEFAULT_YT",
                    platformNameBn = "অফিসিয়াল ইউটিউব চ্যানেল",
                    platformNameEn = "Official YouTube Channel",
                    url = "https://youtube.com/@ashequllahrafiq",
                    iconType = "YOUTUBE",
                    order = 2
                ),
                SocialMediaLink(
                    id = "DEFAULT_WA",
                    platformNameBn = "হোয়াটসঅ্যাপ হেল্পলাইন",
                    platformNameEn = "WhatsApp Helpline",
                    url = "https://wa.me/8801819000000",
                    iconType = "WHATSAPP",
                    order = 3
                ),
                SocialMediaLink(
                    id = "DEFAULT_TW",
                    platformNameBn = "এক্স (টুইটার) প্রোফাইল",
                    platformNameEn = "X (Twitter) Profile",
                    url = "https://twitter.com/AsheqRafiqMP",
                    iconType = "TWITTER",
                    order = 4
                ),
                SocialMediaLink(
                    id = "DEFAULT_INSTA",
                    platformNameBn = "ইনস্টাগ্রাম অ্যাকাউন্ট",
                    platformNameEn = "Instagram Account",
                    url = "https://instagram.com/asheq_rafiq_mp",
                    iconType = "INSTAGRAM",
                    order = 5
                )
            )
        } else {
            links
        }

        val chunked = displayLinks.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunked.forEach { rowLinks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowLinks.forEach { link ->
                        SocialMediaBox(
                            link = link,
                            language = language,
                            isAdmin = isAdminAuthenticated,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onClick = {
                                IntentUtils.safeOpenUrl(context, link.url, language)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowLinks.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}



@Composable
fun AdminFaqEditDialog(
    faq: FaqItem,
    language: AppLanguage,
    onSave: (FaqItem) -> Unit,
    onDismiss: () -> Unit
) {
    var questionBn by remember { mutableStateOf(faq.questionBn) }
    var questionEn by remember { mutableStateOf(faq.questionEn) }
    var answerBn by remember { mutableStateOf(faq.answerBn) }
    var answerEn by remember { mutableStateOf(faq.answerEn) }
    var orderText by remember { mutableStateOf(faq.order.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (faq.id.isBlank()) (if (language == AppLanguage.BN) "নতুন FAQ যোগ করুন" else "Add New FAQ") else (if (language == AppLanguage.BN) "FAQ সম্পাদনা করুন" else "Edit FAQ"),
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
                )

                OutlinedTextField(
                    value = questionBn, onValueChange = { questionBn = it },
                    label = { Text(if (language == AppLanguage.BN) "প্রশ্ন (বাংলা)" else "Question (Bengali)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = questionEn, onValueChange = { questionEn = it },
                    label = { Text(if (language == AppLanguage.BN) "প্রশ্ন (English)" else "Question (English)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = answerBn, onValueChange = { answerBn = it },
                    label = { Text(if (language == AppLanguage.BN) "উত্তর (বাংলা)" else "Answer (Bengali)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                OutlinedTextField(
                    value = answerEn, onValueChange = { answerEn = it },
                    label = { Text(if (language == AppLanguage.BN) "উত্তর (English)" else "Answer (English)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (questionBn.isNotBlank() && answerBn.isNotBlank()) {
                                onSave(
                                    faq.copy(
                                        questionBn = questionBn,
                                        questionEn = if (questionEn.isBlank()) questionBn else questionEn,
                                        answerBn = answerBn,
                                        answerEn = if (answerEn.isBlank()) answerBn else answerEn,
                                        order = orderText.toIntOrNull() ?: faq.order
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppointmentEditDialog(
    request: AppointmentRequest,
    language: AppLanguage,
    canDelete: Boolean = true,
    onSave: (status: String, note: String, date: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var status by remember { mutableStateOf(request.status) }
    var note by remember { mutableStateOf(request.adminNote) }
    var date by remember { mutableStateOf(request.scheduledDate) }

    val statuses = listOf("Pending", "Under Review", "REVIEWING", "SCHEDULED", "In Progress", "COMPLETED", "Resolved", "CANCELLED")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (language == AppLanguage.BN) "আবেদন পর্যালোচনা" else "Review Application", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
                
                Text("${if (language == AppLanguage.BN) "আবেদনকারী:" else "Applicant:"} ${request.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${if (language == AppLanguage.BN) "মোবাইল:" else "Phone:"} ${request.phone} (${request.upazila} - ${request.union})", fontSize = 12.sp, color = Color.Gray)
                Text("${if (language == AppLanguage.BN) "বার্তা:" else "Message:"} ${request.message}", fontSize = 13.sp, color = Color.DarkGray)

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

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(status, note, date) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                    ) {
                        Text(if (language == AppLanguage.BN) "আপডেট করুন" else "Update Status")
                    }
                }
            }
        }
    }
}







@Composable
fun AdminContactEditDialog(
    appConfig: AppConfig,
    language: AppLanguage,
    onSave: (AppConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var coxsBazarBn by remember { mutableStateOf(appConfig.officeAddressCoxsBazarBn) }
    var coxsBazarEn by remember { mutableStateOf(appConfig.officeAddressCoxsBazarEn) }
    var moheshkhaliBn by remember { mutableStateOf(appConfig.officeAddressMoheshkhaliBn) }
    var moheshkhaliEn by remember { mutableStateOf(appConfig.officeAddressMoheshkhaliEn) }
    var kutubdiaBn by remember { mutableStateOf(appConfig.officeAddressKutubdiaBn) }
    var kutubdiaEn by remember { mutableStateOf(appConfig.officeAddressKutubdiaEn) }
    var hotline by remember { mutableStateOf(appConfig.hotlinePhone) }
    var email by remember { mutableStateOf(appConfig.contactEmail) }
    var availabilityType by remember { mutableStateOf(appConfig.availabilityType) }
    var scheduleStatusBn by remember { mutableStateOf(appConfig.meetingScheduleStatusBn) }
    var scheduleStatusEn by remember { mutableStateOf(appConfig.meetingScheduleStatusEn) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.88f).testTag("admin_contact_edit_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue900, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "অফিস ঠিকানা ও সময়সূচি সম্পাদনা" else "Edit Office & Schedule Info",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("কক্সবাজার প্রধান কার্যালয়:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue900)
                    }
                    item {
                        OutlinedTextField(
                            value = coxsBazarBn,
                            onValueChange = { coxsBazarBn = it },
                            label = { Text("কক্সবাজার কার্যালয় (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = coxsBazarEn,
                            onValueChange = { coxsBazarEn = it },
                            label = { Text("Cox's Bazar Office (English)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("মহেশখালী ও কুতুবদিয়া দ্বীপ কার্যালয়:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue900)
                    }
                    item {
                        OutlinedTextField(
                            value = moheshkhaliBn,
                            onValueChange = { moheshkhaliBn = it },
                            label = { Text("মহেশখালী কার্যালয় (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = kutubdiaBn,
                            onValueChange = { kutubdiaBn = it },
                            label = { Text("কুতুবদিয়া কার্যালয় (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("যোগাযোগ হটলাইন ও ইমেইল:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue900)
                    }
                    item {
                        OutlinedTextField(
                            value = hotline,
                            onValueChange = { hotline = it },
                            label = { Text("হটলাইন নম্বর / Hotline Phone") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("ইমেইল / Contact Email") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("উপস্থিতি ও সময়সূচি নির্বাচন:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RoyalBlue900)
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "OFFICE_PRESENT" to "অফিসে উপস্থিত",
                                "OFFICIAL_PROGRAM" to "কর্মসূচিতে গমন",
                                "PARLIAMENT_SESSION" to "সংসদ অধিবেশন"
                            ).forEach { (type, label) ->
                                FilterChip(
                                    selected = availabilityType == type,
                                    onClick = { availabilityType = type },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = scheduleStatusBn,
                            onValueChange = { scheduleStatusBn = it },
                            label = { Text("উপস্থিতি বিস্তারিত নোট (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                        }
                        Button(
                            onClick = {
                                val updated = appConfig.copy(
                                    officeAddressCoxsBazarBn = coxsBazarBn,
                                    officeAddressCoxsBazarEn = coxsBazarEn,
                                    officeAddressMoheshkhaliBn = moheshkhaliBn,
                                    officeAddressMoheshkhaliEn = moheshkhaliEn,
                                    officeAddressKutubdiaBn = kutubdiaBn,
                                    officeAddressKutubdiaEn = kutubdiaEn,
                                    hotlinePhone = hotline,
                                    contactEmail = email,
                                    availabilityType = availabilityType,
                                    meetingScheduleStatusBn = scheduleStatusBn,
                                    meetingScheduleStatusEn = scheduleStatusEn
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900),
                            modifier = Modifier.weight(1f).testTag("save_contact_config_button")
                        ) {
                            Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfficeLocationsModalDialog(
    appConfig: AppConfig,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f).testTag("office_locations_modal_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = ForestGreen, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "কার্যালয়ের ঠিকানা ও নেভিগেশন" else "Office Locations & Navigation",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OfficeAddressCardItem(
                            title = if (language == AppLanguage.BN) "কক্সবাজার প্রধান কার্যালয়" else "Cox's Bazar Main Office",
                            address = if (language == AppLanguage.BN) appConfig.officeAddressCoxsBazarBn else appConfig.officeAddressCoxsBazarEn,
                            tag = if (language == AppLanguage.BN) "প্রধান কার্যালয়" else "Main Office",
                            color = ForestGreen,
                            language = language,
                            onNavigate = {
                                IntentUtils.safeOpenUrl(context, "https://maps.google.com/?q=${Uri.encode(appConfig.officeAddressCoxsBazarBn)}", language)
                            }
                        )
                    }

                    item {
                        OfficeAddressCardItem(
                            title = if (language == AppLanguage.BN) "মহেশখালী দ্বীপ কার্যালয়" else "Moheshkhali Office",
                            address = if (language == AppLanguage.BN) appConfig.officeAddressMoheshkhaliBn else appConfig.officeAddressMoheshkhaliEn,
                            tag = if (language == AppLanguage.BN) "উপজেলা কার্যালয়" else "Upazila Office",
                            color = RoyalBlue800,
                            language = language,
                            onNavigate = {
                                IntentUtils.safeOpenUrl(context, "https://maps.google.com/?q=${Uri.encode(appConfig.officeAddressMoheshkhaliBn)}", language)
                            }
                        )
                    }

                    item {
                        OfficeAddressCardItem(
                            title = if (language == AppLanguage.BN) "কুতুবদিয়া দ্বীপ কার্যালয়" else "Kutubdia Office",
                            address = if (language == AppLanguage.BN) appConfig.officeAddressKutubdiaBn else appConfig.officeAddressKutubdiaEn,
                            tag = if (language == AppLanguage.BN) "দ্বীপ কার্যালয়" else "Island Office",
                            color = Color(0xFFD97706),
                            language = language,
                            onNavigate = {
                                IntentUtils.safeOpenUrl(context, "https://maps.google.com/?q=${Uri.encode(appConfig.officeAddressKutubdiaBn)}", language)
                            }
                        )
                    }
                }

                Surface(color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (language == AppLanguage.BN) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OfficeAddressCardItem(
    title: String,
    address: String,
    tag: String,
    color: Color,
    language: AppLanguage,
    onNavigate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RoyalBlue900)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tag,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Place, contentDescription = null, tint = color, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = address, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigate,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                border = BorderStroke(1.dp, color),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == AppLanguage.BN) "গুগল ম্যাপে দিকনির্দেশনা" else "Get Directions on Google Maps",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MeetingScheduleModalDialog(
    appConfig: AppConfig,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f).testTag("meeting_schedule_modal_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = Color(0xFFD97706), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == AppLanguage.BN) "দৈনন্দিন উপস্থিতি ও সাক্ষাত সূচি" else "Daily Availability & Schedule",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val (statusTitle, statusColor, iconVector) = when (appConfig.availabilityType) {
                            "OFFICIAL_PROGRAM" -> Triple(
                                if (language == AppLanguage.BN) "আজ সরকারি/উন্নয়ন কর্মসূচিতে গমন করেছেন" else "Attending Official Program Today",
                                Color(0xFFD97706),
                                Icons.Default.EventBusy
                            )
                            "PARLIAMENT_SESSION" -> Triple(
                                if (language == AppLanguage.BN) "জাতীয় সংসদ অধিবেশন—ঢাকায় অবস্থান করছেন" else "Parliament Session—In Dhaka",
                                RoyalBlue800,
                                Icons.Default.AccountBalance
                            )
                            else -> Triple(
                                if (language == AppLanguage.BN) "আজ কক্সবাজার কার্যালয়ে উপস্থিত আছেন" else "Present Today at Cox's Bazar Office",
                                ForestGreen,
                                Icons.Default.CheckCircle
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = statusColor, modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(iconVector, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = statusTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = statusColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN) appConfig.meetingScheduleStatusBn else appConfig.meetingScheduleStatusEn,
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (language == AppLanguage.BN) "সাক্ষাতের নিয়ম ও অনুরোধ প্রক্রিয়া" else "Meeting Guidelines & Request Process",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = RoyalBlue900
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (language == AppLanguage.BN)
                                        "• মহেশখালী ও কুতুবদিয়ার সকল স্তরের সাধারণ নাগরিক সরাসরি এসে সাক্ষাত ও পরামর্শ গ্রহণ করতে পারেন।\n• নির্দিষ্ট সময়ে সাক্ষাতের জন্য অ্যাপের নিচে থাকা 'সাক্ষাতের অনুরোধ' ফর্ম পূরণ করে ট্র্যাকিং আইডি সংগ্রহ করুন।\n• জরুরি উন্নয়ন বা ব্যক্তিগত আবেদনের লিখিত কপি সরাসরি অফিসে জমাদান করা যায়।"
                                    else
                                        "• All citizens of Moheshkhali & Kutubdia can visit directly for consultation.\n• For scheduled appointments, submit an 'Appointment Request' via the form below to receive a Tracking ID.\n• Physical petition copies can be submitted at the office.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Surface(color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(if (language == AppLanguage.BN) "বন্ধ করুন" else "Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProfileEditDialog(
    profileInfo: ProfileInfo,
    language: AppLanguage,
    onSave: (ProfileInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nameBn by remember { mutableStateOf(profileInfo.nameBn) }
    var titleBn by remember { mutableStateOf(profileInfo.titleBn) }
    var briefIntroBn by remember { mutableStateOf(profileInfo.briefIntroBn) }
    var photoUrl by remember { mutableStateOf(profileInfo.photoUrl) }
    var fullBioBn by remember { mutableStateOf(profileInfo.fullBiographyBn) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = MediaStorageManager.persistCustomImage(context, uri)
            if (path != null) {
                photoUrl = path
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.88f).testTag("admin_profile_edit_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = RoyalBlue900, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == AppLanguage.BN) "সংসদ সদস্য পরিচিতি ও বায়ো সম্পাদনা" else "Edit MP Profile & Biography",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, GoldAccent, CircleShape)
                            ) {
                                AsyncOrLocalImage(
                                    imageUrl = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BN) "গ্যালারি থেকে ছবি" else "Pick Image", fontSize = 12.sp)
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = nameBn,
                            onValueChange = { nameBn = it },
                            label = { Text("পূর্ণ নাম (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = titleBn,
                            onValueChange = { titleBn = it },
                            label = { Text("পদবী (বাংলা)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = briefIntroBn,
                            onValueChange = { briefIntroBn = it },
                            label = { Text("সংক্ষিপ্ত পরিচিতি (বাংলা)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            maxLines = 4
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = fullBioBn,
                            onValueChange = { fullBioBn = it },
                            label = { Text("সম্পূর্ণ জীবনী (বাংলা)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 6
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                        }
                        Button(
                            onClick = {
                                val updated = profileInfo.copy(
                                    nameBn = nameBn,
                                    titleBn = titleBn,
                                    briefIntroBn = briefIntroBn,
                                    fullBiographyBn = fullBioBn,
                                    photoUrl = photoUrl
                                )
                                onSave(updated)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue900),
                            modifier = Modifier.weight(1f).testTag("save_profile_button")
                        ) {
                            Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save Changes")
                        }
                    }
                }
            }
        }
    }
}

// --- Notices & In-App Notification Center Dialog (Spark-Compatible) ---
@Composable
fun NoticesModalDialog(
    notifications: List<InAppNotification>,
    readNotificationIds: Set<String>,
    noticesAlertsEnabled: Boolean,
    currentUserId: String?,
    language: AppLanguage,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onToggleNoticeAlerts: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(0) } // 0 = All, 1 = Official Notices, 2 = My Updates
    var expandedId by remember { mutableStateOf<String?>(null) }

    val filteredItems = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            1 -> notifications.filter { it.type == InAppNotificationType.NOTICE }
            2 -> notifications.filter { it.type != InAppNotificationType.NOTICE }
            else -> notifications
        }
    }
    val unreadCount = remember(notifications, readNotificationIds) {
        notifications.count { it.id !in readNotificationIds }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .testTag("notices_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = RoyalBlue800,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "বিজ্ঞপ্তি ও নোটিফিকেশন" else "Notifications & Notices",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Text(
                                text = if (language == AppLanguage.BN) {
                                    if (unreadCount > 0) "অপঠিত বার্তা: $unreadCount টি" else "সকল বার্তা পঠিত"
                                } else {
                                    if (unreadCount > 0) "$unreadCount unread updates" else "All caught up"
                                },
                                fontSize = 11.sp,
                                color = if (unreadCount > 0) RedDanger else ForestGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("mark_all_notifications_read")
                        ) {
                            Text(
                                text = if (language == AppLanguage.BN) "সব পঠিত" else "Mark All",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue800
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preference Toggle Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = if (language == AppLanguage.BN) "বিজ্ঞপ্তি অ্যালার্ট পছন্দ" else "Notice Alert Preference",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (language == AppLanguage.BN)
                                    "ইন-অ্যাপ আপডেট রিয়েল-টাইম সিঙ্ক হয়; টপিক পছন্দ প্রোফাইলে সংরক্ষিত থাকে।"
                                else
                                    "In-app alerts sync in real time; topic preference is saved to your device & profile.",
                                fontSize = 10.sp,
                                color = TextMuted,
                                lineHeight = 13.sp
                            )
                        }
                        Switch(
                            checked = noticesAlertsEnabled,
                            onCheckedChange = onToggleNoticeAlerts,
                            modifier = Modifier.testTag("notices_alert_toggle")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        0 to (if (language == AppLanguage.BN) "সকল (${notifications.size})" else "All (${notifications.size})"),
                        1 to (if (language == AppLanguage.BN) "অফিসিয়াল নোটিশ" else "Notices"),
                        2 to (if (language == AppLanguage.BN) "আমার আপডেট" else "My Updates")
                    )
                    tabs.forEach { (idx, label) ->
                        FilterChip(
                            selected = selectedFilter == idx,
                            onClick = { selectedFilter = idx },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when {
                                    selectedFilter == 2 && currentUserId.isNullOrBlank() ->
                                        if (language == AppLanguage.BN)
                                            "ব্যক্তিগত আবেদন ও অভিযোগের আপডেট দেখতে অনুগ্রহ করে যোগাযোগ ট্যাব থেকে Google দিয়ে সাইন-ইন করুন।"
                                        else
                                            "Sign in with Google in the Contact tab to view your personal application and grievance updates."
                                    selectedFilter == 2 ->
                                        if (language == AppLanguage.BN) "এই মুহূর্তে কোনো আবেদন বা অভিযোগের আপডেট নেই।" else "No personal application or grievance updates yet."
                                    else ->
                                        if (language == AppLanguage.BN) "কোনো নোটিশ পাওয়া যায়নি।" else "No notifications available."
                                },
                                fontSize = 12.5.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            val isRead = item.id in readNotificationIds
                            val isExpanded = expandedId == item.id
                            val badgeColor = when {
                                item.isUrgent -> RedDanger
                                item.type == InAppNotificationType.NOTICE -> RoyalBlue800
                                else -> ForestGreen
                            }
                            val cardBg = when {
                                !isRead && item.isUrgent -> Color(0xFFFFF1F2)
                                !isRead -> Color(0xFFF0F9FF)
                                else -> Color(0xFFF8FAFC)
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (!isRead) badgeColor.copy(alpha = 0.45f) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedId = if (isExpanded) null else item.id
                                        if (!isRead) {
                                            onMarkRead(item.id)
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(badgeColor)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = badgeColor
                                            ) {
                                                Text(
                                                    text = if (language == AppLanguage.BN) item.badgeBn else item.badgeEn,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (language == AppLanguage.BN) item.dateBn else item.dateEn,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (language == AppLanguage.BN) item.titleBn else item.titleEn,
                                        fontSize = 13.sp,
                                        fontWeight = if (!isRead) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = if (language == AppLanguage.BN) item.bodyBn else item.bodyEn,
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp,
                                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!isRead && !isExpanded) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (language == AppLanguage.BN) "বিস্তারিত দেখতে ও পঠিত করতে ট্যাপ করুন" else "Tap to read full details",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = badgeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800)
                ) {
                    Text(if (language == AppLanguage.BN) "বন্ধ করুন" else "Close")
                }
            }
        }
    }
}




@Composable
private fun UnionRepresentativesSection(
    representatives: List<UnionRepresentative>,
    isAdminAuthenticated: Boolean,
    language: AppLanguage,
    onSaveRepresentative: (UnionRepresentative) -> Unit,
    onDeleteRepresentative: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedUpazilaTab by remember { mutableStateOf("মহেশখালী") }
    var editingRep by remember { mutableStateOf<UnionRepresentative?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val filteredReps = representatives.filter { it.upazila == selectedUpazilaTab }

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.BN) "ইউনিয়ন পরিষদ চেয়ারম্যান ও সচিব ডিরেক্টরি" else "Union Parishad Chairman & Secretary Directory",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900
                    )
                    Text(
                        text = if (language == AppLanguage.BN) "স্থানীয় জনপ্রতিনিধি ও কর্মকর্তাদের সরাসরি যোগাযোগের নম্বর" else "Direct contact numbers of local representatives",
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
                if (isAdminAuthenticated) {
                    IconButton(
                        onClick = {
                            editingRep = UnionRepresentative(id = "UR_${System.currentTimeMillis()}", upazila = selectedUpazilaTab)
                            showEditDialog = true
                        },
                        modifier = Modifier.background(RoyalBlue800, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Representative", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Upazila Tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("মহেশখালী", "কুতুবদিয়া").forEach { upazila ->
                    val isSelected = selectedUpazilaTab == upazila
                    Button(
                        onClick = { selectedUpazilaTab = upazila },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) RoyalBlue800 else Color(0xFFF1F5F9),
                            contentColor = if (isSelected) Color.White else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(upazila, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredReps.forEach { rep ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BN) rep.unionNameBn else rep.unionNameEn,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RoyalBlue900
                                )
                                if (isAdminAuthenticated) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingRep = rep
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreen, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteRepresentative(rep.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Chairman Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "চেয়ারম্যান: ${rep.chairmanNameBn}" else "Chairman: ${rep.chairmanNameBn}",
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { IntentUtils.safeDial(context, rep.chairmanPhone, language) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                                    border = BorderStroke(1.dp, ForestGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "কল করুন" else "Call Now", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Secretary Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "সচিব: ${rep.secretaryNameBn}" else "Secretary: ${rep.secretaryNameBn}",
                                        fontSize = 12.sp, color = Color(0xFF475569)
                                    )
                                }
                                OutlinedButton(
                                    onClick = { IntentUtils.safeDial(context, rep.secretaryPhone, language) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlue800),
                                    border = BorderStroke(1.dp, RoyalBlue800),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (language == AppLanguage.BN) "কল করুন" else "Call Now", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && editingRep != null) {
        AdminUnionRepEditDialog(
            rep = editingRep!!,
            language = language,
            onSave = {
                onSaveRepresentative(it)
                showEditDialog = false
                editingRep = null
            },
            onDismiss = {
                showEditDialog = false
                editingRep = null
            }
        )
    }
}

@Composable
private fun AdminUnionRepEditDialog(
    rep: UnionRepresentative,
    language: AppLanguage,
    onSave: (UnionRepresentative) -> Unit,
    onDismiss: () -> Unit
) {
    var unionBn by remember { mutableStateOf(rep.unionNameBn) }
    var unionEn by remember { mutableStateOf(rep.unionNameEn) }
    var upazila by remember { mutableStateOf(rep.upazila) }
    var chairmanName by remember { mutableStateOf(rep.chairmanNameBn) }
    var chairmanPhone by remember { mutableStateOf(rep.chairmanPhone) }
    var secretaryName by remember { mutableStateOf(rep.secretaryNameBn) }
    var secretaryPhone by remember { mutableStateOf(rep.secretaryPhone) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth(0.92f)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (language == AppLanguage.BN) "ইউনিয়ন প্রতিনিধি তথ্য" else "Union Representative Info",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RoyalBlue900
                )
                OutlinedTextField(
                    value = unionBn, onValueChange = { unionBn = it },
                    label = { Text(if (language == AppLanguage.BN) "ইউনিয়ন নাম (বাংলা)" else "Union Name (BN)") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upazila, onValueChange = { upazila = it },
                    label = { Text(if (language == AppLanguage.BN) "উপজেলা (মহেশখালী / কুতুবদিয়া)" else "Upazila") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chairmanName, onValueChange = { chairmanName = it },
                    label = { Text(if (language == AppLanguage.BN) "চেয়ারম্যানের নাম" else "Chairman Name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chairmanPhone, onValueChange = { chairmanPhone = it },
                    label = { Text(if (language == AppLanguage.BN) "চেয়ারম্যানের ফোন নম্বর" else "Chairman Phone") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secretaryName, onValueChange = { secretaryName = it },
                    label = { Text(if (language == AppLanguage.BN) "সচিবের নাম" else "Secretary Name") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = secretaryPhone, onValueChange = { secretaryPhone = it },
                    label = { Text(if (language == AppLanguage.BN) "সচিবের ফোন নম্বর" else "Secretary Phone") }, modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = {
                            val updated = rep.copy(
                                unionNameBn = unionBn,
                                unionNameEn = unionEn.ifBlank { unionBn },
                                upazila = upazila,
                                chairmanNameBn = chairmanName,
                                chairmanPhone = chairmanPhone,
                                secretaryNameBn = secretaryName,
                                secretaryPhone = secretaryPhone
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ" else "Save")
                    }
                }
            }
        }
    }
}


