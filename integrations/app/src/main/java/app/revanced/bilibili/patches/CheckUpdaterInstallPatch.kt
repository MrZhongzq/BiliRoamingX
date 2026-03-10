package app.revanced.bilibili.patches

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import app.revanced.bilibili.utils.Logger
import app.revanced.bilibili.utils.Utils
import app.revanced.bilibili.utils.sha256Hex
import java.io.File

object CheckUpdaterInstallPatch {
    @Keep
    @JvmStatic
    fun onCheck(context: Context, className: String, methodName: String) {
        if (Build.VERSION.SDK_INT < 29)
            return
        Utils.async {
            try {
                val prefs = Utils.blkvPrefsByName("apk_store_info", true)
                val apkPath = prefs.getString("apk_path", "").orEmpty()
                if (apkPath.isEmpty())
                    return@async
                val apkFile = File(apkPath)
                if (!apkFile.isFile) {
                    prefs.edit().clear().apply()
                    return@async
                }
                val currentApk = File(Utils.getContext().applicationInfo.sourceDir)
                val candidateHash = apkFile.sha256Hex
                val currentHash = currentApk.sha256Hex
                if (candidateHash.isNotEmpty() && candidateHash == currentHash) {
                    Logger.debug { "CheckUpdaterInstallPatch found same apk file, delete cached installer package" }
                    apkFile.delete()
                    prefs.edit().clear().apply()
                    return@async
                }
                Logger.debug { "CheckUpdaterInstallPatch found new apk file, keep original install check flow" }
                Utils.runOnMainThread {
                    invokeOrigin(className, methodName, context)
                }
            } catch (t: Throwable) {
                Logger.error(t) { "CheckUpdaterInstallPatch failed" }
            }
        }
    }

    private fun invokeOrigin(className: String, methodName: String, context: Context) {
        try {
            Class.forName(className)
                .getDeclaredMethod(methodName, Context::class.java)
                .apply { isAccessible = true }
                .invoke(null, context)
        } catch (t: Throwable) {
            Logger.error(t) { "CheckUpdaterInstallPatch failed to invoke $className#$methodName" }
        }
    }
}
