package app.minimise.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppFilter
import kotlinx.coroutines.launch

/**
 * The search box above every list of apps in settings and pickers. It narrows the list as you type
 * (see [app.minimise.apps.domain.AppFilter]); the keyboard's search key just puts the keyboard away.
 */
@Composable
fun AppFilterField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.app_filter_hint),
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.a11y_app_filter_clear))
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A screen's list of apps with the search box above it. [header] (an explainer, a few settings) scrolls
 * away above the box and steps aside while a search is typed, so matches sit right under the box rather
 * than under the keyboard.
 */
@Composable
fun SearchableAppList(
    apps: List<AppEntry>,
    modifier: Modifier = Modifier,
    header: LazyListScope.() -> Unit = {},
    row: @Composable (AppEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(apps, query) { AppFilter.filter(apps, query) { it.label } }
    val listState = rememberLazyListState()
    LazyColumn(state = listState, modifier = modifier) {
        if (query.isBlank()) header()
        appFilterHeader(query = query, onQueryChange = { query = it }, listState = listState)
        items(shown, key = { it.packageName }) { row(it) }
        if (shown.isEmpty() && query.isNotBlank()) item(key = "none") { AppFilterEmpty(query) }
    }
}

/**
 * The search box inside a LazyColumn: it scrolls up with whatever is above it, then stays at the top.
 * Tapping it brings it to the top, so the matches show under it, above the keyboard.
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyListScope.appFilterHeader(query: String, onQueryChange: (String) -> Unit, listState: LazyListState, hint: String? = null) {
    stickyHeader(key = SEARCH_KEY) {
        val scope = rememberCoroutineScope()
        AppFilterField(
            query = query,
            onQueryChange = onQueryChange,
            hint = hint ?: stringResource(R.string.app_filter_hint),
            modifier = Modifier
                // Opaque, so rows scrolling up pass behind it.
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 8.dp)
                .onFocusChanged { focus ->
                    if (!focus.hasFocus) return@onFocusChanged
                    val index = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == SEARCH_KEY }?.index ?: return@onFocusChanged
                    scope.launch { listState.animateScrollToItem(index) }
                },
        )
    }
}

private const val SEARCH_KEY = "app_filter"

/** Shown in place of the list when nothing matches. */
@Composable
fun AppFilterEmpty(query: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_filter_none, query.trim()),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 16.dp),
    )
}
