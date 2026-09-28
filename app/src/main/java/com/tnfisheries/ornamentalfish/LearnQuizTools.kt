package com.tnfisheries.ornamentalfish

import android.content.SharedPreferences
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.util.Locale
import kotlin.math.pow

@Composable
internal fun NativeLearnScreen(
    book: List<BookPage>,
    labs: List<LabActivity>,
    tamil: Boolean,
    prefs: SharedPreferences
) {
    var mode by rememberSaveable { mutableStateOf("book") }
    var pageIndex by rememberSaveable {
        mutableStateOf(prefs.getInt("book_page", 0).coerceIn(0, (book.size - 1).coerceAtLeast(0)))
    }
    var contents by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = mode == "book",
                onClick = { mode = "book"; contents = false },
                label = { Text(if (tamil) "பாடங்கள்" else "Lessons") }
            )
            FilterChip(
                selected = mode == "labs",
                onClick = { mode = "labs"; contents = false },
                label = { Text(if (tamil) "செய்முறைப் பயிற்சி" else "Practicals") }
            )
        }

        if (mode == "labs") {
            NativePracticalList(labs, tamil)
            return@Column
        }

        if (book.isEmpty()) {
            Text(if (tamil) "பாடத் தரவு இல்லை." else "No lesson data.", modifier = Modifier.padding(16.dp))
            return@Column
        }

        val safe = pageIndex.coerceIn(0, book.lastIndex)

        if (contents) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        if (tamil) "பாட உள்ளடக்கம்" else "Lesson Contents",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (tamil) "தேவையான பாடத்தைத் தேர்ந்தெடுக்கவும்." else "Select a lesson to continue.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                items(book.indices.toList()) { i ->
                    val page = book[i]
                    Card(
                        onClick = {
                            pageIndex = i
                            prefs.edit().putInt("book_page", i).apply()
                            contents = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (i == safe)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                (if (tamil) "பாடம் " else "Lesson ") + (i + 1),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (tamil) page.taTitle else page.enTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            return@Column
        }

        val page = book[safe]
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { contents = true }) {
                    Text(if (tamil) "உள்ளடக்கம்" else "Contents")
                }
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text(
                        (if (tamil) "பாடம் " else "Lesson ") + (safe + 1) + " / " + book.size,
                        style = MaterialTheme.typography.labelLarge
                    )
                    LinearProgressIndicator(
                        progress = { (safe + 1).toFloat() / book.size.toFloat() },
                        modifier = Modifier.width(120.dp)
                    )
                }
            }

            if (page.chapter.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        (if (tamil) "அலகு " else "Unit ") + page.chapter,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                if (tamil) page.taTitle else page.enTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            NativeRichHtml(if (tamil) page.taHtml else page.enHtml)

            HorizontalDivider()

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        pageIndex = (safe - 1).coerceAtLeast(0)
                        prefs.edit().putInt("book_page", pageIndex).apply()
                    },
                    enabled = safe > 0
                ) {
                    Text(if (tamil) "← முந்தையது" else "← Previous")
                }

                Button(
                    onClick = {
                        pageIndex = (safe + 1).coerceAtMost(book.lastIndex)
                        prefs.edit().putInt("book_page", pageIndex).apply()
                    },
                    enabled = safe < book.lastIndex
                ) {
                    Text(if (tamil) "அடுத்தது →" else "Next →")
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NativePracticalList(labs: List<LabActivity>, tamil: Boolean) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (tamil) "செய்முறைப் பயிற்சிகள்" else "Practical Activities",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (tamil)
                    "நோக்கம், கோட்பாடு, செய்முறை, முன்னெச்சரிக்கை மற்றும் வாய்மொழி வினாக்களுடன்."
                else
                    "Structured with aim, principle, procedure, precautions and viva questions."
            )
        }
        items(labs, key = { it.id }) { lab ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (tamil) lab.taTitle else lab.enTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    NativeRichHtml(if (tamil) lab.taHtml else lab.enHtml)
                }
            }
        }
    }
}

@Composable
private fun NativeRichHtml(html: String) {
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                textSize = 17f
                setLineSpacing(8f, 1.08f)
                setTextIsSelectable(true)
                setPadding(0, 0, 0, 0)
            }
        },
        update = { view ->
            view.setTextColor(textColor)
            view.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        }
    )
}

@Composable
internal fun NativeQuizScreen(
    questions: List<QuizQuestion>,
    tamil: Boolean,
    prefs: SharedPreferences,
    scheduleReminder: (Int, String, String) -> Unit
) {
    if (questions.isEmpty()) {
        Text(if (tamil) "வினாக்கள் இல்லை." else "No quiz questions.", modifier = Modifier.padding(16.dp))
        return
    }

    val answered = remember {
        mutableStateMapOf<Int, Int>().apply { putAll(loadNativeQuizAnswers(prefs)) }
    }
    var index by rememberSaveable {
        mutableStateOf(prefs.getInt("quiz_index", 0).coerceIn(0, questions.lastIndex))
    }
    var results by rememberSaveable { mutableStateOf(false) }
    val score = answered.count { entry -> questions.getOrNull(entry.key)?.answer == entry.value }

    if (results) {
        NativeQuizResults(
            questions = questions,
            answered = answered,
            score = score,
            tamil = tamil,
            onRestart = {
                answered.clear()
                saveNativeQuizAnswers(prefs, answered)
                index = 0
                prefs.edit().putInt("quiz_index", 0).apply()
                results = false
            },
            onRemind = {
                scheduleReminder(
                    24,
                    if (tamil) "அலங்கார மீன் வினாக்களை மீள்பார்க்கவும்" else "Review ornamental fish quiz",
                    if (tamil) "தவறிய வினாக்களையும் விளக்கங்களையும் மீண்டும் பார்க்கவும்."
                    else "Review the questions and explanations you missed."
                )
            }
        )
        return
    }

    val q = questions[index]
    val options = if (tamil) q.taOptions else q.enOptions
    val picked = answered[index]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    if (tamil) "சுயமதிப்பீட்டு வினா" else "Self-assessment Quiz",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    q.type.uppercase(Locale.ROOT) + " · " + (index + 1) + " / " + questions.size,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                (if (tamil) "மதிப்பெண் " else "Score ") + score + " / " + answered.size,
                style = MaterialTheme.typography.labelLarge
            )
        }

        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                if (tamil) q.taQuestion else q.enQuestion,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        options.forEachIndexed { optionIndex, option ->
            val correct = picked != null && optionIndex == q.answer
            val wrongPick = picked != null && optionIndex == picked && picked != q.answer
            Card(
                onClick = {
                    if (picked == null) {
                        answered[index] = optionIndex
                        saveNativeQuizAnswers(prefs, answered)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = when {
                    correct -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    wrongPick -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    else -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                }
            ) {
                Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(('A'.code + optionIndex).toChar().toString() + ".", fontWeight = FontWeight.Bold)
                    Text(option, modifier = Modifier.weight(1f))
                }
            }
        }

        if (picked != null) {
            val correct = picked == q.answer
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (correct)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (correct)
                            (if (tamil) "சரியான விடை" else "Correct")
                        else
                            (if (tamil) "மீண்டும் கவனிக்கவும்" else "Review this point"),
                        fontWeight = FontWeight.Bold
                    )
                    Text(if (tamil) q.taExplanation else q.enExplanation)
                }
            }

            Button(
                onClick = {
                    if (index < questions.lastIndex) {
                        index += 1
                        prefs.edit().putInt("quiz_index", index).apply()
                    } else {
                        results = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (index < questions.lastIndex)
                        (if (tamil) "அடுத்த வினா" else "Next question")
                    else
                        (if (tamil) "முடிவைக் காண்க" else "See result")
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { results = true }) {
                Text(if (tamil) "முடிவுகள்" else "Results")
            }
            OutlinedButton(onClick = {
                answered.clear()
                saveNativeQuizAnswers(prefs, answered)
                index = 0
                prefs.edit().putInt("quiz_index", 0).apply()
            }) {
                Text(if (tamil) "மீட்டமை" else "Reset")
            }
        }
    }
}

@Composable
private fun NativeQuizResults(
    questions: List<QuizQuestion>,
    answered: Map<Int, Int>,
    score: Int,
    tamil: Boolean,
    onRestart: () -> Unit,
    onRemind: () -> Unit
) {
    val wrong = questions.indices.filter { answered[it] != questions[it].answer }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        if (tamil) "வினா முடிவு" else "Quiz Result",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val pct = score * 100 / questions.size
                    Text(
                        (if (tamil) "மதிப்பெண்: " else "Score: ") + score + " / " + questions.size + " (" + pct + "%)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        (if (tamil) "விடையளித்தவை: " else "Answered: ") + answered.size + " / " + questions.size
                    )
                }
            }
        }

        if (wrong.isEmpty()) {
            item {
                Text(
                    if (tamil) "அனைத்து விடைகளும் சரியாக உள்ளன." else "All answered questions are correct.",
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            item {
                Text(
                    if (tamil) "மீள்பார்க்க வேண்டிய வினாக்கள்" else "Questions to Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(wrong) { i ->
                val q = questions[i]
                val opts = if (tamil) q.taOptions else q.enOptions
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            (i + 1).toString() + ". " + if (tamil) q.taQuestion else q.enQuestion,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            (if (tamil) "உங்கள் விடை: " else "Your answer: ") +
                                (answered[i]?.let { opts.getOrNull(it) } ?: "—")
                        )
                        Text(
                            (if (tamil) "சரியான விடை: " else "Correct answer: ") +
                                opts.getOrNull(q.answer).orEmpty()
                        )
                        Text(if (tamil) q.taExplanation else q.enExplanation)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRestart) { Text(if (tamil) "மீண்டும் தொடங்கு" else "Restart") }
                OutlinedButton(onClick = onRemind) {
                    Text(if (tamil) "நாளை நினைவூட்டு" else "Remind tomorrow")
                }
            }
        }
    }
}

private fun loadNativeQuizAnswers(prefs: SharedPreferences): Map<Int, Int> = runCatching {
    val obj = JSONObject(prefs.getString("quiz_answers", "{}") ?: "{}")
    buildMap {
        obj.keys().forEach { key ->
            key.toIntOrNull()?.let { put(it, obj.optInt(key)) }
        }
    }
}.getOrDefault(emptyMap())

private fun saveNativeQuizAnswers(prefs: SharedPreferences, answers: Map<Int, Int>) {
    val obj = JSONObject()
    answers.forEach { entry -> obj.put(entry.key.toString(), entry.value) }
    prefs.edit().putString("quiz_answers", obj.toString()).apply()
}

@Composable
internal fun NativeToolsScreen(tamil: Boolean) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                if (tamil) "அறிவியல் கணக்கீட்டு கருவிகள்" else "Scientific Calculators",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (tamil)
                    "வகுப்பறை மற்றும் பண்ணைப் பயிற்சிக்கான கணக்கீடுகள். அளவீடுகள் சரியான கருவிகளால் உறுதிப்படுத்தப்பட வேண்டும்."
                else
                    "Calculators for classroom and farm practice. Measurements should be verified with appropriate instruments."
            )
        }
        item { NativeVolumeCalculator(tamil) }
        item { NativeAmmoniaCalculator(tamil) }
        item { NativeWaterChangeCalculator(tamil) }
        item { NativeEconomicsCalculator(tamil) }
    }
}

@Composable
private fun NativeVolumeCalculator(tamil: Boolean) {
    var l by rememberSaveable { mutableStateOf("90") }
    var w by rememberSaveable { mutableStateOf("45") }
    var h by rememberSaveable { mutableStateOf("40") }
    var out by rememberSaveable { mutableStateOf("") }

    NativeCalculatorCard(if (tamil) "மீன் காட்சித் தொட்டியின் பயன்பாட்டு நீரளவு" else "Aquarium working volume") {
        Text(
            if (tamil) "செவ்வகத் தொட்டி: நீளம் × அகலம் × நீர் உயரம் ÷ 1000"
            else "Rectangular tank: length × width × water height ÷ 1000",
            style = MaterialTheme.typography.bodySmall
        )
        NativeNumberField(if (tamil) "நீளம் (செ.மீ.)" else "Length (cm)", l) { l = it }
        NativeNumberField(if (tamil) "அகலம் (செ.மீ.)" else "Width (cm)", w) { w = it }
        NativeNumberField(if (tamil) "நீர் உயரம் (செ.மீ.)" else "Water height (cm)", h) { h = it }
        Button(onClick = {
            val a = l.toDoubleOrNull()
            val b = w.toDoubleOrNull()
            val c = h.toDoubleOrNull()
            out = if (a != null && b != null && c != null && a > 0 && b > 0 && c > 0) {
                (if (tamil) "பயன்பாட்டு நீரளவு ≈ " else "Working volume ≈ ") +
                    "%.1f".format(Locale.US, a * b * c / 1000.0) + " L"
            } else {
                if (tamil) "அனைத்து அளவுகளும் சுழியை விட அதிகமாக இருக்க வேண்டும்."
                else "All dimensions must be greater than zero."
            }
        }) {
            Text(if (tamil) "கணக்கிடு" else "Calculate")
        }
        if (out.isNotBlank()) Text(out, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NativeAmmoniaCalculator(tamil: Boolean) {
    var tan by rememberSaveable { mutableStateOf("0.5") }
    var ph by rememberSaveable { mutableStateOf("8.0") }
    var temp by rememberSaveable { mutableStateOf("28") }
    var out by rememberSaveable { mutableStateOf("") }

    NativeCalculatorCard(if (tamil) "TAN அடிப்படையிலான அயனியாகாத NH₃" else "Unionised NH₃ from TAN") {
        Text(
            if (tamil)
                "Emerson சமநிலைச் சமன்பாட்டை அடிப்படையாகக் கொண்ட கற்பித்தல் கணக்கீடு; ஆய்வக அளவீட்டிற்கு மாற்றாகாது."
            else
                "Teaching calculation based on the Emerson equilibrium relationship; it does not replace laboratory measurement.",
            style = MaterialTheme.typography.bodySmall
        )
        NativeNumberField("TAN (mg/L as N)", tan) { tan = it }
        NativeNumberField("pH", ph) { ph = it }
        NativeNumberField(if (tamil) "வெப்பநிலை (°செ.)" else "Temperature (°C)", temp) { temp = it }

        Button(onClick = {
            val a = tan.toDoubleOrNull()
            val p = ph.toDoubleOrNull()
            val t = temp.toDoubleOrNull()
            out = when {
                a == null || a !in 0.0..50.0 ->
                    if (tamil) "TAN மதிப்பு 0–50 mg/L (N ஆக) இருக்க வேண்டும்." else "TAN must be 0–50 mg/L as N."
                p == null || p !in 6.0..10.0 ->
                    if (tamil) "இந்தக் கணக்கீட்டில் pH 6–10 வீச்சைப் பயன்படுத்தவும்." else "Use pH 6–10 for this calculator."
                t == null || t !in 5.0..40.0 ->
                    if (tamil) "வெப்பநிலை 5–40°செ. வீச்சில் இருக்க வேண்டும்." else "Temperature must be 5–40 °C."
                else -> {
                    val pKa = 0.09018 + 2729.92 / (273.16 + t)
                    val fraction = 1.0 / (10.0.pow(pKa - p) + 1.0)
                    (if (tamil) "அயனியாகாத பகுதி = " else "Unionised fraction = ") +
                        "%.3f".format(Locale.US, fraction) +
                        " · NH₃-N = " + "%.3f".format(Locale.US, a * fraction) + " mg/L"
                }
            }
        }) {
            Text(if (tamil) "கணக்கிட்டு விளக்கம் காண்" else "Calculate")
        }
        if (out.isNotBlank()) Text(out, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NativeWaterChangeCalculator(tamil: Boolean) {
    var litres by rememberSaveable { mutableStateOf("100") }
    var pct by rememberSaveable { mutableStateOf("30") }
    var out by rememberSaveable { mutableStateOf("") }

    NativeCalculatorCard(if (tamil) "நீர் மாற்ற அளவு" else "Water-change volume") {
        NativeNumberField(if (tamil) "தொட்டி நீரளவு (L)" else "Aquarium volume (L)", litres) { litres = it }
        NativeNumberField(if (tamil) "மாற்ற வேண்டிய அளவு (%)" else "Water change (%)", pct) { pct = it }
        Button(onClick = {
            val l = litres.toDoubleOrNull()
            val p = pct.toDoubleOrNull()
            out = if (l != null && l > 0 && p != null && p > 0 && p <= 100) {
                (if (tamil) "மாற்ற வேண்டிய நீர் = " else "Water to replace = ") +
                    "%.1f".format(Locale.US, l * p / 100.0) + " L"
            } else {
                if (tamil) "உள்ளீட்டு மதிப்புகளைச் சரிபார்க்கவும்." else "Check the input values."
            }
        }) {
            Text(if (tamil) "கணக்கிடு" else "Calculate")
        }
        if (out.isNotBlank()) Text(out, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NativeEconomicsCalculator(tamil: Boolean) {
    var cap by rememberSaveable { mutableStateOf("80000") }
    var life by rememberSaveable { mutableStateOf("5") }
    var op by rememberSaveable { mutableStateOf("40000") }
    var prod by rememberSaveable { mutableStateOf("2000") }
    var price by rememberSaveable { mutableStateOf("80") }
    var mort by rememberSaveable { mutableStateOf("10") }
    var out by rememberSaveable { mutableStateOf("") }

    NativeCalculatorCard(if (tamil) "பண்ணைப் பொருளாதாரம்" else "Farm economics") {
        NativeNumberField(if (tamil) "மூலதனச் செலவு (₹)" else "Capital expenditure (₹)", cap) { cap = it }
        NativeNumberField(if (tamil) "சொத்து பயன்பாட்டு ஆயுள் (ஆண்டு)" else "Asset life (years)", life) { life = it }
        NativeNumberField(if (tamil) "ஆண்டு இயக்கச் செலவு (₹)" else "Annual operating cost (₹)", op) { op = it }
        NativeNumberField(if (tamil) "ஆண்டு உற்பத்தி (அலகுகள்)" else "Annual production (units)", prod) { prod = it }
        NativeNumberField(if (tamil) "விற்பனை விலை (₹ / அலகு)" else "Selling price (₹ / unit)", price) { price = it }
        NativeNumberField(if (tamil) "இழப்பு (%)" else "Loss (%)", mort) { mort = it }

        Button(onClick = {
            val c = cap.toDoubleOrNull()
            val y = life.toDoubleOrNull()
            val o = op.toDoubleOrNull()
            val n = prod.toDoubleOrNull()
            val pr = price.toDoubleOrNull()
            val m = mort.toDoubleOrNull()

            out = if (
                c == null || y == null || o == null || n == null || pr == null || m == null ||
                c < 0 || y <= 0 || o < 0 || n < 0 || pr < 0 || m !in 0.0..100.0
            ) {
                if (tamil) "உள்ளீட்டு மதிப்புகளைச் சரிபார்க்கவும்." else "Check the input values."
            } else {
                val sold = n * (1.0 - m / 100.0)
                val revenue = sold * pr
                val depreciation = c / y
                val profit = revenue - o
                val roi = if (c == 0.0) 0.0 else profit / c * 100.0
                val bc = if (depreciation + o == 0.0) 0.0 else revenue / (depreciation + o)

                (if (tamil) "விற்பனைக்குத் தகுந்த அலகுகள்: " else "Saleable units: ") + sold.toInt() +
                    " · " + (if (tamil) "வருவாய்: ₹" else "Revenue: ₹") + revenue.toInt() +
                    " · " + (if (tamil) "லாபம்: ₹" else "Profit: ₹") + profit.toInt() +
                    " · ROI " + "%.1f".format(Locale.US, roi) + "% · B:C " + "%.2f".format(Locale.US, bc)
            }
        }) {
            Text(if (tamil) "கணக்கிடு" else "Calculate")
        }
        if (out.isNotBlank()) Text(out, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NativeCalculatorCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun NativeNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}
