package app.revanced.bilibili.patches

import androidx.annotation.Keep
import app.revanced.bilibili.patches.main.ApplicationDelegate
import app.revanced.bilibili.settings.Settings
import app.revanced.bilibili.utils.Utils
import app.revanced.bilibili.utils.Versions

object SettingsTransfer {
    @Keep
    @JvmStatic
    fun debuggable(): Boolean {
        if (!ApplicationDelegate.attached())
            return false
        return Settings.Debug()
    }

    @Keep
    @JvmStatic
    fun shouldShowCommentFollow(original: Boolean): Boolean {
        if (Settings.BlockFollowButton().contains("comment"))
            return false
        return original
    }

    @Keep
    @JvmStatic
    fun shouldShowDynamicFollow(original: Boolean): Boolean {
        if (Settings.BlockFollowButton().contains("dynamic"))
            return false
        return original
    }

    @Keep
    @JvmStatic
    fun fakeNotInMultiWindow(): Boolean {
        return Settings.FakeNotInMultiWindow()
    }

    @Keep
    @JvmStatic
    fun blockBackInMultiWindowMode(): Boolean {
        return Settings.BlockBackInMultiWindow()
    }

    @Keep
    @JvmStatic
    fun shouldAutoSubscribe(original: Boolean): Boolean {
        if (Settings.DisableAutoSubscribe())
            return false
        return original
    }

    @Keep
    @JvmStatic
    fun shouldAutoSelectOnce(original: Boolean): Boolean {
        if (Settings.DisableAutoSelect())
            return false
        return original
    }

    @Keep
    @JvmStatic
    fun disableAppendTrackingInfo(): Boolean {
        return Settings.PurifyShare()
    }

    @Keep
    @JvmStatic
    fun disableTeenagerDialog(): Boolean {
        return Settings.DisableTeenagerDialog()
    }

    @Keep
    @JvmStatic
    fun blockUpRcmdAds(): Boolean {
        return Settings.BlockUpRcmdAds()
    }

    @Keep
    @JvmStatic
    fun forceOldFav(): Boolean {
        if (!Settings.ForceOldFav())
            return false
        if (Versions.ge8_27_0())
            return true
        return Utils.isHd() && Versions.atLeast("2.2.0")
    }

    @Keep
    @JvmStatic
    fun showBlockPlayerFollow(): Boolean {
        return Settings.BlockFollowButton().contains("player")
    }

    @Keep
    @JvmStatic
    fun disallowCollectPrivacyInfo(): Boolean {
        return Settings.DisallowCollectPrivacyInfo()
    }

    @Keep
    @JvmStatic
    fun uidCopyNoPrefix(): Boolean {
        return Settings.UidCopyNoPrefix()
    }

    @Keep
    @JvmStatic
    fun delayDownloadModules(): Boolean {
        return Settings.DelayDownloadModules()
    }
}
