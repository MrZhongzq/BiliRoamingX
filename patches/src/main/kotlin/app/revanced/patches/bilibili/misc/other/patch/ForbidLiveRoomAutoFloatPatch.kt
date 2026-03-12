package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.bilibili.misc.other.fingerprints.LiveRoomSetFloatWindowFingerprint
import app.revanced.patches.bilibili.utils.isAbstract
import app.revanced.patches.bilibili.utils.isNative
import app.revanced.util.exception
import app.revanced.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction11n
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

@Patch(
    name = "Forbid live room auto float",
    description = "禁止直播间点小窗播放时自动开启“播放被中断时自动小窗播放”",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object ForbidLiveRoomAutoFloatPatch : BytecodePatch(setOf(LiveRoomSetFloatWindowFingerprint)) {
    override fun execute(context: BytecodeContext) {
        val (setClass, setMethod) = LiveRoomSetFloatWindowFingerprint.result?.let {
            it.classDef to it.method
        } ?: throw LiveRoomSetFloatWindowFingerprint.exception
        val iSetMethodSign = setMethod.toMutable().apply {
            definingClass = setClass.interfaces.first()
        }.toString()
        val targets = context.classes.asSequence().flatMap { it.methods }.filter { m ->
            m.accessFlags.let { !it.isAbstract() && !it.isNative() }
                    && m.parameterTypes.isEmpty() && m.returnType == "V"
        }.mapNotNull { m ->
            val instructions = m.implementation!!.instructions.toList()
            val matches = instructions.withIndex().mapNotNull { (index, inst) ->
                if (inst.opcode == Opcode.INVOKE_INTERFACE && (inst as Instruction35c).reference.toString() == iSetMethodSign) {
                    // v8.85.0+: pattern is IF_EQZ, INVOKE_INTERFACE (no CONST_4 in between)
                    // older: pattern is IF_EQZ, CONST_4, INVOKE_INTERFACE
                    val prev1 = instructions.getOrNull(index - 1)
                    val prev2 = instructions.getOrNull(index - 2)
                    when {
                        prev2?.opcode == Opcode.IF_EQZ && prev1?.opcode == Opcode.CONST_4 && prev1 is Instruction11n ->
                            index to prev1.registerA
                        prev1?.opcode == Opcode.IF_EQZ ->
                            index to inst.registerD
                        else -> null
                    }
                } else null
            }
            matches.takeIf { it.isNotEmpty() }?.let { m to it }
        }.toList()

        if (targets.isEmpty())
            throw PatchException("not found startMiniFloatPlay method")

        targets.forEach { (method, matches) ->
            context.findClass(method.definingClass)!!.mutableClass.findMutableMethodOf(method).run {
                matches.sortedByDescending { it.first }.forEach { (index, register) ->
                addInstructionsWithLabels(
                    index - 1, """
                    invoke-static {}, Lapp/revanced/bilibili/patches/LiveRoomPatch;->disableAutoFloat()Z
                    move-result v$register
                    if-nez v$register, :next
                """.trimIndent(),
                    ExternalLabel("next", getInstruction(index + 1))
                )
            }
            }
        }
    }
}
