package app.minimise.core.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.minimise.R

/**
 * A plain full-screen page with a title, used by settings-style screens.
 *
 * @param showBack an arrow before the title that does what the Back gesture does, including any
 * BackHandler the screen has. Off for the first page of setup, where there's nowhere to go back to.
 * @param scrollable the content scrolls as one column. Leave it off for a screen whose content has its
 * own LazyColumn, which can't sit inside another vertical scroll.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        TitleRow(title = title, showBack = showBack)
        if (scrollable) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        } else {
            content()
        }
    }
}

@Composable
private fun TitleRow(title: String, showBack: Boolean) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    if (showBack && dispatcher != null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            // Pulled left so the arrow itself, not its touch area, lines up with the text below.
            IconButton(onClick = { dispatcher.onBackPressed() }, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.a11y_back))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.offset(x = (-8).dp).semantics { heading() },
            )
        }
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp).semantics { heading() },
        )
    }
}
