package app.minimise.capture.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.minimise.R
import app.minimise.capture.domain.Note
import app.minimise.capture.domain.NoteRepository
import app.minimise.core.ui.ScreenScaffold
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InboxViewModel @Inject constructor(private val repository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>?> = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onTicked(note: Note) {
        viewModelScope.launch { repository.setDone(note.id, !note.done) }
    }

    fun onDelete(note: Note) {
        viewModelScope.launch { repository.delete(note.id) }
    }
}

/** Captured thoughts, back at a break: tick, delete or share each one. */
@Composable
fun InboxRoute(viewModel: InboxViewModel = hiltViewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ScreenScaffold(title = stringResource(R.string.inbox_title)) {
        if (notes?.isEmpty() == true) {
            Text(stringResource(R.string.inbox_empty), style = MaterialTheme.typography.bodyLarge)
        }
        LazyColumn {
            items(notes.orEmpty(), key = { it.id }) { note ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = note.done, onCheckedChange = { viewModel.onTicked(note) })
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (note.done) TextDecoration.LineThrough else null,
                            color = if (note.done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Row(modifier = Modifier.padding(start = 40.dp)) {
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, note.text)
                            runCatching { context.startActivity(Intent.createChooser(send, null)) }
                        }) { Text(stringResource(R.string.inbox_share)) }
                        TextButton(onClick = { viewModel.onDelete(note) }) { Text(stringResource(R.string.swap_delete)) }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class InboxCountViewModel @Inject constructor(repository: NoteRepository) : ViewModel() {
    val openCount: StateFlow<Int> = repository.openCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

/** "Inbox · 3 notes", shown at the end of a focus session and in the digest. Nothing when the inbox is empty. */
@Composable
fun InboxLink(onOpenInbox: () -> Unit, viewModel: InboxCountViewModel = hiltViewModel()) {
    val count by viewModel.openCount.collectAsStateWithLifecycle()
    if (count == 0) return
    TextButton(onClick = onOpenInbox) {
        Text(
            text = androidx.compose.ui.res.pluralStringResource(R.plurals.inbox_link, count, count),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
