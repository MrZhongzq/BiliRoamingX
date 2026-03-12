package app.revanced.patches.bilibili.video.subtitle.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object RecordSelectedSubtitleFingerprint : MethodFingerprint(
    strings = listOf(
        "danmaku_subtitle_multi",
    ),
    returnType = "V",
)
