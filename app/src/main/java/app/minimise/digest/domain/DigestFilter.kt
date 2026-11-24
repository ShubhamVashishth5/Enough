package app.minimise.digest.domain

/** What to do with a newly posted notification. */
enum class DigestAction {
    /** Leave it alone: calls, alarms, exempt apps, ongoing ones. */
    PASS,

    /** Save it for the next digest and take it off the shade. */
    HOLD,

    /** A group's summary line: take it off too, but there's nothing to save. */
    DROP,
}

object DigestFilter {
    private val ALWAYS_THROUGH = setOf("call", "alarm")

    fun actionFor(
        packageName: String,
        ownPackage: String,
        exempt: Set<String>,
        category: String?,
        ongoing: Boolean,
        clearable: Boolean,
        fullScreen: Boolean,
        groupSummary: Boolean,
    ): DigestAction = when {
        packageName == ownPackage || packageName in exempt -> DigestAction.PASS
        category in ALWAYS_THROUGH || fullScreen || ongoing || !clearable -> DigestAction.PASS
        groupSummary -> DigestAction.DROP
        else -> DigestAction.HOLD
    }
}
