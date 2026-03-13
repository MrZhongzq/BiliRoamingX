package app.revanced.patches.all.misc.packagename

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patcher.patch.options.PatchOptionException
import app.revanced.util.get
import app.revanced.util.set
import java.io.Closeable

@Patch(
    name = "Change package name",
    description = "Changes the package name to org.qingye.bilibili by default, including provider authorities and permissions.",
    use = true
)
@Suppress("unused")
object ChangePackageNamePatch : ResourcePatch(), Closeable {
    private val packageNameOption = stringPatchOption(
        key = "packageName",
        default = "org.qingye.bilibili",
        values = mapOf(
            "QingYe" to "org.qingye.bilibili",
            "Default (append .revanced)" to "Default"
        ),
        title = "Package name",
        description = "The name of the package to rename the app to.",
        required = true
    ) {
        it == "Default" || it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+\$"))
    }

    private lateinit var context: ResourceContext

    override fun execute(context: ResourceContext) {
        this.context = context
    }

    /**
     * Set the package name to use.
     * If this is called multiple times, the first call will set the package name.
     *
     * @param fallbackPackageName The package name to use if the user has not already specified a package name.
     * @return The package name that was set.
     * @throws PatchOptionException.ValueValidationException If the package name is invalid.
     */
    fun setOrGetFallbackPackageName(fallbackPackageName: String): String {
        val packageName = packageNameOption.value!!

        return if (packageName == packageNameOption.default)
            fallbackPackageName.also { packageNameOption.value = it }
        else
            packageName
    }

    override fun close() = context.document["AndroidManifest.xml"].use { dom ->
        val packageName = packageNameOption.value
        val oldPackageName = dom["manifest"]["package"]
        val newPackageName = if (!packageName.isNullOrEmpty()
            && packageName != packageNameOption.default
        ) packageName else "$oldPackageName.revanced"
        dom["manifest"]["package"] = newPackageName
        // Also rename provider authorities, permissions, and other references
        // to avoid conflicts with original app
        if (newPackageName != oldPackageName) {
            dom.walk { node ->
                node.attributes?.run {
                    for (i in 0 until length) {
                        val attr = item(i)
                        if (attr.nodeValue.contains(oldPackageName)) {
                            // Replace in authorities, permissions, and permission declarations
                            val name = attr.nodeName
                            if (name == "android:authorities" ||
                                name == "android:permission" ||
                                name == "android:readPermission" ||
                                name == "android:writePermission" ||
                                (name == "android:name" && node.nodeName == "permission" && attr.nodeValue.contains(oldPackageName)) ||
                                (name == "android:name" && node.nodeName == "uses-permission" && attr.nodeValue.contains(oldPackageName))
                            ) {
                                attr.nodeValue = attr.nodeValue.replace(oldPackageName, newPackageName)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun org.w3c.dom.Document.walk(action: (org.w3c.dom.Node) -> Unit) {
        fun visit(node: org.w3c.dom.Node) {
            action(node)
            val children = node.childNodes
            for (i in 0 until children.length) visit(children.item(i))
        }
        visit(documentElement)
    }
}
