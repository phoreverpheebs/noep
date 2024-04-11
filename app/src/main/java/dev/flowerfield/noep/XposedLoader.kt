package dev.flowerfield.noep

import android.graphics.Bitmap
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.findConstructorBestMatch
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedLoader : IXposedHookLoadPackage {
    val featRegex = Regex("\\s?(?:\\(|\\[)[fF]eat(?:\\.|(?:uring)) ([^\\)|\\]]+)(?:\\)|\\])")
    val parenRegex1 = Regex("\\([^\\)]+\\)(?: \\[[^\\]]+\\])?")
    val parenRegex2 = Regex("\\[([^\\]]+)\\]")

    fun fixBrackets(title: String): String {
        if (title.indexOfAny("([".toCharArray()) == -1) {
            return title
        }

        if (parenRegex1.matches(title)) {
            return title
        }

        val match: MatchResult = parenRegex2.find(title)!!

        return title.replace(match.groupValues[0], "(${match.groupValues[1]})")
    }

    override fun handleLoadPackage(p0: XC_LoadPackage.LoadPackageParam) {
        if (p0.packageName != "fm.last.android")
            return

        XposedBridge.log("last.fm hooked");

        val Track = findClass("fm.last.android.scrobbler.scrobble.models.Track", p0.classLoader)

        val constructor = findConstructorBestMatch(
            Track,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Long::class.java,
            Bitmap::class.java
        )

        hookMethod(constructor, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam<*>) {
                val trackTitle: String = param.args[0]!! as String

                var newTrackTitle = trackTitle

                try {
                    newTrackTitle = fixBrackets(featRegex.replace(trackTitle, ""))
                } catch (e: NullPointerException) {
                }

                if (newTrackTitle != trackTitle) {
                    XposedBridge.log("Changed track title ${trackTitle} to ${newTrackTitle}")
                }

                param.args[0] = newTrackTitle
            }
        })

        findAndHookMethod(Track, "setAlbum", String::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam<*>) {
                val albumTitle: String = param.args[0] as String

                var newAlbumTitle = albumTitle.removeSuffix(" - EP").removeSuffix(" - Single")

                try {
                    newAlbumTitle = fixBrackets(featRegex.replace(newAlbumTitle, ""))
                } catch (e: NullPointerException) {
                }

                if (newAlbumTitle != albumTitle) {
                    XposedBridge.log("Changed album title ${albumTitle} to ${newAlbumTitle}")
                }

                param.args[0] = newAlbumTitle
            }
        })
    }
}
