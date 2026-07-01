package com.swiftlite.keyboard.ime;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class KeyboardLayout {
    public Object[][] row1, row2, row3, row4;
    public Object[][] numRow, numRowShifted;
    public final Map<Character, String[]> longPress = new HashMap<>();
    public final Map<Character, String[]> subLongPress = new HashMap<>();
    public final List<String> emailDomains = new ArrayList<>();
    public final List<String> searchDomains = new ArrayList<>();

    private static KeyboardLayout sInstance;

    public static KeyboardLayout getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new KeyboardLayout();
            sInstance.load(ctx);
        }
        return sInstance;
    }

    private void load(Context ctx) {
        try {
            String json = readAsset(ctx, "keyboard_layout.json");
            JSONObject obj = new JSONObject(json);
            JSONObject keys = obj.getJSONObject("keys");
            row1 = parseRow(keys.getJSONArray("row1"));
            row2 = parseRow(keys.getJSONArray("row2"));
            row3 = parseRow(keys.getJSONArray("row3"));
            row4 = parseRow(keys.getJSONArray("row4"));

            String jsonNum = readAsset(ctx, "number_row.json");
            JSONObject objNum = new JSONObject(jsonNum);
            numRow = parseRow(objNum.getJSONArray("normal"));
            numRowShifted = parseRow(objNum.getJSONArray("shifted"));

            JSONArray domains = obj.getJSONArray("email_domains");
            for (int i = 0; i < domains.length(); i++) emailDomains.add(domains.getString(i));

            JSONArray sDomains = obj.getJSONArray("search_domains");
            for (int i = 0; i < sDomains.length(); i++) searchDomains.add(sDomains.getString(i));

            JSONObject lp = obj.getJSONObject("long_press");
            Iterator<String> lpIt = lp.keys();
            while (lpIt.hasNext()) {
                String k = lpIt.next();
                longPress.put(k.charAt(0), toStringArray(lp.getJSONArray(k)));
            }
            JSONObject slp = obj.getJSONObject("sub_long_press");
            Iterator<String> slpIt = slp.keys();
            while (slpIt.hasNext()) {
                String k = slpIt.next();
                subLongPress.put(k.charAt(0), toStringArray(slp.getJSONArray(k)));
            }
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to load keyboard layout", e);
        }
    }

    private Object[][] parseRow(JSONArray arr) throws Exception {
        Object[][] row = new Object[arr.length()][];
        for (int i = 0; i < arr.length(); i++) {
            JSONArray key = arr.getJSONArray(i);
            row[i] = new Object[key.length()];
            for (int j = 0; j < key.length(); j++) row[i][j] = key.get(j);
        }
        return row;
    }

    private String[] toStringArray(JSONArray arr) throws Exception {
        String[] res = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) res[i] = arr.getString(i);
        return res;
    }

    private String readAsset(Context ctx, String path) throws Exception {
        try (InputStream is = ctx.getAssets().open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }
}
