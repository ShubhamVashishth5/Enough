package app.minimise.apps.domain

/** Stored by name, so never rename an entry. */
enum class AppKind {
    /** Freely available: home slots and search. */
    TOOL,

    /** Distracting: hidden from home, found only by typing the full name, opened through the gate. */
    VAULT,

    /** Never shown anywhere in the launcher. */
    HIDDEN,
}
