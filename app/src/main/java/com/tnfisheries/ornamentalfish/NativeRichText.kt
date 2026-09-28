package com.tnfisheries.ornamentalfish

import android.graphics.Typeface
import android.text.Html
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.BaselineShift
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Composable
internal fun NativeRichText(html: String) {
    val annotated = remember(html) { htmlToAnnotatedString(html) }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp)
    )
}

private fun htmlToAnnotatedString(source: String): AnnotatedString {
    val prepared = normaliseHtmlLists(source)
    val spanned = Html.fromHtml(prepared, Html.FROM_HTML_MODE_LEGACY)
    val text = spanned.toString()
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
    val builder = AnnotatedString.Builder(text)

    spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = spanned.getSpanStart(span).coerceIn(0, text.length)
        val end = spanned.getSpanEnd(span).coerceIn(start, text.length)
        if (start == end) return@forEach

        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> builder.addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
                    start,
                    end
                )
            }
            is UnderlineSpan -> builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                start,
                end
            )
            is SuperscriptSpan -> builder.addStyle(
                SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 13.sp),
                start,
                end
            )
            is SubscriptSpan -> builder.addStyle(
                SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 13.sp),
                start,
                end
            )
        }
    }
    return builder.toAnnotatedString()
}

private fun normaliseHtmlLists(source: String): String {
    var html = source
    val item = Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val ordered = Regex("<ol[^>]*>(.*?)</ol>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    html = ordered.replace(html) { match ->
        var n = 0
        item.replace(match.groupValues[1]) { li ->
            n += 1
            "<p><b>" + n + ".</b> " + li.groupValues[1] + "</p>"
        }
    }
    html = item.replace(html) { li -> "<p>• " + li.groupValues[1] + "</p>" }
    html = html.replace(
        Regex("</?div[^>]*>", RegexOption.IGNORE_CASE),
        "<p>"
    )
    return html
}
