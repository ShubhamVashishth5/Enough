package app.minimise.intervention.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.intervention.domain.PauseReason
import kotlin.time.Duration

@Composable
fun PauseScreen(
    state: PauseUiState,
    onReasonChosen: (PauseReason) -> Unit,
    onTimeBoxChosen: (Duration) -> Unit,
    onChangeReason: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = state.appLabel, style = MaterialTheme.typography.titleLarge)
        state.openNumber?.let {
            Text(
                text = stringResource(R.string.pause_open_count, it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.waiting -> Breathing(secondsLeft = state.remaining.inWholeSeconds + 1)
                state.reason == null -> ReasonChoices(onReasonChosen)
                else -> TimeBoxChoices(state.timeBoxOptions, onTimeBoxChosen, onChangeReason)
            }
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.pause_cancel), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Breathing(secondsLeft: Long) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath scale",
    )
    val colour = MaterialTheme.colorScheme.onSurface
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(color = colour, radius = size.minDimension / 2 * scale, style = Stroke(width = 2.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = secondsLeft.toString(), style = MaterialTheme.typography.displayMedium)
            Text(
                text = stringResource(R.string.pause_breathe),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReasonChoices(onReasonChosen: (PauseReason) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.pause_question),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        PauseReason.entries.forEach { reason ->
            Text(
                text = stringResource(reason.labelRes()),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReasonChosen(reason) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun TimeBoxChoices(options: List<Duration>, onTimeBoxChosen: (Duration) -> Unit, onChangeReason: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.pause_how_long),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        options.forEach { option ->
            Text(
                text = stringResource(R.string.duration_minutes, option.inWholeMinutes),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTimeBoxChosen(option) }
                    .padding(vertical = 12.dp),
            )
        }
        TextButton(onClick = onChangeReason, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.pause_change_reason))
        }
    }
}

private fun PauseReason.labelRes(): Int = when (this) {
    PauseReason.REPLY -> R.string.pause_reason_reply
    PauseReason.LOOK_UP -> R.string.pause_reason_look_up
    PauseReason.BORED -> R.string.pause_reason_bored
    PauseReason.HABIT -> R.string.pause_reason_habit
}

private const val BREATH_MILLIS = 4_000
