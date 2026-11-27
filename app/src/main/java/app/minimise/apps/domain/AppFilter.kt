package app.minimise.apps.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Narrows a list of apps by name, wherever settings or a picker list them. Unlike home's search
 * (AppSearch), every app matches on any part of its name: these lists are for choosing, not opening.
 * Case and accents don't count, so "meteo" finds "Météo".
 */
object AppFilter {
    fun matches(label: String, query: String): Boolean {
        val needle = query.normalised()
        return needle.isEmpty() || needle in label.normalised()
    }

    /** [items] whose [label] matches [query], in their order; all of them for a blank query. */
    fun <T> filter(items: List<T>, query: String, label: (T) -> String): List<T> =
        if (query.isBlank()) items else items.filter { matches(label(it), query) }

    private fun String.normalised(): String =
        Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(MARKS, "").lowercase(Locale.ROOT)

    private val MARKS = Regex("\\p{Mn}+")
}
