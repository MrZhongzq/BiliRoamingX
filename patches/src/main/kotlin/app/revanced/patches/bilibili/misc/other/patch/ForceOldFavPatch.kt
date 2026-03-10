package app.revanced.patches.bilibili.misc.other.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch

@Patch(
    name = "Force old favorite",
    description = "恢复竖屏视频旧版收藏交互",
    compatiblePackages = [
        CompatiblePackage(name = "tv.danmaku.bili"),
        CompatiblePackage(name = "tv.danmaku.bilibilihd"),
        CompatiblePackage(name = "com.bilibili.app.in")
    ]
)
object ForceOldFavPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {
        context.findClass("Lcom/bilibili/video/story/action/widget/StoryFavoriteWidget;")
            ?.mutableClass
            ?.methods
            ?.filter { it.name == "<init>" && it.implementation != null }
            ?.forEach { method ->
                method.addInstruction(
                    method.implementation!!.instructions.size - 1, """
                    invoke-static {p0}, Lapp/revanced/bilibili/patches/ForceOldFavPatch;->onNewStoryFavWidget(Landroid/view/View;)V
                """.trimIndent()
                )
            }
    }
}
