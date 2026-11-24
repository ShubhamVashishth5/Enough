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

    /** The websites of those apps, so the browser isn't a way round once the app is in the Vault. */
    val websites: Map<String, List<String>> = mapOf(
        "com.instagram.android" to listOf("instagram.com"),
        "com.instagram.barcelona" to listOf("threads.net", "threads.com"),
        "com.google.android.youtube" to listOf("youtube.com"),
        "com.facebook.katana" to listOf("facebook.com"),
        "com.facebook.lite" to listOf("facebook.com"),
        "com.twitter.android" to listOf("x.com", "twitter.com"),
        "com.reddit.frontpage" to listOf("reddit.com"),
        "com.zhiliaoapp.musically" to listOf("tiktok.com"),
        "com.ss.android.ugc.trill" to listOf("tiktok.com"),
        "com.pinterest" to listOf("pinterest.com"),
        "tv.twitch.android.app" to listOf("twitch.tv"),
        "com.netflix.mediaclient" to listOf("netflix.com"),
        "com.amazon.avod.thirdpartyclient" to listOf("primevideo.com"),
        "in.startv.hotstar" to listOf("hotstar.com"),
    )
}
