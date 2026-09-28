package com.tnfisheries.ornamentalfish

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

internal enum class NativeSection(
    val key: String,
    val en: String,
    val ta: String,
    val iconRes: Int
) {
    HOME("home", "Home", "முகப்பு", R.drawable.nav_home),
    ATLAS("atlas", "Atlas", "இனங்கள்", R.drawable.nav_atlas),
    LEARN("learn", "Learn", "பாடங்கள்", R.drawable.nav_learn),
    QUIZ("quiz", "Quiz", "வினா", R.drawable.nav_quiz),
    TOOLS("tools", "Tools", "கருவிகள்", R.drawable.nav_tools)
}

private val AquaScheme = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F0),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    secondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFF4F607C),
    tertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFFF6FAF9),
    surface = Color(0xFFF6FAF9),
    surfaceVariant = Color(0xFFDCE5E4),
    outline = Color(0xFF6F7979)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrnamentalFishNativeApp(
    contentResult: Result<NativeContent>,
    speak: (String, Boolean) -> Unit,
    share: (String) -> Unit,
    exportBookmarks: (String) -> Unit,
    scheduleReminder: (Int, String, String) -> Unit
) {
    MaterialTheme(colorScheme = AquaScheme) {
        val content = contentResult.getOrNull()
        if (content == null) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Content could not be loaded", style = MaterialTheme.typography.headlineSmall)
                    Text(contentResult.exceptionOrNull()?.message ?: "Required local data are missing.")
                }
            }
            return@MaterialTheme
        }

        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("of_native_state", Context.MODE_PRIVATE) }
        var tamil by remember { mutableStateOf(prefs.getBoolean("tamil", false)) }
        var section by rememberSaveable { mutableStateOf(NativeSection.HOME) }
        var previousSectionKey by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
        var privacy by rememberSaveable { mutableStateOf(false) }
        var bookmarks by remember {
            mutableStateOf(prefs.getStringSet("bookmarks", emptySet()).orEmpty().toSet())
        }

        BackHandler(selectedId != null || privacy || section != NativeSection.HOME) {
            when {
                selectedId != null -> selectedId = null
                privacy -> privacy = false
                previousSectionKey != null -> {
                    section = NativeSection.entries.firstOrNull { it.key == previousSectionKey } ?: NativeSection.HOME
                    previousSectionKey = null
                }
                else -> section = NativeSection.HOME
            }
        }

        fun navigateTo(target: NativeSection) {
            if (target != section) previousSectionKey = section.key
            section = target
            selectedId = null
            privacy = false
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (tamil) "அலங்கார மீன் அறிவியல்" else "Ornamental Fish Science",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        TextButton(onClick = {
                            tamil = !tamil
                            prefs.edit().putBoolean("tamil", tamil).apply()
                        }) {
                            Text(if (tamil) "English" else "தமிழ்", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NativeSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item && !privacy,
                            onClick = { navigateTo(item) },
                            icon = {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(if (tamil) item.ta else item.en, fontSize = 12.sp) }
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
                            onAtlas = { navigateTo(NativeSection.ATLAS) },
                            onLearn = { navigateTo(NativeSection.LEARN) },
                            onQuiz = { navigateTo(NativeSection.QUIZ) },
                            onTools = { navigateTo(NativeSection.TOOLS) },
                            onPrivacy = { previousSectionKey = section.key; privacy = true }
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
    val hero = content.species.firstOrNull { it.id == "oranda" } ?: content.species.firstOrNull()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column {
                    hero?.let {
                        NativeAssetImage(
                            it.image,
                            Modifier.fillMaxWidth().height(150.dp),
                            fit = true,
                            description = if (tamil) it.ta else it.en
                        )
                    }
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (tamil) "அலங்கார மீன்களின் உலகை அறிக" else "Explore the science of ornamental fishes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tamil)
                                "இன அடையாளம், பராமரிப்பு, இனப்பெருக்கம், நீர்தரம் மற்றும் செய்முறைப் பயிற்சி."
                            else
                                "Species identification, husbandry, breeding, water quality and practical learning."
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NativeStatCard(content.species.size.toString(), if (tamil) "இனங்கள்" else "Species", Modifier.weight(1f))
                NativeStatCard(content.book.size.toString(), if (tamil) "பாடங்கள்" else "Lessons", Modifier.weight(1f))
                NativeStatCard(content.quiz.size.toString(), if (tamil) "வினாக்கள்" else "Questions", Modifier.weight(1f))
            }
        }

        item {
            NativeActionCard(
                R.drawable.nav_atlas,
                if (tamil) "இன விவரணம்" else "Species Atlas",
                if (tamil) "22 இனங்கள் · படங்கள் · தேடல் · நீர்தர மற்றும் பராமரிப்பு விவரங்கள்"
                else "22 species · images · search · water and husbandry profiles",
                onAtlas
            )
        }
        item {
            NativeActionCard(
                R.drawable.nav_learn,
                if (tamil) "பாடங்களும் செய்முறைகளும்" else "Lessons & Practicals",
                if (tamil)
                    content.book.size.toString() + " பாடங்கள் · " + content.labs.size + " செய்முறைப் பயிற்சிகள்"
                else
                    content.book.size.toString() + " lessons · " + content.labs.size + " practical activities",
                onLearn
            )
        }
        item {
            NativeActionCard(
                R.drawable.nav_quiz,
                if (tamil) "சுயமதிப்பீட்டு வினா" else "Self-assessment Quiz",
                if (tamil)
                    content.quiz.size.toString() + " வினாக்கள் · விளக்கங்களுடன் உடனடி பின்னூட்டம்"
                else
                    content.quiz.size.toString() + " questions · immediate feedback with explanations",
                onQuiz
            )
        }
        item {
            NativeActionCard(
                R.drawable.nav_tools,
                if (tamil) "அறிவியல் கணக்கீட்டு கருவிகள்" else "Scientific Calculators",
                if (tamil) "தொட்டி நீரளவு · அமோனியா · நீர் மாற்றம் · பண்ணைப் பொருளாதாரம்"
                else "Tank volume · ammonia · water change · farm economics",
                onTools
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text((if (tamil) "சேமித்த இனங்கள்: " else "Saved species: ") + bookmarkCount)
                TextButton(onClick = onPrivacy) {
                    Text(if (tamil) "பயன்பாடு & தனியுரிமை" else "About & Privacy")
                }
            }
        }
    }
}

@Composable
private fun NativeStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun NativeActionCard(icon: Int, title: String, body: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
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
    NativeFilter("livebearer", "Livebearer", "குஞ்சுகளை ஈனும்"),
    NativeFilter("egg-layer", "Egg-layer", "முட்டையிடும்"),
    NativeFilter("peaceful", "Peaceful", "அமைதியான"),
    NativeFilter("active", "Active / territorial", "சுறுசுறுப்பு / எல்லை காக்கும்"),
    NativeFilter("conservation", "Conservation", "பாதுகாப்பு")
)

@OptIn(ExperimentalLayoutApi::class)
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
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    val selected = selectedId?.let { id -> species.firstOrNull { it.id == id } }
    if (selected != null) {
        NativeSpeciesDetail(selected, tamil, selected.id in bookmarks, onBack, onBookmark, speak, share)
        return
    }
    val visible = remember(species, query, filter) {
        species.filter { NativeSpeciesMatcher.matches(it, query, filter) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        if (tamil) "இன விவரணம்" else "Species Atlas",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        (if (tamil) visible.size.toString() + " / " + species.size + " இனங்கள்"
                        else visible.size.toString() + " / " + species.size + " species"),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (bookmarks.isNotEmpty()) {
                    TextButton(onClick = {
                        val rows = species.filter { it.id in bookmarks }.map {
                            (if (tamil) it.ta else it.en) + " — " + it.sci.substringBefore("(").trim()
                        }
                        exportBookmarks((listOf("Ornamental Fish Science — bookmarks", "") + rows).joinToString("\n"))
                    }) {
                        Text(if (tamil) "ஏற்றுமதி" else "Export saved")
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (tamil) "பெயர், அறிவியல் பெயர், குடும்பம் அல்லது பரவல்"
                        else "Search name, scientific name, family or region"
                    )
                },
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))
            Text(
                if (tamil) "வடிகட்டிகள்" else "Filters",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                nativeFilters.forEach { f ->
                    FilterChip(
                        selected = filter == f.id,
                        onClick = { filter = f.id },
                        label = { Text(if (tamil) f.ta else f.en, fontSize = 12.sp) }
                    )
                }
            }
        }

        items(visible, key = { it.id }) { fish ->
            Card(onClick = { onSelect(fish.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NativeAssetImage(
                        fish.image,
                        Modifier.size(104.dp),
                        fit = true,
                        description = if (tamil) fish.ta else fish.en
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (tamil) fish.ta else fish.en,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (fish.id in bookmarks) Text("★", color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            fish.sci.substringBefore("(").trim(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic)
                        )
                        Text(
                            fish.family,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
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
        return listOf(
            s.en, s.ta, s.sci, s.family, s.order,
            s.value("native"), s.value("ta_native"), s.value("status_in")
        ).joinToString(" ").lowercase(Locale.ROOT).contains(q)
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text(if (tamil) "← இனங்கள்" else "← Atlas") }
            OutlinedButton(onClick = { onBookmark(fish.id) }) {
                Text(
                    if (bookmarked)
                        (if (tamil) "★ சேமிக்கப்பட்டது" else "★ Saved")
                    else
                        (if (tamil) "☆ சேமி" else "☆ Save")
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { speak(name, tamil) }) { Text(if (tamil) "கேட்க" else "Listen") }
            OutlinedButton(onClick = { share(nativeShareText(fish, tamil)) }) {
                Text(if (tamil) "பகிர்" else "Share")
            }
        }

        NativeAssetImage(
            fish.image,
            Modifier.fillMaxWidth().height(250.dp),
            fit = true,
            description = name
        )
        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            fish.sci.substringBefore("(").trim(),
            style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.primary
        )

        NativeDetailBlock(
            if (tamil) "வகைப்பாட்டியல் மற்றும் அடையாளம்" else "Taxonomy & identification",
            listOf(
                (if (tamil) "குடும்பம்" else "Family") to fish.family,
                (if (tamil) "வரிசை" else "Order") to fish.order,
                (if (tamil) "அளவு" else "Adult size") to fish.bilingual("size_cm", "ta_size_cm", tamil),
                (if (tamil) "வாழ்நாள்" else "Lifespan") to fish.bilingual("life_yr", "ta_life_yr", tamil),
                (if (tamil) "நடத்தை" else "Temperament") to fish.bilingual("temper", "ta_temper", tamil)
            )
        )

        NativeDetailBlock(
            if (tamil) "பரவலும் இந்திய நிலையும்" else "Distribution & status in India",
            listOf(
                (if (tamil) "தாயகப் பரவல்" else "Native range") to fish.bilingual("native", "ta_native", tamil),
                (if (tamil) "இந்திய நிலை" else "Status in India") to fish.bilingual("status_in", "ta_status_in", tamil),
                (if (tamil) "வாழிடம்" else "Habitat") to nativeHabitatLabel(fish.habitat, tamil)
            )
        )

        NativeDetailBlock(
            if (tamil) "தொட்டி மற்றும் நீர்த் தேவைகள்" else "Housing & water requirements",
            listOf(
                (if (tamil) "குறைந்தபட்ச தொட்டி அளவு" else "Minimum tank") to fish.bilingual("tank_min_l", "ta_tank_min_l", tamil),
                (if (tamil) "குழுவமைப்பு" else "Grouping") to fish.bilingual("group", "ta_group", tamil),
                (if (tamil) "பராமரிப்பு வெப்பநிலை" else "Maintenance temperature") to fish.bilingual("temp_hold_c", "ta_temp_hold_c", tamil),
                "pH" to fish.bilingual("ph", "ta_ph", tamil),
                (if (tamil) "நீர்க்கடினத்தன்மை" else "Hardness") to fish.bilingual("gh_kh", "ta_gh_kh", tamil),
                (if (tamil) "உவர்ப்புத்தன்மை" else "Salinity") to fish.bilingual("salinity", "ta_salinity", tamil),
                (if (tamil) "இணைவாழ்வு" else "Compatibility") to fish.bilingual("compat", "ta_compat", tamil)
            )
        )

        NativeDetailBlock(
            if (tamil) "உணவு, இனப்பெருக்கம் மற்றும் குஞ்சுகள்" else "Diet, breeding & fry",
            listOf(
                (if (tamil) "உணவு" else "Diet") to fish.bilingual("diet", "ta_diet", tamil),
                (if (tamil) "இனப்பெருக்க வெப்பநிலை" else "Breeding temperature") to fish.bilingual("temp_breed_c", "ta_temp_breed_c", tamil),
                (if (tamil) "இனப்பெருக்கம்" else "Breeding") to fish.bilingual("breed", "ta_breed", tamil),
                (if (tamil) "பாலின வேறுபாடு" else "Sex identification") to fish.bilingual("sex", "ta_sex", tamil),
                (if (tamil) "குஞ்சு உணவு" else "Fry feeding") to fish.bilingual("fry", "ta_fry", tamil)
            )
        )

        NativeDetailBlock(
            if (tamil) "நலம், தனிமைப்படுத்தல் மற்றும் பாதுகாப்பு" else "Health, quarantine & conservation",
            listOf(
                (if (tamil) "நலக் குறிப்பு" else "Health notes") to fish.bilingual("disease", "ta_disease", tamil),
                (if (tamil) "தனிமைப்படுத்தல்" else "Quarantine") to fish.bilingual("quarantine", "ta_quarantine", tamil),
                (if (tamil) "பாதுகாப்பு நிலை" else "Conservation") to fish.bilingual("iucn", "ta_iucn", tamil)
            )
        )
    }
}

private fun nativeHabitatLabel(value: String, tamil: Boolean): String {
    if (!tamil) return value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    return when (value.lowercase(Locale.ROOT)) {
        "freshwater" -> "நன்னீர்"
        "brackish" -> "உவர்நீர்"
        "marine" -> "கடல்"
        else -> value
    }
}

private fun nativeShareText(fish: Species, tamil: Boolean): String =
    (if (tamil) fish.ta else fish.en) + " (" + fish.sci.substringBefore("(").trim() + ")\n" +
        (if (tamil) "குடும்பம்: " else "Family: ") + fish.family + "\n" +
        (if (tamil) "தாயகப் பரவல்: " else "Native range: ") + fish.bilingual("native", "ta_native", tamil) + "\n" +
        (if (tamil) "உணவு: " else "Diet: ") + fish.bilingual("diet", "ta_diet", tamil) + "\n" +
        (if (tamil) "இனப்பெருக்கம்: " else "Breeding: ") + fish.bilingual("breed", "ta_breed", tamil) + "\n" +
        "Ornamental Fish Science"

@Composable
internal fun NativeDetailBlock(title: String, rows: List<Pair<String, String>>) {
    val visible = rows.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            visible.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.first, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(row.second, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun NativeAssetImage(
    name: String,
    modifier: Modifier,
    fit: Boolean,
    description: String? = null
) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        if (name.isBlank()) null else runCatching {
            context.assets.open("fish_atlas/$name").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap()
    }
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color(0xFFEAF2F1)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = description,
                modifier = Modifier.fillMaxSize().padding(if (fit) 4.dp else 0.dp),
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text("Image unavailable", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NativePrivacyScreen(tamil: Boolean) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (tamil) "பயன்பாடு மற்றும் தனியுரிமை" else "About & Privacy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(if (tamil) "செயலில் உள்ள தேதி: 22 செப்டம்பர் 2026" else "Effective date: 22 September 2026")
        Text(
            if (tamil)
                "இது இணைய இணைப்பு இல்லாமலும் இயங்கக்கூடிய கல்விச் செயலி. பயனர் கணக்கு தேவையில்லை; தனிப்பட்ட தகவலைச் சேகரிக்க, அனுப்ப, விற்க அல்லது பகிர வடிவமைக்கப்படவில்லை."
            else
                "This is an offline-first educational application. It does not require a user account and is not designed to collect, transmit, sell or share personal information."
        )
        Text(
            if (tamil)
                "மொழித் தேர்வு, சேமித்த இனங்கள், வினா முன்னேற்றம் மற்றும் வாசிப்பு நிலை சாதனத்திலேயே சேமிக்கப்படலாம்."
            else
                "Language preference, saved species, quiz progress and reading position may be stored locally on the device."
        )
        Text(
            if (tamil)
                "மூன்றாம் தரப்பு விளம்பரம், பகுப்பாய்வு SDK அல்லது செயலிகளைக் கடந்து கண்காணிக்கும் வசதி சேர்க்கப்படவில்லை."
            else
                "No third-party advertising, analytics SDK or cross-app tracking is included."
        )
        Button(onClick = {
            val uri = Uri.parse("https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }) {
            Text(if (tamil) "HTTPS தனியுரிமைக் கொள்கையைத் திற" else "Open HTTPS privacy policy")
        }
        HorizontalDivider()
        Text(
            "Developer contact: rameshgascngl@gmail.com\nDepartment of Zoology\nGovernment Arts and Science College, Nagercoil, Tamil Nadu, India",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
