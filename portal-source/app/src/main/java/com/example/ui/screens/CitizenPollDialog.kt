package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import com.example.data.model.AppLanguage
import com.example.data.model.CitizenPollItem
import com.example.data.repository.DefaultPortalContent
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RoyalBlue800
import com.example.util.AuthManager

@Composable
fun CitizenPollDialog(
    language: AppLanguage,
    pollVotes: Map<Int, Int>,
    userVotedOption: Int?,
    currentUserId: String? = null,
    activePoll: CitizenPollItem = DefaultPortalContent.defaultCitizenPoll,
    onVote: (Int) -> Unit,
    onSubmitVoice: (name: String, phone: String, union: String, message: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeTab by remember { mutableStateOf(0) } // 0 = Poll, 1 = Citizen Voice Form
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedUnion by remember { mutableStateOf("মাতারবাড়ি") }
    var opinionMessage by remember { mutableStateOf("") }
    var submittedVoice by remember { mutableStateOf(false) }
    var isSubmittingVoice by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    val options = remember(activePoll, language) {
        if (!activePoll.isPublished) {
            emptyList()
        } else {
            activePoll.options.map { opt ->
                if (language == AppLanguage.BN) opt.labelBn else opt.labelEn.ifBlank { opt.labelBn }
            }
        }
    }

    val unions = listOf(
        "মাতারবাড়ি", "ধলঘাটা", "কালারমারছড়া", "বড় মহেশখালী", "ছোট মহেশখালী",
        "হোয়ানক", "কুতুবজোম", "শাপলাপুর", "বড়ঘোপ", "আলী আকবর ডেইল",
        "কৈয়ারবিল", "লেমশীখালী", "উত্তর ধুরুং", "দক্ষিণ ধুরুং"
    )

    val totalVotes = remember(pollVotes) {
        pollVotes.values.sum().coerceAtLeast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("citizen_poll_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Color(0xFFF57C00),
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
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Poll,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == AppLanguage.BN) {
                                        activePoll.titleBn.ifBlank { "জনমত ও নাগরিক অংশগ্রহণ" }
                                    } else {
                                        activePoll.titleEn.ifBlank { "Citizen Poll & Voice" }
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == AppLanguage.BN) "আপনার মতামতই উন্নয়নের চালিকাশক্তি" else "Your voice shapes our constituency",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFFFFF8E1),
                    contentColor = Color(0xFFE65100)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                if (language == AppLanguage.BN) "📊 নাগরিক গণভোট" else "📊 Public Poll",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                if (language == AppLanguage.BN) "✍️ মতামত পাঠান" else "✍️ Citizen Voice",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }

                if (activeTab == 0) {
                    // Poll Section
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(1.dp, Color(0xFFFFE0B2)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LiveHelp,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.BN) "চলমান নাগরিক সমীক্ষা" else "Active Public Poll",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (language == AppLanguage.BN)
                                            activePoll.questionBn.ifBlank { "মহেশখালী ও কুতুবদিয়া উন্নয়নে আপনার মতে পরবর্তী সর্বোচ্চ অগ্রাধিকার কোনটি হওয়া উচিত?" }
                                        else
                                            activePoll.questionEn.ifBlank { "What should be the highest priority for the next development phase of Moheshkhali & Kutubdia?" },
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        lineHeight = 20.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (options.isEmpty()) {
                                            if (language == AppLanguage.BN) "বর্তমানে কোনো সক্রিয় জরিপ চালু নেই।" else "No active public poll is published right now."
                                        } else if (userVotedOption != null) {
                                            if (language == AppLanguage.BN) "✅ আপনি ভোট দিয়েছেন। মোট ভোট: $totalVotes টি" else "✅ You voted. Total votes: $totalVotes"
                                        } else {
                                            if (language == AppLanguage.BN) "পছন্দের বিকল্পে ট্যাপ করে এখনই আপনার ভোট দিন:" else "Tap an option below to submit your vote:"
                                        },
                                        fontSize = 11.5.sp,
                                        color = if (userVotedOption != null) ForestGreen else Color(0xFF78350F)
                                    )
                                }
                            }
                        }

                        if (options.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "আপনার মতামত সরাসরি জানাতে 'মতামত পাঠান' ট্যাব ব্যবহার করুন।" else "Please use the 'Citizen Voice' tab to share your feedback directly.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        items(options.indices.toList()) { index ->
                            val optionText = options[index]
                            val votes = pollVotes[index] ?: 0
                            val percentage = (votes.toFloat() / totalVotes * 100f).coerceIn(0f, 100f)
                            val isSelected = userVotedOption == index
                            val animProgress by animateFloatAsState(targetValue = percentage / 100f, label = "poll_anim")

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSelected) ForestGreen else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onVote(index)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onVote(index) },
                                            colors = RadioButtonDefaults.colors(selectedColor = ForestGreen)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = optionText,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Progress Bar
                                    LinearProgressIndicator(
                                        progress = { animProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = if (isSelected) ForestGreen else Color(0xFFF57C00),
                                        trackColor = Color(0xFFE2E8F0)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${String.format("%.1f", percentage)}%",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ForestGreen else Color(0xFFE65100)
                                        )
                                        Text(
                                            text = if (language == AppLanguage.BN) "$votes জন ভোট দিয়েছেন" else "$votes votes",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Citizen Voice Form
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = if (language == AppLanguage.BN) "আপনার মতামত বা পরামর্শ সরাসরি জানান" else "Submit your suggestion to the MP's desk",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue800
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "এলাকার সমস্যা, সংস্কার বা নতুন প্রকল্প বিষয়ে আপনার অভিমত দিন।" else "Share your views on local civic issues, reform or proposed projects.",
                                fontSize = 11.5.sp,
                                color = Color.Gray
                            )
                        }

                        if (currentUserId.isNullOrBlank()) {
                            item {
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
                                                "মতামত পাঠাতে আপনার Google অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।"
                                            else
                                                "Please sign in with Google to submit your voice.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                if (!isSigningIn) {
                                                    isSigningIn = true
                                                    AuthManager.signInWithGoogle(
                                                        context = context,
                                                        credentialManager = credentialManager,
                                                        scope = scope,
                                                        onAuthSuccess = { isSigningIn = false },
                                                        onAuthError = { err ->
                                                            isSigningIn = false
                                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                                        },
                                                        onAuthCancelled = { isSigningIn = false }
                                                    )
                                                }
                                            },
                                            enabled = !isSigningIn,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("voice_google_signin_button")
                                        ) {
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

                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(if (language == AppLanguage.BN) "আপনার নাম" else "Your Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text(if (language == AppLanguage.BN) "মোবাইল নম্বর" else "Mobile Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Text(
                                text = if (language == AppLanguage.BN) "আপনার ইউনিয়ন নির্বাচন করুন" else "Select Your Union",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(unions) { u ->
                                    val isSelected = selectedUnion == u
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedUnion = u },
                                        label = { Text(u, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFF57C00),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = opinionMessage,
                                onValueChange = { opinionMessage = it },
                                label = { Text(if (language == AppLanguage.BN) "আপনার প্রস্তাব বা মতামত লিখুন" else "Your Message / Proposal") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Button(
                                onClick = {
                                    if (currentUserId.isNullOrBlank()) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (language == AppLanguage.BN) "মতামত পাঠাতে প্রথমে Google দিয়ে সাইন-ইন করুন" else "Please sign in with Google first",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    val trimmedName = name.trim()
                                    val trimmedPhone = phone.trim()
                                    val trimmedMessage = opinionMessage.trim()
                                    if (trimmedName.isBlank() || trimmedPhone.length < 6 || trimmedMessage.isBlank()) {
                                        android.widget.Toast.makeText(
                                            context,
                                            if (language == AppLanguage.BN) "দয়া করে নাম, কমপক্ষে ৬ অঙ্কের মোবাইল নম্বর এবং মতামত লিখুন" else "Please enter your name, valid phone (at least 6 chars), and message",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }
                                    isSubmittingVoice = true
                                    onSubmitVoice(
                                        trimmedName,
                                        trimmedPhone,
                                        selectedUnion,
                                        trimmedMessage,
                                        {
                                            isSubmittingVoice = false
                                            submittedVoice = true
                                            opinionMessage = ""
                                        },
                                        { err ->
                                            isSubmittingVoice = false
                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isSubmittingVoice,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_voice_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSubmittingVoice) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = if (language == AppLanguage.BN) "মতামত জমা দিন" else "Submit Citizen Voice",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        if (submittedVoice) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9),
                                    border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = ForestGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (language == AppLanguage.BN) "ধন্যবাদ! আপনার মূল্যবান মতামত সফলভাবে সংরক্ষিত হয়েছে।" else "Thank you! Your opinion has been registered successfully.",
                                            fontSize = 12.sp,
                                            color = ForestGreen,
                                            fontWeight = FontWeight.Bold
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
