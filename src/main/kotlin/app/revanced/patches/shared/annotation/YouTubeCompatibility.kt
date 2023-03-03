package app.revanced.patches.shared.annotation

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility([Package("com.google.android.youtube", arrayOf("18.06.41", "18.07.35", "18.08.38"))])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class YouTubeCompatibility

