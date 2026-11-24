package app.minimise.intervention.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.core.ui.ChoiceText
import app.minimise.core.ui.animationsOff
import app.minimise.core.ui.announcedHeading
import app.minimise.core.ui.durationSpoken
import app.minimise.core.ui.durationText
import app.minimise.intervention.domain.PauseReason
import app.minimise.swaps.domain.Swap
import kotlin.time.Duration

@Composable
fun PauseScreen(
    state: PauseUiState,
    onReasonChosen: (PauseReason) -> Unit,
    onTimeBoxChosen: (Duration) -> Unit,
    onChangeReason: () -> Unit,
    onSwapTaken: () -> Unit,
    onSwapDeclined: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = state.appLabel, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        state.openNumber?.let {
            Text(
                text = pluralStringResource(R.plurals.pause_open_count, it, it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.budgetLeft?.let {
            Text(
                text = stringResource(R.string.pause_budget_left, durationText(it)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.waiting -> Breathing(secondsLeft = state.remaining.inWholeSeconds + 1)
                state.breatheOnly -> OpenChoice(state.appLabel, onOpen)
                state.reason == null -> ReasonChoices(onReasonChosen)
                state.offeringSwap && state.swap != null ->
                    SwapOffer(state.swap, state.swapAppLabel, onSwapTaken, onSwapDeclined)
                else -> TimeBoxChoices(state.timeBoxOptions, onTimeBoxChosen, onChangeReason)
            }
        }
        TextButton(onClick = onCancel) {
            Text(text = stringResource(R.string.pause_cancel), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * The circle grows and shrinks at breathing pace, and holds still when the system's animations are off.
 * TalkBack reads it as one line ("Breathe, 8 seconds left") when touched; it doesn't count down out loud.
 */
@Composable
private fun Breathing(secondsLeft: Long) {
    val still = animationsOff()
    val transition = rememberInfiniteTransition(label = "breath")
    val breathing by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(BREATH_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath scale",
    )
    val scale = if (still) STILL_SCALE else breathing
    val colour = MaterialTheme.colorScheme.onSurface
    val spoken = pluralStringResource(R.plurals.pause_breathe_spoken, secondsLeft.toInt(), secondsLeft)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }) {
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

/** After a Breathe app's pause: still want it? */
@Composable
private fun OpenChoice(appLabel: String, onOpen: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.pause_breathe_question),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.announcedHeading(),
        )
        OutlinedButton(onClick = onOpen) {
            Text(text = stringResource(R.string.pause_breathe_open, appLabel), style = MaterialTheme.typography.titleMedium)
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
            modifier = Modifier.padding(bottom = 16.dp).announcedHeading(),
        )
        PauseReason.entries.forEach { reason ->
            ChoiceText(text = stringResource(reason.labelRes()), onClick = { onReasonChosen(reason) })
        }
    }
}

@Composable
private fun SwapOffer(swap: Swap, appLabel: String?, onSwapTaken: () -> Unit, onSwapDeclined: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.pause_swap_question),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.announcedHeading(),
        )
        Text(
            text = swap.text,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        appLabel?.let {
            Text(
                text = stringResource(R.string.swaps_opens, it),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onSwapTaken, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = stringResource(R.string.pause_swap_take), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onSwapDeclined) { Text(stringResource(R.string.pause_swap_open_anyway)) }
    }
}

@Composable
private fun TimeBoxChoices(options: List<Duration>, onTimeBoxChosen: (Duration) -> Unit, onChangeReason: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.pause_how_long),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp).announcedHeading(),
        )
        options.forEach { option ->
            ChoiceText(text = durationText(option), description = durationSpoken(option), onClick = { onTimeBoxChosen(option) })
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

/** The circle's size while animations are off. */
private const val STILL_SCALE = 0.8f
