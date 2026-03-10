package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.utils.proxy
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.StringReference

@Patch(
    name = "Always mini play",
    description = "鍏佽 Theseus 椤甸潰鎬绘槸灏忕獥鎾斁",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object TheseusSuppressorPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        val suppressorClass = context.classes.firstOrNull { classDef ->
            classDef.methods.any { method ->
                method.returnType == "V"
                    && method.parameterTypes == listOf("Ljava/lang/Object;")
                    && method.implementation?.instructions?.any { inst ->
                        (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO)
                            && inst.getReference<StringReference>().string in setOf("doSuppressionWith:", "undoSuppressionOf:")
                    } == true
            }
        }?.proxy(context) ?: throw PatchException("not found Theseus suppressor class")

        val doMethod = suppressorClass.methods.firstOrNull { method ->
            method.returnType == "V"
                && method.parameterTypes == listOf("Ljava/lang/Object;")
                && method.implementation?.instructions?.any { inst ->
                    (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO)
                        && inst.getReference<StringReference>().string == "doSuppressionWith:"
                } == true
        } ?: throw PatchException("not found Theseus doSuppression method")
        doMethod.addInstructionsWithLabels(
            0, """
            invoke-static {p1}, Lapp/revanced/bilibili/patches/TheseusSuppressorPatch;->shouldDoSuppression(Ljava/lang/Object;)Z
            move-result v0
            if-nez v0, :jump
            return-void
            :jump
            nop
        """.trimIndent()
        )

        val undoMethod = suppressorClass.methods.firstOrNull { method ->
            method.returnType == "V"
                && method.parameterTypes == listOf("Ljava/lang/Object;")
                && method.implementation?.instructions?.any { inst ->
                    (inst.opcode == Opcode.CONST_STRING || inst.opcode == Opcode.CONST_STRING_JUMBO)
                        && inst.getReference<StringReference>().string == "undoSuppressionOf:"
                } == true
        } ?: throw PatchException("not found Theseus undoSuppression method")
        undoMethod.addInstructionsWithLabels(
            0, """
            invoke-static {p1}, Lapp/revanced/bilibili/patches/TheseusSuppressorPatch;->shouldUndoSuppression(Ljava/lang/Object;)Z
            move-result v0
            if-nez v0, :jump
            return-void
            :jump
            nop
        """.trimIndent()
        )
    }
}
