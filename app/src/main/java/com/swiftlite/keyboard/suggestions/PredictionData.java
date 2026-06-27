package com.swiftlite.keyboard.suggestions;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PredictionData {
    public static Map<String, String[]> LOGICAL_PREDICTIONS = new HashMap<>();

    private static boolean sLoaded = false;

    public static void init(Context ctx) {
        if (sLoaded) return;
        try {
            String json = readAsset(ctx, "predictions.json");
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray arr = obj.getJSONArray(key);
                String[] vals = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) vals[i] = arr.getString(i);
                LOGICAL_PREDICTIONS.put(key, vals);
            }
            sLoaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to load predictions.json", e);
        }
    }

    private static String readAsset(Context ctx, String path) throws Exception {
        try (InputStream is = ctx.getAssets().open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

}
