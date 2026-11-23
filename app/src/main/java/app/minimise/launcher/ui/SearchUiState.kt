package app.minimise.launcher.ui

import app.minimise.apps.domain.AppEntry

data class SearchUiState(
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
    /** An app (or its pause screen) was opened: return to home. */
    val finished: Boolean = false,
)
