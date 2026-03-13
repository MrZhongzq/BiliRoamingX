package app.revanced.bilibili.patches;

import android.os.Parcelable;

import androidx.annotation.Keep;

import com.bapis.bilibili.app.playerunite.v1.PlayViewUniteReq;
import com.bapis.bilibili.playershared.VideoVod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import app.revanced.bilibili.settings.Settings;
import app.revanced.bilibili.utils.Constants;
import app.revanced.bilibili.utils.KtUtils;

public class VideoQualityPatch {

    public static int halfScreenQuality() {
        String qualityStr = Settings.HalfScreenQuality.get();
        return Integer.parseInt(qualityStr);
    }

    public static int fullScreenQuality() {
        String qualityStr = Settings.FullScreenQuality.get();
        return Integer.parseInt(qualityStr);
    }

    public static int mobileFullScreenQuality() {
        String qualityStr = Settings.MobileFullScreenQuality.get();
        return Integer.parseInt(qualityStr);
    }

    @Keep
    public static int getMiniMaxQuality(int quality) {
        if (!Settings.MiniFollowFull.get()) {
            return quality;
        }
        int defaultQn = defaultQn();
        if (defaultQn <= 120) {
            return defaultQn;
        }
        return 120;
    }

    public static int getMiniMaxQuality() {
        return getMiniMaxQuality(0);
    }

    /**
     * fullscreen video quality
     * <p>
     * codes will filled by patcher
     */
    @Keep
    public static int defaultQn() {
        return 0;
    }

    /**
     * unlock 8k limit
     * <p>
     * for old player PGC
     */
    public static void unlockLimit(com.bapis.bilibili.pgc.gateway.player.v2.PlayViewReq playViewReq) {
        int halfScreenQuality = halfScreenQuality();
        int fulledScreenQuality = getMatchedFullScreenQuality();
        if (halfScreenQuality != 0 || fulledScreenQuality != 0) {
            playViewReq.setFnval(Constants.MAX_FNVAL);
            playViewReq.setFourk(true);
        }
    }

    /**
     * unlock 8k limit
     * <p>
     * for old player UGC
     */
    public static void unlockLimit(com.bapis.bilibili.app.playurl.v1.PlayViewReq playViewReq) {
        int halfScreenQuality = halfScreenQuality();
        int fulledScreenQuality = getMatchedFullScreenQuality();
        if (halfScreenQuality != 0 || fulledScreenQuality != 0) {
            playViewReq.setFnval(Constants.MAX_FNVAL);
            playViewReq.setFourk(true);
        }
    }

    /**
     * unlock 8k limit
     * <p>
     * for new unite(PGC + UGC) player
     */
    public static void unlockLimit(PlayViewUniteReq playViewReq) {
        VideoVod videoVod = playViewReq.getVod();
        int halfScreenQuality = halfScreenQuality();
        int fulledScreenQuality = getMatchedFullScreenQuality();
        if (halfScreenQuality != 0 || fulledScreenQuality != 0) {
            videoVod.setFnval(Constants.MAX_FNVAL);
            videoVod.setFourk(true);
        }
    }

    public static int getMatchedHalfScreenQuality() {
        int halfScreenQuality = halfScreenQuality();
        if (halfScreenQuality != 1) // not follow fullscreen quality
            return halfScreenQuality;
        return defaultQn();
    }

    @Keep
    public static int getMatchedFullScreenQuality() {
        return KtUtils.isWifiConnected() ? fullScreenQuality() : mobileFullScreenQuality();
    }

    @Keep
    public static Object onPlayerGetValueFromPref(String key, Object value) {
        int matchedFullScreenQuality;
        return (("pref_player_mediaSource_quality_wifi_key".equals(key)
            || "pref_story_player_mediaSource_quality_wifi_key".equals(key))
            && (matchedFullScreenQuality = getMatchedFullScreenQuality()) != 0)
            ? Integer.valueOf(matchedFullScreenQuality)
            : value;
    }

    @Keep
    public static boolean useRecommendedQn(boolean useRecommendedQn) {
        return halfScreenQuality() == 0 && useRecommendedQn;
    }

    @Keep
    public static void onUpdateQualityAdapter(Object adapter) throws IllegalAccessException {
        ArrayList<Object> qualityItems = findQualityItems(adapter);
        if (qualityItems == null || qualityItems.isEmpty()) {
            return;
        }
        Object first = qualityItems.get(0);
        if (first != null && isAutoQualityItem(first)) {
            qualityItems.remove(0);
            qualityItems.add(first);
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> findQualityItems(Object adapter) throws IllegalAccessException {
        for (Field field : adapter.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(adapter);
            if (!(value instanceof ArrayList)) {
                continue;
            }
            ArrayList<Object> items = (ArrayList<Object>) value;
            if (items.isEmpty()) {
                continue;
            }
            Object first = items.get(0);
            if (first == null || first instanceof Parcelable) {
                continue;
            }
            for (Field itemField : first.getClass().getDeclaredFields()) {
                if (Parcelable.class.isAssignableFrom(itemField.getType())) {
                    return items;
                }
            }
        }
        return null;
    }

    private static boolean isAutoQualityItem(Object item) throws IllegalAccessException {
        for (Field field : item.getClass().getDeclaredFields()) {
            if (field.getType() == Boolean.TYPE) {
                field.setAccessible(true);
                return field.getBoolean(item);
            }
        }
        return false;
    }
}
