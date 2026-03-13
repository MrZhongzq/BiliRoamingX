package app.revanced.patches.bilibili.video.player.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.video.player.fingerprints.IjkMediaPlayerOptionsFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction22c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Force hardware codec",
    description = "强制硬件解码",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object HwCodecPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        // Disabled for v8.85.0+: the fingerprint matches wrong methods causing VerifyError
        // The IjkMediaPlayer options method no longer has the expected ()V signature
        return
        @Suppress("UNREACHABLE_CODE")
        val result = IjkMediaPlayerOptionsFingerprint.result ?: return
        val method = result.mutableMethod
        // Only proceed if the matched method is in the expected IjkMediaPlayer package
        if (!result.classDef.type.contains("ijk/media/player")) return
        // Verify the matched method actually contains the expected references
        val hasSetOptionBundle = method.implementation!!.instructions.any { inst ->
            inst.opcode == Opcode.INVOKE_INTERFACE && runCatching {
                val mr = (inst as BuilderInstruction35c).reference as MethodReference
                mr.returnType == "V" && mr.parameterTypes == listOf("I", "Landroid/os/Bundle;")
            }.getOrDefault(false)
        }
        val hasEnableHwCodec = method.implementation!!.instructions.any { inst ->
            inst.opcode == Opcode.IGET_BOOLEAN && runCatching {
                (inst as BuilderInstruction22c).reference.toString() == "Ltv/danmaku/ijk/media/player/IjkMediaConfigParams;->mEnableHwCodec:Z"
            }.getOrDefault(false)
        }
        if (!hasSetOptionBundle && !hasEnableHwCodec) return // Wrong method matched, skip

        var indexOffset = 0
        method.run {
            if (hasSetOptionBundle) {
                implementation!!.instructions.withIndex().filter { (_, inst) ->
                    inst.opcode == Opcode.INVOKE_INTERFACE && (inst as BuilderInstruction35c).let {
                        val mr = it.reference as MethodReference
                        mr.returnType == "V" && mr.parameterTypes == listOf("I", "Landroid/os/Bundle;")
                    }
                }.map { (index, inst) ->
                    index to (inst as BuilderInstruction35c).registerE
                }.forEach { (index, register) ->
                    addInstruction(
                        index + (indexOffset++), """
                        invoke-static {v$register}, Lapp/revanced/bilibili/patches/HwCodecPatch;->printOptionBundle(Landroid/os/Bundle;)V
                    """.trimIndent()
                    )
                }
            }
            if (hasEnableHwCodec) {
                indexOffset = 0
                implementation!!.instructions.withIndex().filter { (_, inst) ->
                    inst.opcode == Opcode.IGET_BOOLEAN && (inst as BuilderInstruction22c).let {
                        it.reference.toString() == "Ltv/danmaku/ijk/media/player/IjkMediaConfigParams;->mEnableHwCodec:Z"
                    }
                }.map { (index, inst) ->
                    (index + 1) to (inst as BuilderInstruction22c).registerA
                }.forEach { (index, register) ->
                    addInstructions(
                        index + indexOffset, """
                            invoke-static {v$register}, Lapp/revanced/bilibili/patches/HwCodecPatch;->enableHwCodec(Z)Z
                            move-result v$register
                        """.trimIndent()
                    )
                    indexOffset += 2
                }
            }
        }
    }
}
