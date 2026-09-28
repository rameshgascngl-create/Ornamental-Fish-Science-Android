package com.tnfisheries.ornamentalfish

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class Species(
    val id: String,
    val fields: Map<String, String>
) {
    fun value(key: String): String = fields[key].orEmpty()
    fun bilingual(enKey: String, taKey: String, tamil: Boolean): String =
        if (tamil) fields[taKey].orEmpty().ifBlank { fields[enKey].orEmpty() } else fields[enKey].orEmpty()

    val en: String get() = value("en")
    val ta: String get() = value("ta")
    val sci: String get() = value("sci")
    val family: String get() = value("family")
    val order: String get() = value("order")
    val origin: String get() = value("origin")
    val habitat: String get() = value("habitat")
    val breedType: String get() = value("breed_type")
    val temperClass: String get() = value("temper_class")
    val image: String get() = value("img")
}

internal data class BookPage(
    val id: String,
    val chapter: String,
    val enTitle: String,
    val taTitle: String,
    val enHtml: String,
    val taHtml: String
)

internal data class QuizQuestion(
    val type: String,
    val enQuestion: String,
    val taQuestion: String,
    val enOptions: List<String>,
    val taOptions: List<String>,
    val answer: Int,
    val enExplanation: String,
    val taExplanation: String
)

internal data class LabActivity(
    val id: String,
    val enTitle: String,
    val taTitle: String,
    val enHtml: String,
    val taHtml: String
)

internal data class NativeContent(
    val species: List<Species>,
    val book: List<BookPage>,
    val quiz: List<QuizQuestion>,
    val labs: List<LabActivity>
)

internal class NativeContentRepository(private val context: Context) {
    fun load(): Result<NativeContent> = runCatching {
        NativeContent(
            species = readArray("data/species.json").mapObjects(::parseSpecies),
            book = readArray("data/book.json").mapObjects(::parseBookPage),
            quiz = readArray("data/quiz.json").mapObjects(::parseQuiz),
            labs = readArray("data/labs.json").mapObjects(::parseLab)
        )
    }

    private fun readArray(path: String): JSONArray {
        val text = context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONArray(text)
    }

    private fun parseSpecies(obj: JSONObject): Species {
        val fields = buildMap {
            obj.keys().forEach { key ->
                val v = obj.opt(key)
                put(key, if (v == null || v === JSONObject.NULL) "" else v.toString())
            }
        }
        return Species(obj.optString("id"), fields)
    }

    private fun parseBookPage(obj: JSONObject) = BookPage(
        id = obj.optString("id"),
        chapter = obj.optString("ch"),
        enTitle = obj.optString("en_t"),
        taTitle = obj.optString("ta_t"),
        enHtml = obj.optString("en"),
        taHtml = obj.optString("ta")
    )

    private fun parseQuiz(obj: JSONObject) = QuizQuestion(
        type = obj.optString("type"),
        enQuestion = obj.optString("q"),
        taQuestion = obj.optString("qt"),
        enOptions = obj.optJSONArray("o").toStrings(),
        taOptions = obj.optJSONArray("ot").toStrings(),
        answer = obj.optInt("a", -1),
        enExplanation = obj.optString("e"),
        taExplanation = obj.optString("et")
    )

    private fun parseLab(obj: JSONObject) = LabActivity(
        id = obj.optString("id"),
        enTitle = obj.optString("en_t"),
        taTitle = obj.optString("ta_t"),
        enHtml = obj.optString("en"),
        taHtml = obj.optString("ta")
    )
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

private fun JSONArray?.toStrings(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
}
