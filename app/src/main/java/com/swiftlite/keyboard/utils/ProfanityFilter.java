package com.swiftlite.keyboard.utils;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ProfanityFilter {
    private final Set<String> words = new HashSet<>();

    public ProfanityFilter(Context context) {
        try {
            InputStream is = context.getAssets().open("profanity.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject json = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                JSONArray arr = json.getJSONArray(keys.next());
                for (int i = 0; i < arr.length(); i++) {
                    words.add(arr.getString(i).toLowerCase().trim());
                }
            }
        } catch (Exception ignored) {}
    }

    public boolean isProfane(String word) {
        if (word == null) return false;
        String lower = word.toLowerCase().trim();
        if (words.contains(lower)) return true;
        
        if (lower.length() > 4) {
            for (String bad : words) {
                if (bad.length() >= 4 && lower.equals(bad + "s")) return true;
                if (bad.length() >= 4 && lower.equals(bad + "ing")) return true;
                if (bad.length() >= 4 && lower.equals(bad + "ed")) return true;
                if (bad.length() >= 4 && lower.equals(bad + "er")) return true;
            }
        }
        return false;
    }
}
