package com.tnfisheries.ornamentalfish

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

internal enum class NativeSection(val key: String, val en: String, val ta: String) {
    HOME("home", "Home", "முகப்பு"),
    ATLAS("atlas", "Atlas", "இனங்கள்"),
    LEARN("learn", "Learn", "பாடம்"),
    QUIZ("quiz", "Quiz", "வினா"),
    TOOLS("tools", "Tools", "கருவிகள்")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrnamentalFishNativeApp(
    contentResult: Result<NativeContent>,
    speak: (String, Boolean) -> Unit,
    share: (String) -> Unit,
    exportBookmarks: (String) -> Unit,
    scheduleReminder: (Int, String, String) -> Unit
) {
    MaterialTheme {
        val content = contentResult.getOrNull()
        if (content == null) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp)) {
                    Text("Native content could not be loaded", style = MaterialTheme.typography.headlineSmall)
                    Text(contentResult.exceptionOrNull()?.message ?: "Required local data are missing.")
                }
            }
            return@MaterialTheme
        }

        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("of_native_state", Context.MODE_PRIVATE) }
        var tamil by remember { mutableStateOf(prefs.getBoolean("tamil", false)) }
        var section by rememberSaveable { mutableStateOf(NativeSection.HOME) }
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        var privacy by rememberSaveable { mutableStateOf(false) }
        var bookmarks by remember {
            mutableStateOf(prefs.getStringSet("bookmarks", emptySet()).orEmpty().toSet())
        }

        BackHandler(selectedId != null || privacy || section != NativeSection.HOME) {
            when {
                selectedId != null -> selectedId = null
                privacy -> privacy = false
                else -> section = NativeSection.HOME
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(if (tamil) "அலங்கார மீன் அறிவியல்" else "Ornamental Fish Science")
                            Text(if (tamil) "Android Native · v3" else "Android Native · v3", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            tamil = !tamil
                            prefs.edit().putBoolean("tamil", tamil).apply()
                        }) { Text(if (tamil) "EN" else "தமிழ்") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NativeSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item && !privacy,
                            onClick = { section = item; selectedId = null; privacy = false },
                            icon = { Text(item.en.take(1)) },
                            label = { Text(if (tamil) item.ta else item.en) }
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                if (privacy) {
                    NativePrivacyScreen(tamil)
                } else {
                    when (section) {
                        NativeSection.HOME -> NativeHomeScreen(
                            content = content,
                            tamil = tamil,
                            bookmarkCount = bookmarks.size,
                            onAtlas = { section = NativeSection.ATLAS },
                            onLearn = { section = NativeSection.LEARN },
                            onQuiz = { section = NativeSection.QUIZ },
                            onTools = { section = NativeSection.TOOLS },
                            onPrivacy = { privacy = true }
                        )
                        NativeSection.ATLAS -> NativeAtlasScreen(
                            species = content.species,
                            selectedId = selectedId,
                            tamil = tamil,
                            bookmarks = bookmarks,
                            onSelect = { selectedId = it },
                            onBack = { selectedId = null },
                            onBookmark = { id ->
                                bookmarks = if (id in bookmarks) bookmarks - id else bookmarks + id
                                prefs.edit().putStringSet("bookmarks", bookmarks).apply()
                            },
                            speak = speak,
                            share = share,
                            exportBookmarks = exportBookmarks
                        )
                        NativeSection.LEARN -> NativeLearnScreen(content.book, content.labs, tamil, prefs)
                        NativeSection.QUIZ -> NativeQuizScreen(content.quiz, tamil, prefs, scheduleReminder)
                        NativeSection.TOOLS -> NativeToolsScreen(tamil)
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeHomeScreen(
    content: NativeContent,
    tamil: Boolean,
    bookmarkCount: Int,
    onAtlas: () -> Unit,
    onLearn: () -> Unit,
    onQuiz: () -> Unit,
    onTools: () -> Unit,
    onPrivacy: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (tamil) "அலங்கார மீன்களை அறிக" else "Explore ornamental fishes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text((if (tamil) "இனங்கள்: " else "Species: ") + content.species.size + " · " + (if (tamil) "இணையமின்றி இயங்கும்" else "offline-first"))
        }
        item { NativeActionCard(if (tamil) "இன விவரணம்" else "Species atlas", if (tamil) "தேடல், வடிகட்டி, படங்கள், பராமரிப்பு." else "Search, filters, images and husbandry.", onAtlas) }
        item { NativeActionCard(if (tamil) "பாடங்கள்" else "Learning", (if (tamil) "பாடங்கள் " else "Lessons ") + content.book.size + " · " + (if (tamil) "செய்முறைகள் " else "labs ") + content.labs.size, onLearn) }
        item { NativeActionCard(if (tamil) "வினாடி வினா" else "Quiz", (if (tamil) "வினாக்கள் " else "Questions ") + content.quiz.size, onQuiz) }
        item { NativeActionCard(if (tamil) "கணக்கீட்டு கருவிகள்" else "Tools", if (tamil) "கொள்ளளவு, அமோனியா, நீர் மாற்றம், பொருளாதாரம்." else "Volume, ammonia, water change and economics.", onTools) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((if (tamil) "சேமித்த இனங்கள்: " else "Bookmarks: ") + bookmarkCount)
                    OutlinedButton(onClick = onPrivacy) { Text(if (tamil) "தனியுரிமைக் கொள்கை" else "Privacy policy") }
                }
            }
        }
    }
}

@Composable
private fun NativeActionCard(title: String, body: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}

private data class NativeFilter(val id: String, val en: String, val ta: String)

private val nativeFilters = listOf(
    NativeFilter("all", "All", "அனைத்தும்"),
    NativeFilter("indigenous", "Indian native", "இந்தியத் தாயகம்"),
    NativeFilter("exotic", "Exotic", "அயல்நாட்டு"),
    NativeFilter("freshwater", "Freshwater", "நன்னீர்"),
    NativeFilter("brackish", "Brackish", "உவர்நீர்"),
    NativeFilter("marine", "Marine", "கடல்"),
    NativeFilter("livebearer", "Livebearer", "உயிருடன் ஈனும்"),
    NativeFilter("egg-layer", "Egg-layer", "முட்டையிடும்"),
    NativeFilter("peaceful", "Peaceful", "அமைதியான"),
    NativeFilter("active", "Active / territorial", "சுறுசுறுப்பு / எல்லை காக்கும்"),
    NativeFilter("conservation", "Conservation", "பாதுகாப்பு")
)

@Composable
private fun NativeAtlasScreen(
    species: List<Species>,
    selectedId: String?,
    tamil: Boolean,
    bookmarks: Set<String>,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    onBookmark: (String) -> Unit,
    speak: (String, Boolean) -> Unit,
    share: (String) -> Unit,
    exportBookmarks: (String) -> Unit
) {
    val selected = selectedId?.let { id -> species.firstOrNull { it.id == id } }
    if (selected != null) {
        NativeSpeciesDetail(selected, tamil, selected.id in bookmarks, onBack, onBookmark, speak, share)
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val visible = remember(species, query, filter) {
        species.filter { NativeSpeciesMatcher.matches(it, query, filter) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (tamil) "இன விவரணம்" else "Species atlas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (tamil) "பெயர், குடும்பம், பரவல்" else "Search name, family or region") },
                singleLine = true
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(nativeFilters) { f ->
                    FilterChip(
                        selected = filter == f.id,
                        onClick = { filter = f.id },
                        label = { Text(if (tamil) f.ta else f.en) }
                    )
                }
            }
            Text(visible.size.toString() + " / " + species.size)
            if (bookmarks.isNotEmpty()) {
                OutlinedButton(onClick = {
                    val rows = species.filter { it.id in bookmarks }.map { (if (tamil) it.ta else it.en) + " — " + it.sci.substringBefore("(").trim() }
                    exportBookmarks((listOf("Ornamental Fish Science — native bookmarks", "") + rows).joinToString("\n"))
                }) { Text(if (tamil) "சேமிப்புகளை ஏற்றுமதி செய்" else "Export bookmarks") }
            }
        }
        items(visible, key = { it.id }) { fish ->
            Card(onClick = { onSelect(fish.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NativeAssetImage(fish.image, Modifier.height(90.dp).fillMaxWidth(0.30f))
                    Column {
                        Text(if (tamil) fish.ta else fish.en, fontWeight = FontWeight.Bold)
                        Text(fish.sci.substringBefore("(").trim(), style = MaterialTheme.typography.bodySmall)
                        Text(fish.family, style = MaterialTheme.typography.labelSmall)
                        if (fish.id in bookmarks) Text("★")
                    }
                }
            }
        }
    }
}

private object NativeSpeciesMatcher {
    fun matches(s: Species, query: String, filter: String): Boolean {
        when (filter) {
            "indigenous" -> if (s.origin != "indigenous") return false
            "exotic" -> if (s.origin != "exotic") return false
            "freshwater" -> if (s.habitat != "freshwater") return false
            "brackish" -> if (s.habitat != "brackish") return false
            "marine" -> if (s.habitat != "marine") return false
            "livebearer" -> if (s.breedType != "livebearer") return false
            "egg-layer" -> if (s.breedType != "egg-layer") return false
            "peaceful" -> if (!s.temperClass.lowercase(Locale.ROOT).contains("peace")) return false
            "active" -> {
                val t = s.temperClass.lowercase(Locale.ROOT)
                if (!t.contains("active") && !t.contains("territ")) return false
            }
            "conservation" -> {
                val i = s.value("iucn").lowercase(Locale.ROOT)
                if (i.isBlank() || i.contains("least concern")) return false
            }
        }
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isBlank()) return true
        return listOf(s.en, s.ta, s.sci, s.family, s.order, s.value("native"), s.value("ta_native"), s.value("status_in"))
            .joinToString(" ").lowercase(Locale.ROOT).contains(q)
    }
}

@Composable
private fun NativeSpeciesDetail(
    fish: Species,
    tamil: Boolean,
    bookmarked: Boolean,
    onBack: () -> Unit,
    onBookmark: (String) -> Unit,
    speak: (String, Boolean) -> Unit,
    share: (String) -> Unit
) {
    val name = if (tamil) fish.ta else fish.en
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text(if (tamil) "← இனங்கள்" else "← Atlas") }
            OutlinedButton(onClick = { onBookmark(fish.id) }) { Text(if (bookmarked) "★" else "☆") }
            OutlinedButton(onClick = { speak(name, tamil) }) { Text("🔊") }
        }
        Button(onClick = { share(nativeShareText(fish, tamil)) }) { Text(if (tamil) "பகிர்" else "Share") }
        NativeAssetImage(fish.image, Modifier.fillMaxWidth().height(230.dp))
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(fish.sci.substringBefore("(").trim(), style = MaterialTheme.typography.bodyLarge)
        NativeDetailBlock(if (tamil) "வகைப்பாட்டியல்" else "Taxonomy", listOf(
            (if (tamil) "குடும்பம்" else "Family") to fish.family,
            (if (tamil) "வரிசை" else "Order") to fish.order
        ))
        NativeDetailBlock(if (tamil) "பரவலும் வாழிடமும்" else "Distribution & habitat", listOf(
            (if (tamil) "தாயகப் பரவல்" else "Native range") to fish.bilingual("native", "ta_native", tamil),
            (if (tamil) "இந்திய நிலை" else "Status in India") to fish.bilingual("status_in", "ta_status_in", tamil),
            (if (tamil) "வாழிடம்" else "Habitat") to fish.habitat
        ))
        NativeDetailBlock(if (tamil) "பராமரிப்பு" else "Husbandry", listOf(
            (if (tamil) "குறைந்த தொட்டி" else "Minimum tank") to fish.bilingual("tank_min_l", "ta_tank_min_l", tamil),
            (if (tamil) "வெப்பநிலை" else "Temperature") to fish.bilingual("temp_hold_c", "ta_temp_hold_c", tamil),
            "pH" to fish.bilingual("ph", "ta_ph", tamil),
            (if (tamil) "கடினத்தன்மை" else "Hardness") to fish.bilingual("gh_kh", "ta_gh_kh", tamil),
            (if (tamil) "உணவு" else "Diet") to fish.bilingual("diet", "ta_diet", tamil),
            (if (tamil) "இனப்பெருக்கம்" else "Breeding") to fish.bilingual("breed", "ta_breed", tamil)
        ))
        NativeDetailBlock(if (tamil) "நலம் மற்றும் பாதுகாப்பு" else "Health & conservation", listOf(
            (if (tamil) "நோய் குறிப்பு" else "Health notes") to fish.bilingual("disease", "ta_disease", tamil),
            (if (tamil) "தனிமைப்படுத்தல்" else "Quarantine") to fish.bilingual("quarantine", "ta_quarantine", tamil),
            (if (tamil) "IUCN / பாதுகாப்பு" else "IUCN / conservation") to fish.bilingual("iucn", "ta_iucn", tamil)
        ))
    }
}

private fun nativeShareText(fish: Species, tamil: Boolean): String {
    return (if (tamil) fish.ta else fish.en) + " (" + fish.sci.substringBefore("(").trim() + ")\n" +
        (if (tamil) "குடும்பம்: " else "Family: ") + fish.family + "\n" +
        (if (tamil) "வாழிடம்: " else "Habitat: ") + fish.habitat + "\n" +
        (if (tamil) "உணவு: " else "Diet: ") + fish.bilingual("diet", "ta_diet", tamil) + "\n" +
        "Ornamental Fish Science — native v3"
}

@Composable
internal fun NativeDetailBlock(title: String, rows: List<Pair<String, String>>) {
    val visible = rows.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            visible.forEach { row ->
                Text(row.first, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(row.second)
            }
        }
    }
}

@Composable
private fun NativeAssetImage(name: String, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        if (name.isBlank()) null else runCatching {
            context.assets.open("fish_atlas/" + name).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Surface(modifier = modifier, shape = RoundedCornerShape(10.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("No image") }
        }
    }
}

@Composable
private fun NativePrivacyScreen(tamil: Boolean) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(if (tamil) "தனியுரிமைக் கொள்கை" else "Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(if (tamil) "செயலில் உள்ள தேதி: 22 செப்டம்பர் 2026" else "Effective date: 22 September 2026")
        Text(if (tamil) "இச்செயலி இணையம் இன்றியும் இயங்கும் கல்விச் செயலி. பயனர் கணக்கு தேவையில்லை; தனிப்பட்ட தகவலைச் சேகரிக்க, அனுப்ப, விற்க அல்லது பகிர வடிவமைக்கப்படவில்லை." else "Ornamental Fish Science is an offline-first educational application. It does not require a user account and is not designed to collect, transmit, sell or share personal information.")
        Text(if (tamil) "மொழி, புத்தகக்குறி, வினா முன்னேற்றம் மற்றும் வாசிப்பு நிலை சாதனத்தில் மட்டும் சேமிக்கப்படலாம்." else "Language, bookmarks, quiz progress and reading state may be stored locally on the device.")
        Text(if (tamil) "மூன்றாம் தரப்பு விளம்பரம், பகுப்பாய்வு SDK அல்லது குறுக்கு-செயலி கண்காணிப்பு இல்லை." else "No third-party advertising, analytics SDK or cross-app tracking is designed into the app.")
        Button(onClick = {
            val uri = Uri.parse("https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }) { Text(if (tamil) "பொது HTTPS கொள்கையைத் திற" else "Open public HTTPS policy") }
        Text("Contact: rameshgascngl@gmail.com\nDeveloper: Rajamoni Ramesh\nDepartment of Zoology\nGovernment Arts and Science College, Nagercoil, Tamil Nadu, India")
    }
}
