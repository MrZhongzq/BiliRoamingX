package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.misc.other.fingerprints.LiveRoomTouchDispatchViewModelFingerprint
import app.revanced.util.exception
import app.revanced.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Forbid live room slide left",
    description = "禁止直播间向左滑动弹出抽屉",
    compatiblePackages = [CompatiblePackage(name = "tv.danmaku.bili"), CompatiblePackage(name = "com.bilibili.app.in")]
)
object ForbidLiveRoomSlideLeftPatch :
    BytecodePatch(setOf(LiveRoomTouchDispatchViewModelFingerprint)) {
    override fun execute(context: BytecodeContext) {
        LiveRoomTouchDispatchViewModelFingerprint.result?.mutableClass?.methods?.findTargetMethod()
            ?.addInstructionsWithLabels(
            0, """
            invoke-static {}, Lapp/revanced/bilibili/patches/LiveRoomPatch;->disableSlideLeft()Z
            move-result v0
            if-eqz v0, :do_nothing
            return-void
            :do_nothing
            nop
        """.trimIndent()
        ) ?: throw LiveRoomTouchDispatchViewModelFingerprint.exception
    }

    private fun Collection<app.revanced.patcher.util.proxy.mutableTypes.MutableMethod>.findTargetMethod() =
        firstOrNull { it.isTargetMethod() }
            ?: firstOrNull {
                it.parameterTypes.isEmpty() && it.returnType == "V" && it.name != "<clinit>" && it.name != "<init>"
            }

    private fun app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.isTargetMethod(): Boolean {
        if (parameterTypes.isNotEmpty() || returnType != "V" || name == "<clinit>" || name == "<init>")
            return false
        val instructions = implementation?.instructions ?: return false
        val hasTrueConst = instructions.any { inst ->
            inst.opcode == Opcode.SGET_OBJECT &&
                    inst.getReference<FieldReference>().toString() == "Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;"
        }
        val hasSetValue = instructions.any { inst ->
            inst.opcode == Opcode.INVOKE_VIRTUAL &&
                    inst.getReference<MethodReference>().toString() ==
                    "Lcom/bilibili/bililive/infra/arch/jetpack/liveData/SafeMutableLiveData;->setValue(Ljava/lang/Object;)V"
        }
        return hasTrueConst && hasSetValue
    }
}
