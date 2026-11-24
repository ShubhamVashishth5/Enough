package app.minimise.guard.domain

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.monitor.domain.NodeMatcher
import app.minimise.monitor.domain.ScreenNode
import app.minimise.monitor.domain.ScreenTree
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A Settings page that could switch the app off. It is on screen when every one of [matchers] matches some node. */
@Serializable
data class GuardScreen(
    val id: String,
    @SerialName("package") val packageName: String,
    val name: String,
    val matchers: List<NodeMatcher>,
)

@Serializable
private data class GuardScreenFile(val screens: List<GuardScreen> = emptyList())

object GuardScreens {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<GuardScreen> =
        json.decodeFromString(GuardScreenFile.serializer(), text).screens.filter { it.matchers.isNotEmpty() }

    /** The first of [screens] for [packageName] that's showing. [labels] fill in {app_label} and {service_label}. */
    fun match(root: ScreenNode, packageName: String, screens: List<GuardScreen>, labels: Map<String, String>): GuardScreen? =
        screens.filter { it.packageName == packageName }.firstOrNull { screen ->
            ScreenTree.matchesAll(root, screen.matchers.map { it.withLabels(labels) }, Defaults.SCREEN_MAX_DEPTH, Defaults.SCREEN_MAX_NODES)
        }
}

interface GuardScreenSource {
    fun screens(): List<GuardScreen>
}

/**
 * The uninstall guard's second half: during a lock period, the app's own App info, its device admin
 * page and its accessibility settings send the user home, so the app can't be switched off from there.
 * Outside a lock nothing is watched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class LockGuard @Inject constructor(
    source: GuardScreenSource,
    private val settingsStore: SettingsStore,
    private val timeSource: TimeSource,
) {
    private val screens: List<GuardScreen> by lazy { source.screens() }

    /** Settings packages while a lock runs; empty the moment it ends. */
    val watchedPackages: Flow<Set<String>> = settingsStore.settings
        .map { it.lockedUntil }
        .distinctUntilChanged()
        .transformLatest { until ->
            val left = until?.let { durationBetween(timeSource.now(), it) } ?: Duration.ZERO
            if (left > Duration.ZERO) {
                emit(screens.map { it.packageName }.toSet())
                delay(left)
            }
            emit(emptySet())
        }
        .distinctUntilChanged()

    /** The guarded screen showing in [packageName], if a lock is on. */
    suspend fun check(packageName: String, root: ScreenNode, labels: Map<String, String>): GuardScreen? {
        val until = settingsStore.current().lockedUntil ?: return null
        if (!until.isAfter(timeSource.now())) return null
        return GuardScreens.match(root, packageName, screens, labels)
    }
}
