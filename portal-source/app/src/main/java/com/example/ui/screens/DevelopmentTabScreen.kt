package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppConfig
import com.example.data.model.AppLanguage
import com.example.data.model.DevelopmentCategoryItem
import com.example.data.model.DevelopmentProject
import com.example.data.model.GovtReportItem
import com.example.data.repository.DefaultPortalContent
import com.example.ui.components.AsyncOrLocalImage
import com.example.ui.components.PortalIconMapper
import com.example.ui.components.SharedTextFooter
import com.example.ui.theme.*
import com.example.util.MediaStorageManager
import java.text.Normalizer

internal data class DevelopmentCategory(
    val id: String,
    val icon: ImageVector,
    val labelBn: String,
    val labelEn: String,
    val matchKeywords: List<String> = emptyList()
)

internal val STANDARD_DEVELOPMENT_CATEGORIES = listOf(
    DevelopmentCategory(
        id = "power_energy",
        icon = Icons.Default.Bolt,
        labelBn = "বিদ্যুৎ ও জ্বালানি",
        labelEn = "Power & Energy",
        matchKeywords = listOf("বিদ্যুৎ", "জ্বালানি", "power", "energy")
    ),
    DevelopmentCategory(
        id = "roads_comm",
        icon = Icons.Default.AddRoad,
        labelBn = "সড়ক ও যোগাযোগ",
        labelEn = "Roads & Connectivity",
        matchKeywords = listOf("সড়ক", "যোগাযোগ", "সেতু", "road", "communication", "connectivity", "bridge", "transport")
    ),
    DevelopmentCategory(
        id = "embankment_disaster",
        icon = Icons.Default.Shield,
        labelBn = "বেড়িবাঁধ ও দুর্যোগ ব্যবস্থাপনা",
        labelEn = "Embankments & Disaster",
        matchKeywords = listOf("বেড়িবাঁধ", "দুর্যোগ", "সুরক্ষা", "উপকূল", "embankment", "disaster", "coastal", "protection")
    ),
    DevelopmentCategory(
        id = "education",
        icon = Icons.Default.School,
        labelBn = "শিক্ষা",
        labelEn = "Education",
        matchKeywords = listOf("শিক্ষা", "education")
    ),
    DevelopmentCategory(
        id = "healthcare",
        icon = Icons.Default.LocalHospital,
        labelBn = "স্বাস্থ্যসেবা",
        labelEn = "Healthcare",
        matchKeywords = listOf("স্বাস্থ্য", "চিকিৎসা", "health", "healthcare")
    ),
    DevelopmentCategory(
        id = "infrastructure",
        icon = Icons.Default.Foundation,
        labelBn = "অবকাঠামো",
        labelEn = "Infrastructure",
        matchKeywords = listOf("অবকাঠামো", "মেগা প্রকল্প", "গ্রামীণ", "ইউনিয়ন", "infrastructure", "mega", "rural", "union")
    ),
    DevelopmentCategory(
        id = "port_waterways",
        icon = Icons.Default.DirectionsBoat,
        labelBn = "বন্দর ও নৌপথ",
        labelEn = "Port & Waterways",
        matchKeywords = listOf("বন্দর", "নৌপথ", "শিল্প", "port", "waterway", "maritime", "industry")
    ),
    DevelopmentCategory(
        id = "tourism",
        icon = Icons.Default.BeachAccess,
        labelBn = "পর্যটন",
        labelEn = "Tourism",
        matchKeywords = listOf("পর্যটন", "tourism")
    ),
    DevelopmentCategory(
        id = "water_sanitation",
        icon = Icons.Default.WaterDrop,
        labelBn = "পানি ও স্যানিটেশন",
        labelEn = "Water & Sanitation",
        matchKeywords = listOf("পানি", "স্যানিটেশন", "water", "sanitation")
    ),
    DevelopmentCategory(
        id = "agri_fisheries",
        icon = Icons.Default.Agriculture,
        labelBn = "কৃষি ও মৎস্য",
        labelEn = "Agriculture & Fisheries",
        matchKeywords = listOf("কৃষি", "মৎস্য", "agriculture", "fisheries", "fishing")
    ),
    DevelopmentCategory(
        id = "social_welfare",
        icon = Icons.Default.VolunteerActivism,
        labelBn = "সামাজিক ও জনকল্যাণমূলক উন্নয়ন",
        labelEn = "Social & Public Welfare",
        matchKeywords = listOf("সামাজিক", "জনকল্যাণ", "আর্কাইভ", "social", "welfare", "archive")
    )
)

internal fun normalizeDevelopmentText(text: String): String {
    return Normalizer.normalize(text.trim(), Normalizer.Form.NFC)
        .replace("\u09A1\u09BC", "\u09DC")
        .replace("\u09A2\u09BC", "\u09DD")
        .replace("\u09AF\u09BC", "\u09DF")
        .lowercase()
}

internal fun projectMatchesUpazila(project: DevelopmentProject, selectedUpazila: String): Boolean {
    val normProjectUpazila = normalizeDevelopmentText(project.upazila)
    return when (selectedUpazila) {
        "Moheshkhali" -> normProjectUpazila == "moheshkhali" ||
            normProjectUpazila == "maheshkhali" ||
            normProjectUpazila == normalizeDevelopmentText("মহেশখালী")
        "Kutubdia" -> normProjectUpazila == "kutubdia" ||
            normProjectUpazila == normalizeDevelopmentText("কুতুবদিয়া")
        else -> false
    }
}

internal fun DevelopmentCategory.matches(project: DevelopmentProject): Boolean {
    val normBn = normalizeDevelopmentText(project.categoryBn)
    val normEn = normalizeDevelopmentText(project.categoryEn)
    val normLabelBn = normalizeDevelopmentText(labelBn)
    val normLabelEn = normalizeDevelopmentText(labelEn)

    if ((normBn.isNotEmpty() && normBn == normLabelBn) ||
        (normEn.isNotEmpty() && normEn == normLabelEn)
    ) {
        return true
    }

    return matchKeywords.any { kw ->
        val normKw = normalizeDevelopmentText(kw)
        (normBn.isNotEmpty() && normBn.contains(normKw)) ||
            (normEn.isNotEmpty() && normEn.contains(normKw))
    }
}

private fun buildCategoriesForUpazila(
    projects: List<DevelopmentProject>,
    selectedUpazila: String,
    configuredCategories: List<DevelopmentCategoryItem> = DefaultPortalContent.defaultDevelopmentCategories
): List<DevelopmentCategory> {
    val baseCategories = configuredCategories
        .filter { it.isPublished }
        .sortedBy { it.order }
        .map { item ->
            DevelopmentCategory(
                id = item.id,
                icon = PortalIconMapper.resolveIcon(item.iconKey, Icons.Default.Category),
                labelBn = item.labelBn,
                labelEn = item.labelEn.ifBlank { item.labelBn },
                matchKeywords = item.matchKeywords
            )
        }
    val upazilaProjects = projects.filter { projectMatchesUpazila(it, selectedUpazila) }
    val customCategories = upazilaProjects
        .filter { proj ->
            (proj.categoryBn.isNotBlank() || proj.categoryEn.isNotBlank()) &&
                baseCategories.none { it.matches(proj) }
        }
        .distinctBy { normalizeDevelopmentText(it.categoryBn.ifBlank { it.categoryEn }) }
        .map { proj ->
            val bn = proj.categoryBn.ifBlank { proj.categoryEn }
            val en = proj.categoryEn.ifBlank { proj.categoryBn }
            DevelopmentCategory(
                id = "custom_${normalizeDevelopmentText(bn)}",
                icon = Icons.Default.Category,
                labelBn = bn,
                labelEn = en,
                matchKeywords = listOf(bn, en).filter { it.isNotBlank() }
            )
        }
    return baseCategories + customCategories
}

/**
 * Development Tab Screen (উন্নয়ন ও অগ্রযাত্রা)
 * Flow: Select Upazila (Moheshkhali / Kutubdia) -> Dynamic Development Categories -> Filtered Projects.
 */
@Composable
fun DevelopmentTabScreen(
    language: AppLanguage,
    appConfig: AppConfig = AppConfig(),
    projects: List<DevelopmentProject> = emptyList(),
    categories: List<DevelopmentCategoryItem> = DefaultPortalContent.defaultDevelopmentCategories,
    govtReports: List<GovtReportItem> = emptyList(),
    isAdminAuthenticated: Boolean = false,
    onAdminAddClick: () -> Unit = {},
    onSaveProject: (DevelopmentProject) -> Unit = {},
    onDeleteProject: (String) -> Unit = {},
    onSaveReport: (GovtReportItem) -> Unit = {},
    onDeleteReport: (String) -> Unit = {},
    onProjectDetailClick: (DevelopmentProject) -> Unit,
    onAboutClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onPolicyClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    onTermsClick: () -> Unit = onPolicyClick,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedUpazila by remember { mutableStateOf<String?>(null) } // "Moheshkhali" or "Kutubdia"
    var selectedUnion by remember { mutableStateOf("All") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf(2024) }

    // Admin dialog states
    var isAddingProject by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<DevelopmentProject?>(null) }
    var projectToDelete by remember { mutableStateOf<DevelopmentProject?>(null) }

    var isAddingReport by remember { mutableStateOf(false) }
    var editingReport by remember { mutableStateOf<GovtReportItem?>(null) }
    var reportToDelete by remember { mutableStateOf<GovtReportItem?>(null) }

    val moheshkhaliUnions = listOf("All", "মাতারবাড়ি", "ধলঘাটা", "কালারমারছড়া", "শাপলাপুর", "হোয়ানক", "বড় মহেশখালী", "ছোট মহেশখালী", "কুতুবজোম")
    val kutubdiaUnions = listOf("All", "বড়ঘোপ", "আলী আকবর ডেইল", "কৈয়ারবিল", "লেমশীখালী", "উত্তর ধুরুং", "দক্ষিণ ধুরুং")

    val activeCategories = remember(projects, selectedUpazila, categories) {
        if (selectedUpazila == null) {
            emptyList()
        } else {
            buildCategoriesForUpazila(projects, selectedUpazila!!, categories)
        }
    }
    val selectedCategory = remember(activeCategories, selectedCategoryId) {
        activeCategories.find { it.id == selectedCategoryId }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 12.dp)
    ) {
        // 1. Hero Header
        item {
            DevelopmentHeroHeader(language)
        }

        // 2. Step 1: Upazila Selection (Moheshkhali / Kutubdia) & Optional Union Filter
        item {
            AreaSelectors(
                language = language,
                selectedUpazila = selectedUpazila,
                onUpazilaChange = { newUpazila ->
                    selectedUpazila = newUpazila
                    selectedUnion = "All"
                    selectedCategoryId = null
                },
                selectedUnion = selectedUnion,
                onUnionChange = { selectedUnion = it },
                moheshkhaliUnions = moheshkhaliUnions,
                kutubdiaUnions = kutubdiaUnions
            )
        }

        if (selectedUpazila == null) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("select_upazila_prompt_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = RoyalBlue800,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (language == AppLanguage.BN)
                                "প্রথমে মহেশখালী অথবা কুতুবদিয়া উপজেলা নির্বাচন করুন"
                            else
                                "Please select Moheshkhali or Kutubdia first",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue900
                        )
                        Text(
                            text = if (language == AppLanguage.BN)
                                "উপজেলা নির্বাচন করার পর ১১টি উন্নয়ন ক্যাটাগরি এবং সেই উপজেলার প্রকল্পসমূহ দেখা যাবে।"
                            else
                                "After choosing an upazila, the 11 development categories and its projects will appear.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        if (isAdminAuthenticated) {
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { isAddingProject = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = ForestGreen,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("admin_add_project_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == AppLanguage.BN) "নতুন প্রকল্প যোগ" else "Add Project",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 3. Step 2: 11 Development Categories in Compact Grid
            item {
                SectorGrid(
                    categories = activeCategories,
                    selectedCategoryId = selectedCategoryId,
                    onCategoryClick = { catId ->
                        selectedCategoryId = if (selectedCategoryId == catId) null else catId
                    },
                    language = language
                )
            }

            // 4. Projects Section Header (with Admin Add Button)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val headerText = if (selectedCategory != null) {
                        val catTitle = if (language == AppLanguage.BN) selectedCategory.labelBn else selectedCategory.labelEn
                        val upzTitle = if (selectedUpazila == "Moheshkhali") {
                            if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali"
                        } else {
                            if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia"
                        }
                        "$upzTitle • $catTitle"
                    } else {
                        if (language == AppLanguage.BN) "উন্নয়ন প্রকল্পসমূহ" else "Development Projects"
                    }
                    Text(
                        text = headerText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBlue900,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isAdminAuthenticated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { isAddingProject = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = ForestGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_add_project_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.BN) "নতুন প্রকল্প যোগ" else "Add Project", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedCategory == null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (language == AppLanguage.BN)
                                    "সংশ্লিষ্ট প্রকল্প দেখতে উপরের তালিকা থেকে একটি উন্নয়ন ক্যাটাগরি নির্বাচন করুন"
                                else
                                    "Select a development category above to view matching projects",
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                // Filtered Projects List for selected Upazila & Category
                val filteredProjects = projects.filter { p ->
                    val matchesUpazila = projectMatchesUpazila(p, selectedUpazila!!)
                    val matchesUnion = (selectedUnion == "All" || normalizeDevelopmentText(p.union) == normalizeDevelopmentText(selectedUnion))
                    val matchesCat = selectedCategory.matches(p)
                    matchesUpazila && matchesUnion && matchesCat
                }

                if (filteredProjects.isNotEmpty()) {
                    items(filteredProjects, key = { it.id }) { project ->
                        ProjectCompactCard(
                            project = project,
                            language = language,
                            isAdmin = isAdminAuthenticated,
                            onEditClick = { editingProject = project },
                            onDeleteClick = { projectToDelete = project },
                            onClick = { onProjectDetailClick(project) }
                        )
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (language == AppLanguage.BN)
                                        "এই উপজেলা ও ক্যাটাগরিতে এখনো কোনো প্রকল্প পাওয়া যায়নি"
                                    else
                                        "No projects found for this upazila and category",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Historical Archive Section (2014-2024) & Govt Reports/Gazettes
        item {
            val archiveProjects = if (selectedUpazila != null) {
                projects.filter { it.year == selectedYear && projectMatchesUpazila(it, selectedUpazila!!) }
            } else {
                projects.filter { it.year == selectedYear }
            }
            HistoricalArchiveSection(
                language = language,
                selectedYear = selectedYear,
                onYearChange = { selectedYear = it },
                projects = archiveProjects,
                govtReports = govtReports.filter { it.year == selectedYear }.ifEmpty { govtReports },
                isAdmin = isAdminAuthenticated,
                onAddReportClick = { isAddingReport = true },
                onEditReportClick = { editingReport = it },
                onDeleteReportClick = { reportToDelete = it }
            )
        }
    }

    // --- Admin Dialogs ---
    if (isAddingProject || editingProject != null) {
        ProjectEditDialog(
            project = editingProject,
            isNew = isAddingProject,
            language = language,
            onSave = {
                onSaveProject(it)
                isAddingProject = false
                editingProject = null
            },
            onDismiss = {
                isAddingProject = false
                editingProject = null
            }
        )
    }

    if (isAddingReport || editingReport != null) {
        GovtReportEditDialog(
            report = editingReport,
            isNew = isAddingReport,
            language = language,
            onSave = {
                onSaveReport(it)
                isAddingReport = false
                editingReport = null
            },
            onDismiss = {
                isAddingReport = false
                editingReport = null
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text(if (language == AppLanguage.BN) "প্রকল্প মুছে ফেলতে চান?" else "Delete Project?", fontWeight = FontWeight.Bold) },
            text = { Text(if (language == AppLanguage.BN) "আপনি কি নিশ্চিত যে '${projectToDelete?.titleBn}' প্রকল্পটি ক্লাউড ডাটাবেজ থেকে মুছে ফেলতে চান?" else "Are you sure you want to delete this project permanently?") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it.id) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == AppLanguage.BN) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { projectToDelete = null }) {
                    Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (reportToDelete != null) {
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text(if (language == AppLanguage.BN) "গেজেট / প্রতিবেদন মুছে ফেলুন?" else "Delete Report / Gazette?", fontWeight = FontWeight.Bold) },
            text = { Text(if (language == AppLanguage.BN) "আপনি কি নিশ্চিত যে '${reportToDelete?.titleBn}' গেজেটটি ক্লাউড থেকে মুছে ফেলতে চান?" else "Are you sure you want to delete this gazette report permanently?") },
            confirmButton = {
                Button(
                    onClick = {
                        reportToDelete?.let { onDeleteReport(it.id) }
                        reportToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (language == AppLanguage.BN) "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportToDelete = null }) {
                    Text(if (language == AppLanguage.BN) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun DevelopmentHeroHeader(language: AppLanguage) {
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        AsyncOrLocalImage("drawable:moheshkhali_banner", null, Modifier.fillMaxSize(), ContentScale.Crop)
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color(0xFF006A4E).copy(0.8f)))))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(
                text = if (language == AppLanguage.BN) "উন্নয়ন ও অগ্রযাত্রা" else "Development & Progress",
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (language == AppLanguage.BN) "মহেশখালী-কুতুবদিয়ার মেগা প্রকল্পের ডিজিটাল ভাণ্ডার" else "Digital archive of mega projects in Moheshkhali-Kutubdia",
                color = Color.White.copy(0.9f), fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AreaSelectors(
    language: AppLanguage,
    selectedUpazila: String?,
    onUpazilaChange: (String) -> Unit,
    selectedUnion: String,
    onUnionChange: (String) -> Unit,
    moheshkhaliUnions: List<String>,
    kutubdiaUnions: List<String>
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = if (language == AppLanguage.BN) "১. উপজেলা নির্বাচন করুন:" else "1. Select Upazila:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalBlue900
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            UpazilaChip(
                id = "Moheshkhali",
                label = if (language == AppLanguage.BN) "মহেশখালী" else "Moheshkhali",
                isSelected = selectedUpazila == "Moheshkhali",
                modifier = Modifier.weight(1f).testTag("upazila_selector_moheshkhali")
            ) { onUpazilaChange("Moheshkhali") }
            UpazilaChip(
                id = "Kutubdia",
                label = if (language == AppLanguage.BN) "কুতুবদিয়া" else "Kutubdia",
                isSelected = selectedUpazila == "Kutubdia",
                modifier = Modifier.weight(1f).testTag("upazila_selector_kutubdia")
            ) { onUpazilaChange("Kutubdia") }
        }

        if (selectedUpazila != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (language == AppLanguage.BN) "ইউনিয়ন ভিত্তিক বাছাই:" else "Filter by Union:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                val unions = if (selectedUpazila == "Moheshkhali") moheshkhaliUnions else kutubdiaUnions
                items(unions) { union ->
                    FilterChip(
                        selected = selectedUnion == union,
                        onClick = { onUnionChange(union) },
                        label = {
                            Text(
                                text = if (union == "All") (if (language == AppLanguage.BN) "সব ইউনিয়ন" else "All Unions") else union,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ForestGreen, selectedLabelColor = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpazilaChip(
    id: String,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) RoyalBlue800 else Color.White,
        border = BorderStroke(1.dp, if (isSelected) RoyalBlue800 else Color(0xFFCBD5E1)),
        modifier = modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(label, color = if (isSelected) Color.White else Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectorGrid(
    categories: List<DevelopmentCategory>,
    selectedCategoryId: String?,
    onCategoryClick: (String) -> Unit,
    language: AppLanguage
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = if (language == AppLanguage.BN) "২. উন্নয়ন ক্যাটাগরি নির্বাচন করুন:" else "2. Select Development Category:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RoyalBlue900
        )
        Spacer(modifier = Modifier.height(8.dp))

        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    SectorCard(
                        info = category,
                        isSelected = selectedCategoryId == category.id,
                        language = language,
                        onClick = { onCategoryClick(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectorCard(
    info: DevelopmentCategory,
    isSelected: Boolean,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) ForestGreen else Color.White),
        border = BorderStroke(1.dp, if (isSelected) ForestGreen else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
        modifier = modifier.height(54.dp).testTag("dev_category_${info.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(0.22f) else Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else ForestGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val displayLabel = if (language == AppLanguage.BN) info.labelBn else info.labelEn
            Text(
                text = displayLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF334155),
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProjectCompactCard(
    project: DevelopmentProject,
    language: AppLanguage,
    isAdmin: Boolean = false,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncOrLocalImage(project.imageUrl, null, Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), ContentScale.Crop)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BN) project.titleBn else project.titleEn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${project.completionPercentage}% সম্পন্ন • ${project.status}",
                        fontSize = 11.sp,
                        color = ForestGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (project.budgetBn.isNotBlank()) {
                        Text(
                            text = " • ${project.budgetBn}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Admin Actions (Edit ✏️ and Delete 🗑️)
            if (isAdmin) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp).testTag("edit_project_${project.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ForestGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp).testTag("delete_project_${project.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                Icon(Icons.Default.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun HistoricalArchiveSection(
    language: AppLanguage,
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    projects: List<DevelopmentProject>,
    govtReports: List<GovtReportItem> = emptyList(),
    isAdmin: Boolean = false,
    onAddReportClick: () -> Unit = {},
    onEditReportClick: (GovtReportItem) -> Unit = {},
    onDeleteReportClick: (GovtReportItem) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = RoyalBlue800)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (language == AppLanguage.BN) "উন্নয়ন ইতিহাস ও আর্কাইভ (২০১৪–২০২৪)" else "Development History Archive (2014-2024)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBlue900)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (language == AppLanguage.BN) "সাল ভিত্তিক অগ্রগতির চিত্র:" else "Timeline Progress:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 10.dp)) {
            items((2014..2024).toList().reversed()) { year ->
                FilterChip(
                    selected = selectedYear == year,
                    onClick = { onYearChange(year) },
                    label = { Text(year.toString(), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue800, selectedLabelColor = Color.White)
                )
            }
        }

        if (projects.any { it.beforeImageUrl.isNotBlank() && it.afterImageUrl.isNotBlank() }) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(if (language == AppLanguage.BN) "পার্থক্য দেখুন (আগে ও পরে):" else "Visual Comparison (Before & After):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val compProject = projects.first { it.beforeImageUrl.isNotBlank() && it.afterImageUrl.isNotBlank() }
            BeforeAfterSlider(compProject)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Government Project Reports & Gazettes ("সরকারী প্রকল্প প্রতিবেদন ও গেজেট") ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BN) "সরকারি প্রকল্প প্রতিবেদন ও গেজেট" else "Govt Project Reports & Gazettes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue900
                            )
                            Text(
                                text = if (language == AppLanguage.BN) "অফিসিয়াল গেজেট ও মূল্যায়ন সমীক্ষা" else "Official Gazettes & Evaluation Documents",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (isAdmin) {
                        FilledTonalButton(
                            onClick = onAddReportClick,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = RoyalBlue800,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_add_report_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == AppLanguage.BN) "গেজেট যোগ" else "Add Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (govtReports.isNotEmpty()) {
                    govtReports.forEach { report ->
                        GovtReportCard(
                            report = report,
                            language = language,
                            isAdmin = isAdmin,
                            onEditClick = { onEditReportClick(report) },
                            onDeleteClick = { onDeleteReportClick(report) },
                            onOpenClick = {
                                val url = report.pdfUrl.ifEmpty { report.mediaUrl }
                                MediaStorageManager.openDocumentOrMedia(context, url)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Also list any projects from this year that have pdfUrl
                val pdfProjects = projects.filter { it.pdfUrl.isNotBlank() }
                if (pdfProjects.isNotEmpty() && govtReports.isEmpty()) {
                    pdfProjects.forEach { p ->
                        PdfItem(p, language)
                    }
                } else if (govtReports.isEmpty() && pdfProjects.isEmpty()) {
                    Text(
                        text = if (language == AppLanguage.BN) "এই সালের কোনো গেজেট বা রিপোর্ট পাওয়া যায়নি।" else "No reports found for this year.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GovtReportCard(
    report: GovtReportItem,
    language: AppLanguage,
    isAdmin: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().testTag("report_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = if (language == AppLanguage.BN) report.categoryBn else report.categoryEn,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (report.fileSizeMb.isNotBlank()) {
                    Text(report.fileSizeMb, fontSize = 10.sp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (language == AppLanguage.BN) report.titleBn else report.titleEn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${report.ministryOrDeptBn} • ${report.publishDateBn}",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onOpenClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.FileDownload, null, tint = RoyalBlue800, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == AppLanguage.BN) "ডকুমেন্ট দেখুন" else "View / Download", fontSize = 11.sp, color = RoyalBlue900)
                }

                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp).testTag("edit_report_${report.id}")) {
                            Icon(Icons.Default.Edit, null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp).testTag("delete_report_${report.id}")) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeforeAfterSlider(project: DevelopmentProject) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Gray)
    ) {
        val dividerOffset = maxWidth * sliderPosition

        AsyncOrLocalImage(project.afterImageUrl, null, Modifier.fillMaxSize(), ContentScale.Crop)

        Box(modifier = Modifier.fillMaxWidth(sliderPosition).fillMaxHeight()) {
            AsyncOrLocalImage(project.beforeImageUrl, null, Modifier.fillMaxSize(), ContentScale.Crop)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White)
                .align(Alignment.CenterStart)
                .offset(x = dividerOffset)
        )

        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).fillMaxWidth(0.9f),
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent)
        )

        Surface(color = Color.Black.copy(0.6f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(8.dp).align(Alignment.TopStart)) {
            Text("আগে (Before)", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Surface(color = Color.Black.copy(0.6f), shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)) {
            Text("পরে (After)", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun PdfItem(project: DevelopmentProject, language: AppLanguage) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { 
            MediaStorageManager.openDocumentOrMedia(context, project.pdfUrl)
        }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FileDownload, null, tint = RoyalBlue800, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (language == AppLanguage.BN) "${project.titleBn} - বিস্তারিত রিপোর্ট" else "${project.titleEn} - Full Report", fontSize = 12.sp, color = RoyalBlue900, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
    }
}
