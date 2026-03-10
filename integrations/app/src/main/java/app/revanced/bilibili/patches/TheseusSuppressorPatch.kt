package app.revanced.bilibili.patches

import androidx.annotation.Keep
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.utils.Logger

object TheseusSuppressorPatch {
    @Keep
    @JvmStatic
    fun shouldDoSuppression(suppressor: Any?): Boolean {
        if (Settings.Debug()) {
            Logger.debug {
                "TheseusSuppressorPatch.shouldDoSuppression suppressor=$suppressor type=${suppressor?.javaClass?.name}"
            }
        }
        return !(Settings.AlwaysMiniPlay() && suppressor == "autoMiniPlayerSettingSuppressor")
    }

    @Keep
    @JvmStatic
    fun shouldUndoSuppression(suppressor: Any?): Boolean {
        if (Settings.Debug()) {
            Logger.debug {
                "TheseusSuppressorPatch.shouldUndoSuppression suppressor=$suppressor type=${suppressor?.javaClass?.name}"
            }
        }
        return true
    }
}
