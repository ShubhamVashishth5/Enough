package app.minimise.launcher.ui

import app.minimise.apps.domain.AppEntry

data class SearchUiState(
    val query: String = "",
    val results: List<AppEntry> = emptyList(),
)
