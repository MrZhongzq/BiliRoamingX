package app.revanced.patches.bilibili.video.player.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object PlayerSettingCreateSpeedFingerprint : MethodFingerprint(
    strings = listOf("option", "speed", "value"),
    customFingerprint = { methodDef, _ ->
        methodDef.name != "<clinit>"
    }
)
