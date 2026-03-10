package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Patch(
    name = "Block player follow",
    description = "允许隐藏播放器内关注按钮",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object BlockPlayerFollowPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        val clazz = context.findClass("Lcom/bilibili/app/gemini/player/widget/follow/GeminiPlayerFollowWithFaceWidget;")
            ?.mutableClass ?: return
        val method = clazz.methods.firstOrNull { method ->
            !AccessFlags.STATIC.isSet(method.accessFlags)
                && method.returnType == "Z"
                && method.implementation?.instructions?.any { inst ->
                    (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO)
                        && inst.getReference<StringReference>().string.contains("mAuthorInfo ")
                } == true
        } ?: throw PatchException("not found GeminiPlayerFollowWithFaceWidget availability method")
        method.addInstructionsWithLabels(
            0, """
            invoke-static {}, Lapp/revanced/bilibili/patches/SettingsTransfer;->showBlockPlayerFollow()Z
            move-result v0
            if-eqz v0, :jump
            const/4 v0, 0x0
            return v0
            :jump
            nop
        """.trimIndent()
        )
    }
}
