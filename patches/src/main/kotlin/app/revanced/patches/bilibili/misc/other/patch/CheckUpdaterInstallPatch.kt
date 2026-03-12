package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.utils.cloneMutable

@Patch(
    name = "Check updater install",
    description = "淇 Android 10+ 鏇存柊瀹夎媯€鏌ラ€昏緫",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object CheckUpdaterInstallPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        // Find updater helper class dynamically - obfuscated name changes between versions
        val clazz = context.findClass("Ltv/danmaku/bili/update/utils/h;")?.mutableClass
            ?: context.classes.firstOrNull { classDef ->
                classDef.type.startsWith("Ltv/danmaku/bili/update/utils/")
                    && classDef.methods.any { it.returnType == "V" && it.parameterTypes == listOf("Landroid/content/Context;") }
            }?.let { context.findClass(it.type)?.mutableClass }
            ?: return // Skip patch if class not found
        val method = clazz.methods.firstOrNull {
            it.returnType == "V" && it.parameterTypes == listOf("Landroid/content/Context;")
                && it.implementation != null
        } ?: return // Skip patch if method not found
        val origName = method.name
        val originName = "${origName}_Origin"
        if (clazz.methods.any {
                it.name == originName && it.returnType == "V" && it.parameterTypes == listOf("Landroid/content/Context;")
            })
            return
        val className = clazz.type.substring(1, clazz.type.length - 1).replace('/', '.')
        method.name = originName
        method.cloneMutable(registerCount = 3, clearImplementation = true, name = origName).apply {
            addInstructions(
                """
                const-string v0, "$className"
                const-string v1, "$originName"
                invoke-static {p0, v0, v1}, Lapp/revanced/bilibili/patches/CheckUpdaterInstallPatch;->onCheck(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
                return-void
                """.trimIndent()
            )
        }.also { clazz.methods.add(it) }
    }
}
