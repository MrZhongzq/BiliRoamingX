package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.other.fingerprints.CommentConfigFingerprint
import app.revanced.patches.bilibili.utils.classDescriptor
import app.revanced.patches.bilibili.utils.isAbstract
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Force comment time navigable",
    description = "允许分P及交互视频评论时间点可点击",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object ForceCommentNavigablePatch : BytecodePatch(fingerprints = setOf(CommentConfigFingerprint)) {
    override fun execute(context: BytecodeContext) {
        val primaryCommentMainFragmentType =
            "com.bilibili.app.comm.comment2.comments.view.PrimaryCommentMainFragment".classDescriptor
        val patchType = "app.revanced.bilibili.patches.ForceCommentNavigablePatch".classDescriptor
        val baseFragmentType = "com.bilibili.lib.ui.BaseFragment".classDescriptor
        context.findClass(primaryCommentMainFragmentType)?.mutableClass?.methods?.first {
            it.name == "onCreate" && it.parameterTypes == listOf("Landroid/os/Bundle;")
        }?.addInstructions(
            0, """
            invoke-static {p0}, $patchType->onPrimaryCommentMainFragmentCreate($baseFragmentType)V
        """.trimIndent()
        ) ?: throw PatchException("not found PrimaryCommentMainFragment")
        CommentConfigFingerprint.result?.run {
            val index = scanResult.stringsScanResult!!.matches.last().index
            val allInstructions = mutableMethod.getInstructions().toList()
            // Find the IGET_BOOLEAN for seekEnabled field
            // Strategy: find append(Z) call right after "seekEnabled=" string,
            // get its register, then find the IGET_BOOLEAN that loaded that register
            val seekEnabledField = run findField@{
                // First try: look for IGET_BOOLEAN after the string (old layout)
                allInstructions.withIndex().firstNotNullOfOrNull { (i, inst) ->
                    if (i > index && inst.opcode == Opcode.IGET_BOOLEAN)
                        inst.getReference<FieldReference>()
                    else null
                }?.let { return@findField it }
                // v8.85.0+: fields loaded before strings, find append(Z) after seekEnabled string
                val appendIdx = (index + 1 until allInstructions.size).firstOrNull { i ->
                    val inst = allInstructions[i]
                    inst.opcode == Opcode.INVOKE_VIRTUAL && inst.getReference<MethodReference>()?.let {
                        it.name == "append" && it.parameterTypes == listOf("Z")
                    } == true
                } ?: return
                val appendInst = allInstructions[appendIdx] as Instruction35c
                val boolReg = appendInst.registerD
                // Find IGET_BOOLEAN that stored into this register
                allInstructions.withIndex().filter { (i, inst) ->
                    i < index && inst.opcode == Opcode.IGET_BOOLEAN
                }.lastOrNull { (_, inst) ->
                    (inst as? TwoRegisterInstruction)?.registerA == boolReg
                }?.let { (_, inst) -> inst.getReference<FieldReference>() }
            } ?: return
            mutableClass.methods.first { m ->
                m.parameterTypes.isEmpty() && m.returnType == "Z" && !m.accessFlags.isAbstract()
                        && m.getInstruction(0).let {
                    if (it.opcode == Opcode.IGET_BOOLEAN) it.getReference<FieldReference>() else null
                } == seekEnabledField
            }.addInstructionsWithLabels(
                0, """
                invoke-static {}, $patchType->enabled()Z
                move-result v0
                if-eqz v0, :jump
                const/4 v0, 0x1
                return v0
                :jump
                nop
            """.trimIndent()
            )
        }
    }
}
