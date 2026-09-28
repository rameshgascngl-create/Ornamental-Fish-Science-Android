package com.tnfisheries.ornamentalfish

import android.content.SharedPreferences
import android.text.Html
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == "book",
                onClick = { mode = "book"; contents = false },
                label = { Text(if (tamil) "பாடங்கள்" else "Lessons") }
            )
            FilterChip(
                selected = mode == "labs",
                onClick = { mode = "labs"; contents = false },
                label = { Text(if (tamil) "செய்முறை" else "Practical lab") }
            )
        }

        if (mode == "labs") {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(labs, key = { it.id }) { lab ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (tamil) lab.taTitle else lab.enTitle, fontWeight = FontWeight.Bold)
                            Text(nativeHtmlToText(if (tamil) lab.taHtml else lab.enHtml))
                        }
                    }
                }
            }
            return@Column
        }

        if (book.isEmpty()) {
            Text(if (tamil) "பாடத் தரவு இல்லை." else "No lesson data.", modifier = Modifier.padding(16.dp))
            return@Column
        }

        val safe = pageIndex.coerceIn(0, book.lastIndex)
        if (contents) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Text(if (tamil) "பாட உள்ளடக்கம்" else "Lesson contents", style = MaterialTheme.typography.headlineSmall) }
                items(book.indices.toList()) { i ->
                    val page = book[i]
                    OutlinedButton(
                        onClick = {
                            pageIndex = i
                            prefs.edit().putInt("book_page", i).apply()
                            contents = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text((i + 1).toString() + ". " + if (tamil) page.taTitle else page.enTitle)
                    }
                }
            }
        } else {
            val page = book[safe]
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { contents = true }) { Text(if (tamil) "உள்ளடக்கம்" else "Contents") }
                    Text((safe + 1).toString() + " / " + book.size, modifier = Modifier.padding(top = 12.dp))
                }
                Text(page.chapter, style = MaterialTheme.typography.labelMedium)
                Text(if (tamil) page.taTitle else page.enTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(nativeHtmlToText(if (tamil) page.taHtml else page.enHtml), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = {
                            pageIndex = (safe - 1).coerceAtLeast(0)
                            prefs.edit().putInt("book_page", pageIndex).apply()
                        },
                        enabled = safe > 0
                    ) { Text(if (tamil) "← முன்" else "← Previous") }
                    Button(
                        onClick = {
                            pageIndex = (safe + 1).coerceAtMost(book.lastIndex)
                            prefs.edit().putInt("book_page", pageIndex).apply()
                        },
                        enabled = safe < book.lastIndex
                    ) { Text(if (tamil) "அடுத்தது →" else "Next →") }
                }
            }
        }
    }
}

private fun nativeHtmlToText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()

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
        val wrong = questions.indices.filter { answered[it] != questions[it].answer }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                val pct = score * 100 / questions.size
                Text(if (tamil) "வினா முடிவு" else "Quiz result", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text((if (tamil) "மதிப்பெண் " else "Score ") + score + " / " + questions.size + " (" + pct + "%)")
                Text((if (tamil) "விடையளித்தவை " else "Answered ") + answered.size + " / " + questions.size)
            }
            items(wrong) { i ->
                val q = questions[i]
                val opts = if (tamil) q.taOptions else q.enOptions
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text((i + 1).toString() + ". " + if (tamil) q.taQuestion else q.enQuestion, fontWeight = FontWeight.Bold)
                        Text((if (tamil) "உங்கள் விடை: " else "Your answer: ") + (answered[i]?.let { opts.getOrNull(it) } ?: "—"))
                        Text((if (tamil) "சரி: " else "Correct: ") + opts.getOrNull(q.answer).orEmpty())
                        Text(if (tamil) q.taExplanation else q.enExplanation, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        answered.clear()
                        saveNativeQuizAnswers(prefs, answered)
                        index = 0
                        prefs.edit().putInt("quiz_index", 0).apply()
                        results = false
                    }) { Text(if (tamil) "மீண்டும் தொடங்கு" else "Restart") }
                    OutlinedButton(onClick = {
                        scheduleReminder(
                            24,
                            if (tamil) "அலங்கார மீன் வினாவை மீள்பார்" else "Revise ornamental fish quiz",
                            if (tamil) "தவறிய வினாக்களை மீள்பார்." else "Review the questions you missed."
                        )
                    }) { Text(if (tamil) "நாளை நினைவூட்டு" else "Remind tomorrow") }
                }
            }
        }
        return
    }

    val q = questions[index]
    val options = if (tamil) q.taOptions else q.enOptions
    val picked = answered[index]
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(q.type.uppercase(Locale.ROOT) + " " + (index + 1) + "/" + questions.size)
        Text((if (tamil) "மதிப்பெண் " else "Score ") + score + " / " + answered.size)
        Text(if (tamil) q.taQuestion else q.enQuestion, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
                    else -> CardDefaults.cardColors()
                }
            ) { Text(option, modifier = Modifier.padding(16.dp)) }
        }

        if (picked != null) {
            Text(if (tamil) q.taExplanation else q.enExplanation)
            Button(onClick = {
                if (index < questions.lastIndex) {
                    index += 1
                    prefs.edit().putInt("quiz_index", index).apply()
                } else {
                    results = true
                }
            }) { Text(if (index < questions.lastIndex) (if (tamil) "அடுத்த வினா" else "Next question") else (if (tamil) "முடிவு" else "See result")) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { results = true }) { Text(if (tamil) "முடிவுகள்" else "Results") }
            OutlinedButton(onClick = {
                answered.clear()
                saveNativeQuizAnswers(prefs, answered)
                index = 0
                prefs.edit().putInt("quiz_index", 0).apply()
            }) { Text(if (tamil) "மீட்டமை" else "Reset") }
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
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    NativeCalculatorCard(if (tamil) "செவ்வகக் கொள்ளளவு" else "Rectangular volume") {
        NativeNumberField(if (tamil) "நீளம் செ.மீ." else "Length cm", l) { l = it }
        NativeNumberField(if (tamil) "அகலம் செ.மீ." else "Width cm", w) { w = it }
        NativeNumberField(if (tamil) "நீர் உயரம் செ.மீ." else "Water height cm", h) { h = it }
        Button(onClick = {
            val a = l.toDoubleOrNull()
            val b = w.toDoubleOrNull()
            val c = h.toDoubleOrNull()
            out = if (a != null && b != null && c != null && a > 0 && b > 0 && c > 0) {
                (if (tamil) "கொள்ளளவு ≈ " else "Volume ≈ ") + "%.1f".format(Locale.US, a * b * c / 1000.0) + " L"
            } else {
                if (tamil) "அளவுகள் சுழியை விட அதிகமாக இருக்க வேண்டும்." else "Dimensions must be greater than zero."
            }
        }) { Text(if (tamil) "கணக்கிடு" else "Calculate") }
        if (out.isNotBlank()) Text(out)
    }
}

@Composable
private fun NativeAmmoniaCalculator(tamil: Boolean) {
    var tan by rememberSaveable { mutableStateOf("0.5") }
    var ph by rememberSaveable { mutableStateOf("8.0") }
    var temp by rememberSaveable { mutableStateOf("28") }
    var out by rememberSaveable { mutableStateOf("") }
    NativeCalculatorCard(if (tamil) "TAN-இலிருந்து அயனியாகாத NH₃" else "Unionised NH₃ from TAN") {
        Text(
            if (tamil) "Emerson et al. (1975) அடிப்படையிலான கற்பித்தல் கருவி; ஆய்வகச் சான்று அல்ல."
            else "Teaching tool using Emerson et al. (1975); not a certified laboratory result.",
            style = MaterialTheme.typography.bodySmall
        )
        NativeNumberField("TAN (mg/L as N)", tan) { tan = it }
        NativeNumberField("pH", ph) { ph = it }
        NativeNumberField("T °C", temp) { temp = it }
        Button(onClick = {
            val a = tan.toDoubleOrNull()
            val p = ph.toDoubleOrNull()
            val t = temp.toDoubleOrNull()
            out = when {
                a == null || a !in 0.0..50.0 -> if (tamil) "TAN 0–50 mg/L ஆக இருக்க வேண்டும்." else "TAN must be 0–50 mg/L as N."
                p == null || p !in 6.0..10.0 -> if (tamil) "pH 6–10 மட்டும்." else "pH is limited to 6–10."
                t == null || t !in 5.0..40.0 -> if (tamil) "வெப்பம் 5–40°செ." else "Temperature must be 5–40 °C."
                else -> {
                    val pKa = 0.09018 + 2729.92 / (273.16 + t)
                    val fraction = 1.0 / (10.0.pow(pKa - p) + 1.0)
                    (if (tamil) "அயனியாகாத விகிதம் " else "Unionised fraction ") +
                        "%.3f".format(Locale.US, fraction) +
                        " · NH₃-N " + "%.3f".format(Locale.US, a * fraction) + " mg/L"
                }
            }
        }) { Text(if (tamil) "விளக்கு" else "Interpret") }
        if (out.isNotBlank()) Text(out)
    }
}

@Composable
private fun NativeWaterChangeCalculator(tamil: Boolean) {
    var litres by rememberSaveable { mutableStateOf("100") }
    var pct by rememberSaveable { mutableStateOf("30") }
    var out by rememberSaveable { mutableStateOf("") }
    NativeCalculatorCard(if (tamil) "நீர் மாற்ற அளவு" else "Water-change volume") {
        NativeNumberField(if (tamil) "தொட்டி லிட்டர்" else "Aquarium volume L", litres) { litres = it }
        NativeNumberField(if (tamil) "மாற்ற %" else "Change %", pct) { pct = it }
        Button(onClick = {
            val l = litres.toDoubleOrNull()
            val p = pct.toDoubleOrNull()
            out = if (l != null && l > 0 && p != null && p > 0 && p <= 100) {
                "%.1f".format(Locale.US, l * p / 100.0) + " L"
            } else {
                if (tamil) "உள்ளீட்டைச் சரிபார்க்கவும்." else "Check the inputs."
            }
        }) { Text(if (tamil) "கணக்கிடு" else "Calculate") }
        if (out.isNotBlank()) Text(out)
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
        NativeNumberField("CAPEX ₹", cap) { cap = it }
        NativeNumberField(if (tamil) "சொத்து ஆயுள் ஆண்டு" else "Asset life years", life) { life = it }
        NativeNumberField("OPEX ₹", op) { op = it }
        NativeNumberField(if (tamil) "ஆண்டு உற்பத்தி" else "Annual production", prod) { prod = it }
        NativeNumberField(if (tamil) "விலை ₹ / அலகு" else "Price ₹ / unit", price) { price = it }
        NativeNumberField(if (tamil) "இழப்பு %" else "Loss %", mort) { mort = it }

        Button(onClick = {
            val c = cap.toDoubleOrNull()
            val y = life.toDoubleOrNull()
            val o = op.toDoubleOrNull()
            val n = prod.toDoubleOrNull()
            val pr = price.toDoubleOrNull()
            val m = mort.toDoubleOrNull()

            out = if (c == null || y == null || o == null || n == null || pr == null || m == null ||
                c < 0 || y <= 0 || o < 0 || n < 0 || pr < 0 || m !in 0.0..100.0
            ) {
                if (tamil) "உள்ளீட்டைச் சரிபார்க்கவும்." else "Check the inputs."
            } else {
                val sold = n * (1.0 - m / 100.0)
                val revenue = sold * pr
                val depreciation = c / y
                val profit = revenue - o
                val roi = if (c == 0.0) 0.0 else profit / c * 100.0
                val bc = if (depreciation + o == 0.0) 0.0 else revenue / (depreciation + o)
                (if (tamil) "விற்கும் அலகு " else "Saleable units ") + sold.toInt() +
                    " · " + (if (tamil) "வருவாய் ₹" else "revenue ₹") + revenue.toInt() +
                    " · " + (if (tamil) "லாபம் ₹" else "profit ₹") + profit.toInt() +
                    " · ROI " + "%.1f".format(Locale.US, roi) + "% · B:C " + "%.2f".format(Locale.US, bc)
            }
        }) { Text(if (tamil) "கணக்கிடு" else "Calculate") }
        if (out.isNotBlank()) Text(out)
    }
}

@Composable
private fun NativeCalculatorCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
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
