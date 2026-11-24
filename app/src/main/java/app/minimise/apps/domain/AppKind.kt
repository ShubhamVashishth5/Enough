package app.minimise.apps.domain

/** Stored by name, so never rename an entry. */
enum class AppKind {
    /** Freely available: home slots and search. */
    TOOL,

    /** Distracting: hidden from home, found only by typing the full name, opened through the gate. */
    VAULT,

    /** Never shown anywhere in the launcher. */
    HIDDEN,

    /**
     * On home and in search like a Tool app, but every open starts with the breathing pause. Nothing
     * else of the Vault: no reason, no time box, no daily budget, no open limit.
     */
    BREATHE,
    ;

    companion object {
        /** Kinds that can sit on the home screen and match on any part of their name in search. */
        val VISIBLE: Set<AppKind> = setOf(TOOL, BREATHE)
    }
}
