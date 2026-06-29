package com.swiftlite.keyboard.suggestions;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides emoji suggestions based on typed shortcodes or names.
 * Kept under 300 lines as per requirement.
 */
public class EmojiSuggestionProvider {
    private static final String ASSET_NAME = "emoji_shortcodes.json";
    private JSONObject mMapping;

    public EmojiSuggestionProvider(Context context) {
        try (InputStream is = context.getAssets().open(ASSET_NAME)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int read = is.read(buffer);
            if (read > 0) {
                mMapping = new JSONObject(new String(buffer, 0, read, StandardCharsets.UTF_8));
            } else {
                mMapping = new JSONObject();
            }
        } catch (Exception e) {
            mMapping = new JSONObject();
        }
    }

    public List<String> getEmojiSuggestions(String word) {
        List<String> results = new ArrayList<>();
        if (word == null || word.length() < 2) return results;

        String q = word.toLowerCase();
        if (q.startsWith(":")) q = q.substring(1);
        if (q.endsWith(":")) q = q.substring(0, q.length() - 1);
        if (q.isEmpty()) return results;

        Iterator<String> keys = mMapping.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith(q)) {
                String emoji = mMapping.optString(key);
                if (!results.contains(emoji)) {
                    results.add(emoji);
                }
            }
            if (results.size() >= 3) break;
        }
        return results;
    }
}
