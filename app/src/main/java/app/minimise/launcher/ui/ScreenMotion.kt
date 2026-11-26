package app.minimise.launcher.ui

import androidx.activity.BackEventCompat
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry

/**
 * How screens move between each other. Opening a screen is instant. Going back slides the whole screen
 * off the side, over the screen beneath, which stays still.
 *
 * With the Back gesture the screen follows the finger: right when swiping in from the left edge, left
 * when swiping in from the right. Letting go finishes the slide; letting go early puts it back. The back
 * arrow (or a three-button Back) plays the same slide to the right.
 *
 * Navigation gives the gesture its own transitions ([gestureEnter], [gestureExit]), separate from the
 * back arrow's ([backEnter], [backExit]). Leaving them out once gave the gesture Navigation's default, a
 * shrink to 70% that ended in a hard cut.
 */
internal object ScreenMotion {
    private const val BACK_MILLIS = 300

    val open: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }

    val covered: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { ExitTransition.None }

    val backEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { EnterTransition.None }

    val backExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(BACK_MILLIS, easing = FastOutSlowInEasing)) { width -> width }
    }

    val gestureEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = { EnterTransition.None }

    /** Linear, so the gesture's progress moves the screen by the same share of its width. */
    val gestureExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = { edge ->
        slideOutHorizontally(tween(BACK_MILLIS, easing = LinearEasing)) { width ->
            if (edge == BackEventCompat.EDGE_RIGHT) -width else width
        }
    }

    /**
     * A screen's own opaque background. Screens used to be see-through over the activity's, so a screen
     * sliding away showed its text on top of the screen beneath.
     */
    @Composable
    fun Surface(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
    }
}
