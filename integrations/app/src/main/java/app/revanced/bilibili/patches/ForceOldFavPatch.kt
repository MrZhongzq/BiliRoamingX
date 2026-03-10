package app.revanced.bilibili.patches

import android.view.View
import androidx.annotation.Keep
import app.revanced.bilibili.utils.Reflex

object ForceOldFavPatch {
    @Keep
    @JvmStatic
    fun onNewStoryFavWidget(view: View) {
        if (!SettingsTransfer.forceOldFav())
            return
        val listenerInfo = runCatching {
            Reflex.getObjectField<Any>(view, "mListenerInfo")
        }.getOrNull() ?: return
        val clickListener = runCatching {
            Reflex.getObjectField<View.OnClickListener>(listenerInfo, "mOnClickListener")
        }.getOrNull()
        val longClickListener = runCatching {
            Reflex.getObjectField<View.OnLongClickListener>(listenerInfo, "mOnLongClickListener")
        }.getOrNull()
        view.setOnClickListener { target ->
            longClickListener?.onLongClick(target)
        }
        view.setOnLongClickListener { target ->
            clickListener?.onClick(target)
            clickListener != null
        }
    }
}
