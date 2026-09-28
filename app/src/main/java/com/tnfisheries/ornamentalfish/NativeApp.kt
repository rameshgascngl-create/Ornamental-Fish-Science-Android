package com.tnfisheries.ornamentalfish

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

internal enum class NativeSection(val en: String, val ta: String) {
    HOME("Home", "முகப்பு"),
    ATLAS("Atlas", "மீன்கள்"),
    LEARN("Learn", "பாடம்"),
    QUIZ("Quiz", "வினா"),
    TOOLS("Tools", "கருவிகள்")
}

private data class NativeFilter(val id: String, val en: String, val ta: String)

private val nativeFilters = listOf(
    NativeFilter("all", "All", "அனைத்தும்"),
    NativeFilter("indigenous", "Indian", "தாயக"),
    NativeFilter("exotic", "Exotic", "அந்நிய"),
    NativeFilter("freshwater", "Freshwater", "நன்னீர்"),
    NativeFilter("brackish", "Brackish", "உவர்நீர்"),
    NativeFilter("marine", "Marine", "கடல்"),
    NativeFilter("livebearer", "Livebearer", "குஞ்சு ஈனும்"),
    NativeFilter("egg-layer", "Egg-layer", "முட்டையிடும்"),
    NativeFilter("peaceful", "Peaceful", "அமைதியான"),
    NativeFilter("active", "Active", "சுறுசுறுப்பு"),
    NativeFilter("conservation", "Conservation", "பாதுகாப்பு")
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
    OrnamentalFishTheme {
        val content = contentResult.getOrNull()
        if (content == null) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Content could not be loaded", style = MaterialTheme.typography.headlineSmall)
                    Text(contentResult.exceptionOrNull()?.message ?: "Required offline data are missing.")
                }
            }
            return@OrnamentalFishTheme
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
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        Image(
                            painter = painterResource(R.drawable.app_icon),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 12.dp).size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            if (tamil) "அலங்கார மீன் அறிவியல்" else "Ornamental Fish Science",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        OutlinedButton(
                            onClick = {
                                tamil = !tamil
                                prefs.edit().putBoolean("tamil", tamil).apply()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (tamil) "English" else "தமிழ்")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NativeSection.entries.forEach { item ->
                        NavigationBarItem(
                            selected = section == item && !privacy,
                            onClick = {
                                section = item
                                selectedId = null
                                privacy = false
                            },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        NativeSection.HOME -> Icons.Rounded.Home
                                        NativeSection.ATLAS -> Icons.Rounded.SetMeal
                                        NativeSection.LEARN -> Icons.Rounded.MenuBook
                                        NativeSection.QUIZ -> Icons.Rounded.Quiz
                                        NativeSection.TOOLS -> Icons.Rounded.Build
                                    },
                                    contentDescription = if (tamil) item.ta else item.en
                                )
                            },
                            label = { Text(if (tamil) item.ta else item.en, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
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
    val hero = content.species.firstOrNull { it.id == "goldfish" } ?: content.species.firstOrNull()
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (hero != null) {
                        NativeAssetImage(
                            hero.image,
                            Modifier.size(118.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (tamil) "அலங்கார மீன்களை அறிவோம்" else "Explore ornamental fishes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (tamil)
                                "இன அடையாளம், வளர்ப்பு, இனப்பெருக்கம், நீர்தரம் மற்றும் பண்ணை மேலாண்மை."
                            else
                                "Species identification, husbandry, breeding, water quality and farm management.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onAtlas) {
                            Icon(Icons.Rounded.SetMeal, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (tamil) "மீன் இனங்களைப் பார்க்க" else "Open species atlas")
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NativeStatCard(content.species.size.toString(), if (tamil) "மீன் இனங்கள்" else "Species", Modifier.weight(1f))
                NativeStatCard(content.book.size.toString(), if (tamil) "பாடங்கள்" else "Lessons", Modifier.weight(1f))
                NativeStatCard(content.quiz.size.toString(), if (tamil) "வினாக்கள்" else "Questions", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NativeHomeAction(
                    title = if (tamil) "கற்றல்" else "Learn",
                    body = if (tamil) "37 பாடங்கள் + செய்முறைகள்" else "37 lessons + practicals",
                    icon = Icons.Rounded.MenuBook,
                    onClick = onLearn,
                    modifier = Modifier.weight(1f)
                )
                NativeHomeAction(
                    title = if (tamil) "வினாடி வினா" else "Quiz",
                    body = if (tamil) "மதிப்பெண் மற்றும் விளக்கம்" else "Score and explanations",
                    icon = Icons.Rounded.Quiz,
                    onClick = onQuiz,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NativeHomeAction(
                    title = if (tamil) "கருவிகள்" else "Tools",
                    body = if (tamil) "நீர்தரம் மற்றும் கணக்கீடுகள்" else "Water-quality calculators",
                    icon = Icons.Rounded.Build,
                    onClick = onTools,
                    modifier = Modifier.weight(1f)
                )
                NativeHomeAction(
                    title = if (tamil) "சேமித்தவை" else "Bookmarks",
                    body = bookmarkCount.toString() + if (tamil) " இனங்கள்" else " species",
                    icon = Icons.Rounded.Bookmark,
                    onClick = onAtlas,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) {
                Text(if (tamil) "தனியுரிமைக் கொள்கை" else "Privacy policy")
            }
        }
    }
}

@Composable
private fun NativeStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun NativeHomeAction(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier.height(132.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

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

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (tamil) "மீன் இனங்கள்" else "Species atlas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (tamil) "பெயர், குடும்பம் அல்லது பரவல்" else "Search name, family or region") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = if (tamil) "அழி" else "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(end = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nativeFilters) { f ->
                        FilterChip(
                            selected = filter == f.id,
                            onClick = { filter = f.id },
                            label = { Text(if (tamil) f.ta else f.en) }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (tamil) "காட்டப்படுவது " else "Showing ") + visible.size + " / " + species.size,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (bookmarks.isNotEmpty()) {
                        TextButton(onClick = {
                            val rows = species.filter { it.id in bookmarks }.map {
                                (if (tamil) it.ta else it.en) + " — " + it.sci.substringBefore("(").trim()
                            }
                            exportBookmarks((listOf("Ornamental Fish Science — bookmarks", "") + rows).joinToString("\n"))
                        }) {
                            Text(if (tamil) "சேமித்தவை ஏற்றுமதி" else "Export bookmarks")
                        }
                    }
                }
            }
        }

        items(visible, key = { it.id }) { fish ->
            Card(
                onClick = { onSelect(fish.id) },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NativeAssetImage(
                        fish.image,
                        Modifier.width(118.dp).height(92.dp),
                        contentScale = ContentScale.Fit
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            if (tamil) fish.ta else fish.en,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            fish.sci.substringBefore("(").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic
                        )
                        Text(fish.family, style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NativePill(
                                when (fish.habitat) {
                                    "freshwater" -> if (tamil) "நன்னீர்" else "Freshwater"
                                    "brackish" -> if (tamil) "உவர்நீர்" else "Brackish"
                                    "marine" -> if (tamil) "கடல்" else "Marine"
                                    else -> fish.habitat
                                }
                            )
                            if (fish.id in bookmarks) {
                                Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NativePill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
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
                val iucn = s.value("iucn").lowercase(Locale.ROOT)
                if (iucn.isBlank() || iucn.contains("least concern")) return false
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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = if (tamil) "பின்செல்" else "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { onBookmark(fish.id) }) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (tamil) "சேமி" else "Bookmark"
                )
            }
            IconButton(onClick = { speak(name, tamil) }) {
                Icon(Icons.Rounded.VolumeUp, contentDescription = if (tamil) "ஒலிக்க" else "Pronounce")
            }
            IconButton(onClick = { share(nativeShareText(fish, tamil)) }) {
                Icon(Icons.Rounded.Share, contentDescription = if (tamil) "பகிர்" else "Share")
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            NativeAssetImage(
                fish.image,
                Modifier.fillMaxWidth().height(260.dp).padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            fish.sci.substringBefore("(").trim(),
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NativePill(fish.family)
            NativePill(fish.order)
        }

        NativeDetailBlock(
            if (tamil) "பரவலும் வாழிடமும்" else "Distribution and habitat",
            listOf(
                (if (tamil) "தாயகப் பரவல்" else "Native range") to fish.bilingual("native", "ta_native", tamil),
                (if (tamil) "இந்திய நிலை" else "Status in India") to fish.bilingual("status_in", "ta_status_in", tamil),
                (if (tamil) "வாழிடம்" else "Habitat") to fish.habitat
            )
        )
        NativeDetailBlock(
            if (tamil) "பராமரிப்பு" else "Husbandry",
            listOf(
                (if (tamil) "குறைந்த தொட்டி அளவு" else "Minimum tank") to fish.bilingual("tank_min_l", "ta_tank_min_l", tamil),
                (if (tamil) "பராமரிப்பு வெப்பநிலை" else "Maintenance temperature") to fish.bilingual("temp_hold_c", "ta_temp_hold_c", tamil),
                "pH" to fish.bilingual("ph", "ta_ph", tamil),
                (if (tamil) "கடினத்தன்மை" else "Hardness") to fish.bilingual("gh_kh", "ta_gh_kh", tamil),
                (if (tamil) "உணவு" else "Diet") to fish.bilingual("diet", "ta_diet", tamil),
                (if (tamil) "இனப்பெருக்கம்" else "Breeding") to fish.bilingual("breed", "ta_breed", tamil)
            )
        )
        NativeDetailBlock(
            if (tamil) "நலம் மற்றும் பாதுகாப்பு" else "Health and conservation",
            listOf(
                (if (tamil) "நோய் குறிப்பு" else "Health notes") to fish.bilingual("disease", "ta_disease", tamil),
                (if (tamil) "தனிமைப்படுத்தல்" else "Quarantine") to fish.bilingual("quarantine", "ta_quarantine", tamil),
                (if (tamil) "பாதுகாப்பு நிலை" else "Conservation") to fish.bilingual("iucn", "ta_iucn", tamil)
            )
        )
        Spacer(Modifier.height(8.dp))
    }
}

private fun nativeShareText(fish: Species, tamil: Boolean): String {
    return (if (tamil) fish.ta else fish.en) + " (" + fish.sci.substringBefore("(").trim() + ")\n" +
        (if (tamil) "குடும்பம்: " else "Family: ") + fish.family + "\n" +
        (if (tamil) "வாழிடம்: " else "Habitat: ") + fish.habitat + "\n" +
        (if (tamil) "உணவு: " else "Diet: ") + fish.bilingual("diet", "ta_diet", tamil) + "\n" +
        "Ornamental Fish Science"
}

@Composable
internal fun NativeDetailBlock(title: String, rows: List<Pair<String, String>>) {
    val visible = rows.filter { it.second.isNotBlank() }
    if (visible.isEmpty()) return

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            visible.forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.first, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        if (name.isBlank()) null else runCatching {
            context.assets.open("fish_atlas/" + name).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap()
    }

    if (bitmap != null) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp)
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = contentScale
            )
        }
    } else {
        Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SetMeal, contentDescription = null)
            }
        }
    }
}

@Composable
private fun NativePrivacyScreen(tamil: Boolean) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (tamil) "தனியுரிமைக் கொள்கை" else "Privacy Policy",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(if (tamil) "செயலில் உள்ள தேதி: 22 செப்டம்பர் 2026" else "Effective date: 22 September 2026")
        Text(
            if (tamil)
                "இச்செயலி இணையமின்றியும் இயங்கும் கல்விச் செயலி. பயனர் கணக்கு தேவையில்லை; தனிப்பட்ட தகவலைச் சேகரிக்க, அனுப்ப, விற்க அல்லது பகிர வடிவமைக்கப்படவில்லை."
            else
                "Ornamental Fish Science is an offline-first educational application. It does not require a user account and is not designed to collect, transmit, sell or share personal information."
        )
        Text(
            if (tamil)
                "மொழித் தேர்வு, சேமித்த இனங்கள், வினா முன்னேற்றம் மற்றும் வாசிப்பு நிலை சாதனத்தில் மட்டும் சேமிக்கப்படலாம்."
            else
                "Language choice, bookmarks, quiz progress and reading state may be stored locally on the device."
        )
        Text(
            if (tamil)
                "மூன்றாம் தரப்பு விளம்பரம், பகுப்பாய்வு SDK அல்லது செயலிகளுக்கிடையேயான கண்காணிப்பு இல்லை."
            else
                "The app does not include third-party advertising, analytics SDKs or cross-app tracking."
        )
        Button(onClick = {
            val uri = Uri.parse("https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html")
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }) {
            Text(if (tamil) "இணையத் தனியுரிமைக் கொள்கையைத் திற" else "Open online privacy policy")
        }
        Text(
            "Contact: rameshgascngl@gmail.com\n" +
                "Developer: Rajamoni Ramesh\n" +
                "Department of Zoology\n" +
                "Government Arts and Science College, Nagercoil, Tamil Nadu, India",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
