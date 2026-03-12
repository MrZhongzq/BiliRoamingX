package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    name = "Resource cleanup",
    description = "Clean up invalid resource entries that prevent recompilation on v8.85.0+",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object ResourceCleanupPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {
        // Remove resource entries with $ in names from public.xml only
        // The $ character is invalid for aapt2 resource entry names
        // Actual drawable files are kept - they just won't have public visibility
        val publicXml = context["res/values/public.xml", false]
        if (publicXml.exists()) {
            val lines = publicXml.readLines()
            val cleaned = lines.filter { line ->
                !line.contains("\"\$")
            }
            publicXml.writeText(cleaned.joinToString("\n"))
        }
        // Remove unsupported Android 15+ attributes from layouts
        // The bundled aapt2 may not support newer platform attributes
        val resDir = context["res", false]
        if (resDir.exists() && resDir.isDirectory) {
            resDir.walk().filter { it.isFile && it.extension == "xml" }.forEach { xmlFile ->
                val content = xmlFile.readText()
                if (content.contains("android:useLocalePreferredLineHeightForMinimum")) {
                    xmlFile.writeText(content.replace(
                        Regex("""android:useLocalePreferredLineHeightForMinimum="[^"]*"\s*"""), ""
                    ))
                }
            }
        }
    }
}
