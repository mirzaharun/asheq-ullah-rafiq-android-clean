package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppLanguage
import com.example.data.model.DevelopmentProject
import com.example.data.model.GovtReportItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RoyalBlue800
import com.example.ui.theme.RoyalBlue900
import com.example.util.MediaStorageManager

@Composable
fun ProjectEditDialog(
    project: DevelopmentProject?,
    isNew: Boolean,
    language: AppLanguage,
    onSave: (DevelopmentProject) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var titleBn by remember { mutableStateOf(project?.titleBn ?: "") }
    var titleEn by remember { mutableStateOf(project?.titleEn ?: "") }
    var categoryBn by remember { mutableStateOf(project?.categoryBn?.ifBlank { STANDARD_DEVELOPMENT_CATEGORIES.first().labelBn } ?: STANDARD_DEVELOPMENT_CATEGORIES.first().labelBn) }
    var categoryEn by remember { mutableStateOf(project?.categoryEn?.ifBlank { STANDARD_DEVELOPMENT_CATEGORIES.first().labelEn } ?: STANDARD_DEVELOPMENT_CATEGORIES.first().labelEn) }
    var budgetBn by remember { mutableStateOf(project?.budgetBn ?: "") }
    var completionPercentage by remember { mutableStateOf(project?.completionPercentage?.toString() ?: "0") }
    var status by remember { mutableStateOf(project?.status ?: "Ongoing") }
    var upazila by remember { mutableStateOf(project?.upazila ?: "Moheshkhali") }
    var union by remember { mutableStateOf(project?.union ?: "All") }
    var year by remember { mutableStateOf(project?.year?.toString() ?: "2024") }
    var descriptionBn by remember { mutableStateOf(project?.descriptionBn ?: "") }
    var impactBn by remember { mutableStateOf(project?.impactBn ?: "") }
    var imageUrl by remember { mutableStateOf(project?.imageUrl ?: "drawable:moheshkhali_banner") }
    var pdfUrl by remember { mutableStateOf(project?.pdfUrl ?: "") }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var isUploadingPdf by remember { mutableStateOf(false) }
    var uploadStatusMsg by remember { mutableStateOf("") }

    val sectorOptions = STANDARD_DEVELOPMENT_CATEGORIES.map { it.labelBn to it.labelEn }

    // Image Picker for Project Media
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingMedia = true
            uploadStatusMsg = if (language == AppLanguage.BN) "মিডিয়া আপলোড হচ্ছে..." else "Uploading media..."
            MediaStorageManager.uploadToFirebaseStorage(
                context = context,
                sourceUri = it,
                destinationFolder = "development_media",
                onSuccess = { downloadUrl ->
                    isUploadingMedia = false
                    imageUrl = downloadUrl
                    uploadStatusMsg = if (language == AppLanguage.BN) "ছবি সফলভাবে সংযুক্ত হয়েছে" else "Image linked successfully"
                    Toast.makeText(context, uploadStatusMsg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isUploadingMedia = false
                    uploadStatusMsg = err
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // PDF Document Picker
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingPdf = true
            uploadStatusMsg = if (language == AppLanguage.BN) "গেজেট / পিডিএফ আপলোড হচ্ছে..." else "Uploading PDF..."
            val customName = "doc_${System.currentTimeMillis()}.pdf"
            MediaStorageManager.uploadToFirebaseStorage(
                context = context,
                sourceUri = it,
                destinationFolder = "development_documents",
                customFileName = customName,
                onSuccess = { downloadUrl ->
                    isUploadingPdf = false
                    pdfUrl = downloadUrl
                    uploadStatusMsg = if (language == AppLanguage.BN) "পিডিএফ সফলভাবে আপলোড হয়েছে" else "PDF uploaded successfully"
                    Toast.makeText(context, uploadStatusMsg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isUploadingPdf = false
                    uploadStatusMsg = err
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("project_edit_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            if (isNew) "নতুন প্রকল্প যুক্ত করুন" else "প্রকল্প তথ্য সংশোধন"
                        } else {
                            if (isNew) "Add New Project" else "Edit Project Details"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Titles
                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text("প্রকল্পের নাম (বাংলা) *") },
                        modifier = Modifier.fillMaxWidth().testTag("project_title_bn_input"),
                        singleLine = false,
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text("Project Title (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2
                    )

                    // Sector Selection Chips (11 standard categories + custom category field)
                    Text("উন্নয়ন ক্যাটাগরি (Category):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sectorOptions.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { (bn, en) ->
                                    val isSelected = normalizeDevelopmentText(categoryBn) == normalizeDevelopmentText(bn)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            categoryBn = bn
                                            categoryEn = en
                                        },
                                        label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = categoryBn,
                        onValueChange = {
                            categoryBn = it
                            if (categoryEn.isBlank()) categoryEn = it
                        },
                        label = { Text(if (language == AppLanguage.BN) "ক্যাটাগরি নাম (কাস্টম বা নির্বাচিত)" else "Category Name (Selected or Custom)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Budget & Completion %
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = budgetBn,
                            onValueChange = { budgetBn = it },
                            label = { Text("বাজেট (যেমন: ৫১৮ কোটি টাকা)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = completionPercentage,
                            onValueChange = { completionPercentage = it.filter { ch -> ch.isDigit() } },
                            label = { Text("অগ্রগতি % (0-100)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Upazila Selector & Year
                    Text(if (language == AppLanguage.BN) "উপজেলা (Upazila):" else "Upazila:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RoyalBlue900)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Moheshkhali" to "মহেশখালী", "Kutubdia" to "কুতুবদিয়া").forEach { (code, bnLabel) ->
                            val isSelected = upazila.equals(code, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { upazila = code },
                                label = { Text(if (language == AppLanguage.BN) bnLabel else code, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = union,
                            onValueChange = { union = it },
                            label = { Text(if (language == AppLanguage.BN) "ইউনিয়ন (ঐচ্ছিক)" else "Union (Optional)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter { ch -> ch.isDigit() } },
                            label = { Text("সাল (Year)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Status
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Ongoing", "Completed", "Planned").forEach { st ->
                            val isSelected = status.equals(st, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { status = st },
                                label = {
                                    val stLabel = when(st) {
                                        "Ongoing" -> if (language == AppLanguage.BN) "চলমান" else "Ongoing"
                                        "Completed" -> if (language == AppLanguage.BN) "বাস্তবায়িত" else "Completed"
                                        else -> if (language == AppLanguage.BN) "পরিকল্পিত" else "Planned"
                                    }
                                    Text(stLabel, fontSize = 11.sp)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Description & Impact
                    OutlinedTextField(
                        value = descriptionBn,
                        onValueChange = { descriptionBn = it },
                        label = { Text("প্রকল্পের বিবরণ (Description)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    OutlinedTextField(
                        value = impactBn,
                        onValueChange = { impactBn = it },
                        label = { Text("জনজীবনে ইতিবাচক প্রভাব (Impact)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Media Uploads Box
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("মিডিয়া ও ডকুমেন্ট সংযুক্তি (Firebase Storage):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)

                            // Image / Video Upload
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isUploadingMedia
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ছবি/ভিডিও আপলোড", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isUploadingMedia) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (imageUrl.isNotBlank()) {
                                    Text("সংযুক্ত আছে ✓", fontSize = 11.sp, color = ForestGreen, fontWeight = FontWeight.Bold)
                                }
                            }

                            // PDF Document Upload
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { pdfPicker.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue800),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isUploadingPdf
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অফিসিয়াল গেজেট/PDF আপলোড", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isUploadingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (pdfUrl.isNotBlank()) {
                                    Text("পিডিএফ সংযুক্ত ✓", fontSize = 11.sp, color = RoyalBlue800, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (uploadStatusMsg.isNotBlank()) {
                                Text(uploadStatusMsg, fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = {
                            if (titleBn.isNotBlank()) {
                                val projId = project?.id?.ifEmpty { "P_${System.currentTimeMillis()}" }
                                    ?: "P_${System.currentTimeMillis()}"
                                val pct = completionPercentage.toIntOrNull() ?: 0
                                val yr = year.toIntOrNull() ?: 2024
                                onSave(
                                    (project ?: DevelopmentProject()).copy(
                                        id = projId,
                                        titleBn = titleBn.trim(),
                                        titleEn = titleEn.trim().ifEmpty { titleBn.trim() },
                                        categoryBn = categoryBn,
                                        categoryEn = categoryEn,
                                        budgetBn = budgetBn.trim(),
                                        completionPercentage = pct.coerceIn(0, 100),
                                        status = status,
                                        upazila = upazila.trim().ifEmpty { "Moheshkhali" },
                                        union = union.trim().ifEmpty { "All" },
                                        year = yr,
                                        descriptionBn = descriptionBn.trim(),
                                        impactBn = impactBn.trim(),
                                        imageUrl = imageUrl,
                                        pdfUrl = pdfUrl
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = titleBn.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f).testTag("save_project_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun GovtReportEditDialog(
    report: GovtReportItem?,
    isNew: Boolean,
    language: AppLanguage,
    onSave: (GovtReportItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var titleBn by remember { mutableStateOf(report?.titleBn ?: "") }
    var titleEn by remember { mutableStateOf(report?.titleEn ?: "") }
    var categoryBn by remember { mutableStateOf(report?.categoryBn ?: "সরকারি গেজেট") }
    var categoryEn by remember { mutableStateOf(report?.categoryEn ?: "Official Gazette") }
    var publishDateBn by remember { mutableStateOf(report?.publishDateBn ?: "অক্টোবর ২০২৪") }
    var year by remember { mutableStateOf(report?.year?.toString() ?: "2024") }
    var ministryOrDeptBn by remember { mutableStateOf(report?.ministryOrDeptBn ?: "পরিকল্পনা মন্ত্রণালয় ও স্থানীয় সরকার বিভাগ") }
    var fileSizeMb by remember { mutableStateOf(report?.fileSizeMb ?: "2.5 MB") }
    var pdfUrl by remember { mutableStateOf(report?.pdfUrl ?: "") }
    var mediaUrl by remember { mutableStateOf(report?.mediaUrl ?: "") }
    var mediaType by remember { mutableStateOf(report?.mediaType ?: "PDF") }
    var isUploadingPdf by remember { mutableStateOf(false) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var uploadStatusMsg by remember { mutableStateOf("") }

    val reportCategories = listOf(
        Pair("সরকারি গেজেট", "Official Gazette"),
        Pair("প্রকল্প প্রতিবেদন", "Project Report"),
        Pair("অডিট ও মূল্যায়ন", "Audit & Evaluation"),
        Pair("পরিবেশ সমীক্ষা", "Environmental Survey")
    )

    // PDF Document Picker for Firebase Storage
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingPdf = true
            uploadStatusMsg = if (language == AppLanguage.BN) "গেজেট ফাইল আপলোড হচ্ছে..." else "Uploading Gazette PDF..."
            val customName = "gazette_${System.currentTimeMillis()}.pdf"
            MediaStorageManager.uploadToFirebaseStorage(
                context = context,
                sourceUri = it,
                destinationFolder = "development_documents",
                customFileName = customName,
                onSuccess = { downloadUrl ->
                    isUploadingPdf = false
                    pdfUrl = downloadUrl
                    mediaType = "PDF"
                    uploadStatusMsg = if (language == AppLanguage.BN) "গেজেট সফলভাবে আপলোড হয়েছে" else "Gazette uploaded successfully"
                    Toast.makeText(context, uploadStatusMsg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isUploadingPdf = false
                    uploadStatusMsg = err
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Media Picker for Images and Videos linked to reports
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingMedia = true
            uploadStatusMsg = if (language == AppLanguage.BN) "মিডিয়া আপলোড হচ্ছে..." else "Uploading media attachment..."
            MediaStorageManager.uploadToFirebaseStorage(
                context = context,
                sourceUri = it,
                destinationFolder = "development_media",
                onSuccess = { downloadUrl ->
                    isUploadingMedia = false
                    mediaUrl = downloadUrl
                    uploadStatusMsg = if (language == AppLanguage.BN) "মিডিয়া সংযুক্ত হয়েছে" else "Media attached successfully"
                    Toast.makeText(context, uploadStatusMsg, Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isUploadingMedia = false
                    uploadStatusMsg = err
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("govt_report_edit_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) {
                            if (isNew) "সরকারি প্রতিবেদন ও গেজেট যোগ" else "প্রতিবেদন/গেজেট সম্পাদনা"
                        } else {
                            if (isNew) "Add Govt Report / Gazette" else "Edit Report / Gazette"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Selection Chips
                    Text("প্রতিবেদনের ধরন (Category):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        reportCategories.take(2).forEach { (bn, en) ->
                            val isSelected = categoryBn == bn
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryBn = bn
                                    categoryEn = en
                                },
                                label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        reportCategories.drop(2).forEach { (bn, en) ->
                            val isSelected = categoryBn == bn
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    categoryBn = bn
                                    categoryEn = en
                                },
                                label = { Text(if (language == AppLanguage.BN) bn else en, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Titles
                    OutlinedTextField(
                        value = titleBn,
                        onValueChange = { titleBn = it },
                        label = { Text("প্রতিবেদন / গেজেটের শিরোনাম (বাংলা) *") },
                        modifier = Modifier.fillMaxWidth().testTag("report_title_bn_input"),
                        singleLine = false,
                        maxLines = 3
                    )
                    OutlinedTextField(
                        value = titleEn,
                        onValueChange = { titleEn = it },
                        label = { Text("Report / Gazette Title (English)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )

                    // Ministry & Dept
                    OutlinedTextField(
                        value = ministryOrDeptBn,
                        onValueChange = { ministryOrDeptBn = it },
                        label = { Text("মন্ত্রণালয় / সরকারি দপ্তর") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date & Year & File Size
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = publishDateBn,
                            onValueChange = { publishDateBn = it },
                            label = { Text("প্রকাশের তারিখ") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("সাল (Year)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Document & Media Upload Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ফাইল আপলোড (Firebase Storage):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBlue900)

                            // PDF Picker
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { pdfPicker.launch("application/pdf") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isUploadingPdf
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("অফিসিয়াল PDF গেজেট ফাইল আপলোড", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isUploadingPdf) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (pdfUrl.isNotBlank()) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                }
                            }

                            // Secondary Media Attachment Picker (Images/Videos)
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isUploadingMedia
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBlue800)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ছবি / ভিডিও প্রমাণ সংযুক্ত করুন", fontSize = 11.sp, color = RoyalBlue800)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isUploadingMedia) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (mediaUrl.isNotBlank()) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                }
                            }

                            if (uploadStatusMsg.isNotBlank()) {
                                Text(uploadStatusMsg, fontSize = 11.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                    }
                    Button(
                        onClick = {
                            if (titleBn.isNotBlank()) {
                                val repId = report?.id?.ifEmpty { "rep_${System.currentTimeMillis()}" }
                                    ?: "rep_${System.currentTimeMillis()}"
                                val yr = year.toIntOrNull() ?: 2024
                                onSave(
                                    (report ?: GovtReportItem()).copy(
                                        id = repId,
                                        titleBn = titleBn.trim(),
                                        titleEn = titleEn.trim().ifEmpty { titleBn.trim() },
                                        categoryBn = categoryBn,
                                        categoryEn = categoryEn,
                                        publishDateBn = publishDateBn.trim(),
                                        year = yr,
                                        ministryOrDeptBn = ministryOrDeptBn.trim(),
                                        fileSizeMb = fileSizeMb.trim(),
                                        pdfUrl = pdfUrl,
                                        mediaUrl = mediaUrl,
                                        mediaType = if (pdfUrl.isNotBlank()) "PDF" else "IMAGE"
                                    )
                                )
                                onDismiss()
                            }
                        },
                        enabled = titleBn.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier.weight(1f).testTag("save_report_button")
                    ) {
                        Text(if (language == AppLanguage.BN) "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}
