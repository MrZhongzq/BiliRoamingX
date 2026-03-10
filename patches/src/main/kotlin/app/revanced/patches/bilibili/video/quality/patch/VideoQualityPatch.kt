package app.revanced.patches.bilibili.video.quality.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.bilibili.utils.cloneMutable
import app.revanced.patches.bilibili.utils.proxy
import app.revanced.patches.bilibili.video.quality.fingerprints.PlayerSettingHelperFingerprint
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Patch(
    name = "Video default quality",
    description = "视频默认画质设置",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object VideoQualityPatch : BytecodePatch(setOf(PlayerSettingHelperFingerprint)) {
    override fun execute(context: BytecodeContext) {
        var defaultQnMethod: Method? = null
        PlayerSettingHelperFingerprint.result?.also {
            defaultQnMethod = it.method
        }?.mutableMethod?.addInstructionsWithLabels(
            0, """
            invoke-static {}, Lapp/revanced/bilibili/patches/VideoQualityPatch;->getMatchedFullScreenQuality()I
            move-result v0
            if-eqz v0, :jump
            return v0
            :jump
            nop
        """.trimIndent()
        ) ?: throw PlayerSettingHelperFingerprint.exception
        context.findClass("Lapp/revanced/bilibili/patches/VideoQualityPatch;")!!.mutableClass.run {
            methods.first { it.name == "defaultQn" }.also { methods.remove(it) }
                .cloneMutable(registerCount = 1, clearImplementation = true).apply {
                    addInstructions(
                        """
                        invoke-static {}, $defaultQnMethod
                        move-result v0
                        return v0
                    """.trimIndent()
                    )
                }.also { methods.add(it) }
        }
        patchPlayerSettingService(context)
        patchUseRecommendedQn(context)
        patchMiniPlayerQuality(context)
        patchQualityAdapter(
            context,
            "Lcom/bilibili/playerbizcommonv2/widget/quality/q0;",
            "J0"
        )
        patchQualityAdapter(
            context,
            "Lcom/mall/videodetail/vd/united/page/videoquality/o;",
            "L0"
        )
    }

    private fun patchPlayerSettingService(context: BytecodeContext) {
        val clazz = context.classes.firstOrNull { classDef ->
            classDef.type.startsWith("Ltv/danmaku/biliplayerv2/service/setting/IPlayerSettingService")
                && classDef.methods.any { method ->
                method.name == "getFromPref"
                    && method.parameterTypes == listOf(
                    "Landroid/content/SharedPreferences;",
                    "Ljava/lang/String;",
                    "Ljava/lang/Class;",
                    "Ljava/lang/Object;"
                )
            }
        }?.proxy(context) ?: return
        if (clazz.methods.any {
                it.name == "getFromPref_Origin" && it.parameterTypes == listOf(
                    "Landroid/content/SharedPreferences;",
                    "Ljava/lang/String;",
                    "Ljava/lang/Class;",
                    "Ljava/lang/Object;"
                )
            })
            return
        val method = clazz.methods.firstOrNull {
            it.name == "getFromPref" && it.parameterTypes == listOf(
                "Landroid/content/SharedPreferences;",
                "Ljava/lang/String;",
                "Ljava/lang/Class;",
                "Ljava/lang/Object;"
            )
        } ?: throw PatchException("not found player setting getFromPref")
        method.name = "getFromPref_Origin"
        method.cloneMutable(
            registerCount = maxOf(6, method.implementation?.registerCount ?: 0),
            clearImplementation = true,
            name = "getFromPref"
        ).apply {
            addInstructions(
                """
                invoke-virtual {p0, p1, p2, p3, p4}, $method
                move-result-object v0
                invoke-static {p2, v0}, Lapp/revanced/bilibili/patches/VideoQualityPatch;->onPlayerGetValueFromPref(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v0
                return-object v0
                """.trimIndent()
            )
        }.also { clazz.methods.add(it) }
    }

    private fun patchUseRecommendedQn(context: BytecodeContext) {
        val method = context.findClass("Ltv/danmaku/ijk/media/player/IjkMediaPlayerItem;")
            ?.mutableClass
            ?.methods
            ?.firstOrNull {
                it.name == "setRecommendedQn"
                    && it.returnType == "V"
                    && it.parameterTypes == listOf("Z", "I", "I")
            } ?: return
        if (method.implementation?.instructions?.any { it.toString().contains("useRecommendedQn") } == true)
            return
        method.addInstructions(
            0, """
            invoke-static {p1}, Lapp/revanced/bilibili/patches/VideoQualityPatch;->useRecommendedQn(Z)Z
            move-result p1
        """.trimIndent()
        )
    }

    private fun patchMiniPlayerQuality(context: BytecodeContext) {
        val method = context.findClass("Lcom/bilibili/mini/player/biz/DefaultMiniPlayerBizManager;")
            ?.mutableClass
            ?.methods
            ?.firstOrNull { candidate ->
                candidate.implementation?.instructions?.any { inst ->
                    inst is Instruction35c
                        && (inst.reference as? MethodReference)?.let { ref ->
                        ref.name == "setMaxQuality" && ref.parameterTypes == listOf("I")
                    } == true
                } == true
            } ?: return
        if (method.implementation?.instructions?.any { it.toString().contains("getMiniMaxQuality") } == true)
            return
        val (index, qualityRegister) = method.implementation!!.instructions.withIndex().firstNotNullOfOrNull { (index, inst) ->
            if (inst !is Instruction35c) return@firstNotNullOfOrNull null
            val ref = inst.reference as? MethodReference ?: return@firstNotNullOfOrNull null
            if (ref.name == "setMaxQuality" && ref.parameterTypes == listOf("I") && inst.registerCount >= 2)
                index to inst.registerD
            else null
        } ?: throw PatchException("not found mini player max quality invoke")
        method.addInstructions(
            index, """
            invoke-static {v$qualityRegister}, Lapp/revanced/bilibili/patches/VideoQualityPatch;->getMiniMaxQuality(I)I
            move-result v$qualityRegister
        """.trimIndent()
        )
    }

    private fun patchQualityAdapter(
        context: BytecodeContext,
        className: String,
        methodName: String
    ) {
        val clazz = context.findClass(className)?.mutableClass ?: return
        if (clazz.methods.any { it.name == "${methodName}_Origin" })
            return
        val method = clazz.methods.firstOrNull {
            it.name == methodName
                && it.returnType == "V"
                && it.parameterTypes.size == 5
        } ?: return
        val originMethod = method.cloneMutable(name = "${methodName}_Origin")
            .also { clazz.methods.add(it) }
        clazz.methods.remove(method)
        method.cloneMutable(
            registerCount = maxOf(6, method.implementation?.registerCount ?: 0),
            clearImplementation = true
        ).apply {
            addInstructions(
                """
                invoke-virtual/range {p0 .. p5}, $originMethod
                invoke-static {p0}, Lapp/revanced/bilibili/patches/VideoQualityPatch;->onUpdateQualityAdapter(Ljava/lang/Object;)V
                return-void
                """.trimIndent()
            )
        }.also { clazz.methods.add(it) }
    }
}
