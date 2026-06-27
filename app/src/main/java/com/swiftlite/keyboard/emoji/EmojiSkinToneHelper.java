package com.swiftlite.keyboard.emoji;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class EmojiSkinToneHelper {

    public static String[] SKIN_TONES;

    private static final String ZWJ = "\u200D";

    private static Set<String> SIMPLE_TONE_BASES;

    private static Set<String> ZWJ_HUMAN_BASES;

    private static boolean sLoaded = false;

    public static void init(Context ctx) {
        if (sLoaded) return;
        try {
            String json = readAsset(ctx, "emoji_skin_tones.json");
            JSONObject obj = new JSONObject(json);

            JSONArray skinTones = obj.getJSONArray("skin_tones");
            SKIN_TONES = new String[skinTones.length()];
            for (int i = 0; i < skinTones.length(); i++) SKIN_TONES[i] = skinTones.getString(i);

            JSONArray simple = obj.getJSONArray("simple_tone_bases");
            SIMPLE_TONE_BASES = new HashSet<>();
            for (int i = 0; i < simple.length(); i++) SIMPLE_TONE_BASES.add(simple.getString(i));

            JSONArray zwj = obj.getJSONArray("zwj_human_bases");
            ZWJ_HUMAN_BASES = new HashSet<>();
            for (int i = 0; i < zwj.length(); i++) ZWJ_HUMAN_BASES.add(zwj.getString(i));

            sLoaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to load emoji_skin_tones.json", e);
        }
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        try (InputStream is = ctx.getAssets().open(path);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    public static boolean isToneSupportedEmoji(String emoji) {
        if (emoji == null || emoji.isEmpty()) return false;
        if (emoji.length() >= 2 && SIMPLE_TONE_BASES.contains(emoji.substring(0, 2))) return true;
        if (emoji.contains(ZWJ)) {
            for (String base : ZWJ_HUMAN_BASES) if (emoji.contains(base)) return true;
        }
        return false;
    }

    public static String applyTone(String base, String modifier) {
        if (modifier == null) return base;
        if (!base.contains(ZWJ)) return base + modifier;
        String[] parts = base.split(ZWJ, -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String seg = stripTone(parts[i]);
            sb.append(seg);
            if (seg.length() >= 2 && ZWJ_HUMAN_BASES.contains(seg.substring(0, 2))) sb.append(modifier);
            if (i < parts.length - 1) sb.append(ZWJ);
        }
        return sb.toString();
    }

    private static String stripTone(String seg) {
        for (String t : SKIN_TONES) if (seg.endsWith(t)) return seg.substring(0, seg.length() - t.length());
        return seg;
    }
}
