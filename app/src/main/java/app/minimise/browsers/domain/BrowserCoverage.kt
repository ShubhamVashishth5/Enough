package app.minimise.browsers.domain

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
import app.minimise.core.Defaults
import app.minimise.core.di.ApplicationScope
import app.minimise.monitor.domain.ScreenNode
import app.minimise.monitor.domain.ScreenTree
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** What a browser's address bar says. */
sealed interface AddressBarRead {
    /** Couldn't tell (no address bar on screen, or the user is typing in it): keep what was there. */
    data object Unknown : AddressBarRead

    /** @property targetId the Vault website it shows ("web:instagram.com"), or null for any other page. */
    data class Page(val targetId: String?) : AddressBarRead
}

object AddressBar {
    fun read(root: ScreenNode, rule: BrowserRule, vaultDomains: Set<String>): AddressBarRead {
        val bar = ScreenTree.firstMatch(root, rule.matchers, Defaults.SCREEN_MAX_DEPTH, Defaults.SCREEN_MAX_NODES)
            ?: return AddressBarRead.Unknown
        if (bar.focused) return AddressBarRead.Unknown
        val host = WebTarget.hostOf(bar.text?.toString().orEmpty()) ?: return AddressBarRead.Page(null)
        return AddressBarRead.Page(WebTarget.match(host, vaultDomains)?.let(WebTarget::idFor))
    }
}

/**
 * Makes a Vault website in a browser count as that website, not as the browser: the accessibility
 * service reports "web:instagram.com" in front instead of Chrome, and every rule takes it from there.
 * Browsers are only watched while at least one website is in the Vault.
 */
@Singleton
class BrowserCoverage @Inject constructor(
    source: BrowserRuleSource,
    appRepository: AppRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val rules: Map<String, BrowserRule> by lazy { source.rules().associateBy { it.packageName } }

    private val vaultDomains: StateFlow<Set<String>> = appRepository.kinds
        .map { kinds -> kinds.filter { (id, kind) -> kind == AppKind.VAULT && WebTarget.isWebTarget(id) }.keys.map(WebTarget::domainOf).toSet() }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val watchedPackages: Flow<Set<String>> = vaultDomains
        .map { if (it.isEmpty()) emptySet() else rules.keys }
        .distinctUntilChanged()

    fun isBrowser(packageName: String): Boolean = packageName in rules

    fun read(packageName: String, root: ScreenNode): AddressBarRead {
        val rule = rules[packageName] ?: return AddressBarRead.Unknown
        return AddressBar.read(root, rule, vaultDomains.value)
    }
}
