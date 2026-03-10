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
        val clazz = context.findClass("Ltv/danmaku/bili/update/utils/h;")?.mutableClass
            ?: throw PatchException("not found updater helper class")
        val method = clazz.methods.firstOrNull {
            it.name == "d" && it.returnType == "V" && it.parameterTypes == listOf("Landroid/content/Context;")
        } ?: throw PatchException("not found updater check method")
        if (clazz.methods.any {
                it.name == "d_Origin" && it.returnType == "V" && it.parameterTypes == listOf("Landroid/content/Context;")
            })
            return
        method.name = "d_Origin"
        method.cloneMutable(registerCount = 3, clearImplementation = true, name = "d").apply {
            addInstructions(
                """
                const-string v0, "tv.danmaku.bili.update.utils.h"
                const-string v1, "d_Origin"
                invoke-static {p0, v0, v1}, Lapp/revanced/bilibili/patches/CheckUpdaterInstallPatch;->onCheck(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
                return-void
                """.trimIndent()
            )
        }.also { clazz.methods.add(it) }
    }
}
