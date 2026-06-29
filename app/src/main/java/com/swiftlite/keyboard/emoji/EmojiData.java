package com.swiftlite.keyboard.emoji;

import android.content.res.AssetManager;
import android.graphics.Paint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class EmojiData {

    private static final String ASSET_PATH = "emoji_data.json";

    public static volatile String[][] ALL;
    public static volatile String[] TAB_ICONS;

    private static final Object LOCK = new Object();

    public static void init(AssetManager assets) {
        if (ALL != null) return;
        synchronized (LOCK) {
            if (ALL != null) return;
            try {
                String json = readAsset(assets);
                JSONObject root = new JSONObject(json);
                JSONArray cats = root.getJSONArray("categories");

                int numCats = cats.length();
                String[][] all = new String[numCats][];
                for (int i = 0; i < numCats; i++) {
                    JSONArray cat = cats.getJSONArray(i);
                    String[] emojis = new String[cat.length()];
                    for (int j = 0; j < cat.length(); j++) {
                        emojis[j] = cat.getString(j);
                    }
                    all[i] = emojis;
                }

                String[] tabIcons = new String[numCats];
                java.util.Arrays.setAll(tabIcons, i -> all[i].length > 0 ? all[i][0] : "");

                TAB_ICONS = tabIcons;
                ALL = all;
            } catch (Exception e) {
                throw new RuntimeException("Critical: Failed to load emoji_data.json", e);
            }
        }
    }

    static String[] filter(String[] candidates, Paint paint) {
        List<String> out = new ArrayList<>(candidates.length);
        for (String e : candidates) {
            if (paint.hasGlyph(e)) out.add(e);
        }
        return out.toArray(new String[0]);
    }

    private static String readAsset(AssetManager assets) throws Exception {
        try (InputStream is = assets.open(ASSET_PATH)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    private EmojiData() {}
}
