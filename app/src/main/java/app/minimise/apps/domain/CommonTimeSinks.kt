package app.minimise.apps.domain

/** Apps pre-ticked for the Vault during onboarding, when installed. Package names, not labels. */
object CommonTimeSinks {
    val packages: Set<String> = setOf(
        "com.instagram.android",
        "com.instagram.barcelona", // Threads
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.twitter.android", // X
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill", // TikTok (some regions)
        "com.pinterest",
        "tv.twitch.android.app",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient", // Prime Video
        "in.startv.hotstar",
        "in.mohalla.sharechat",
        "com.king.candycrushsaga",
        "com.supercell.clashofclans",
        "com.supercell.clashroyale",
        "com.roblox.client",
        "com.tencent.ig", // PUBG Mobile
        "com.pubg.imobile", // BGMI
        "com.dts.freefireth",
    )
}
