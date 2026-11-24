package app.minimise.tasks.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.tasks.domain.TodayTask

/** Today's tasks under the clock. Tap ticks one off; long-press edits them all. */
@Composable
fun TodayTaskList(
    tasks: List<TodayTask>,
    onTaskTapped: (TodayTask) -> Unit,
    onEditTasks: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.today_set_tasks),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.combinedClickable(onClick = onEditTasks).padding(vertical = 6.dp),
            )
        }
        tasks.forEach { task ->
            Text(
                text = task.text,
                style = MaterialTheme.typography.titleMedium,
                color = if (task.done) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { onTaskTapped(task) }, onLongClick = onEditTasks)
                    .padding(vertical = 6.dp),
            )
        }
    }
}
