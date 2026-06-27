package com.swiftlite.keyboard.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UsageManager {
    private static final String PREF_NAME     = "suggestion_learning";
    private static final String KEY_CAT_USAGE = "cat_usage_";

    private final Context mCtx;
    private final SharedPreferences mUsagePrefs;
    private final SharedPreferences mWordPrefs;
    private final SharedPreferences mBigramPrefs;
    private final SharedPreferences mRejectionPrefs;

    private final long[] mCategoryUsage = new long[11];
    private final Map<String, Integer> mWordUsage    = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> mBigramUsage = new ConcurrentHashMap<>();
    private final Map<String, Integer> mRejections   = new ConcurrentHashMap<>();

    public UsageManager(Context ctx) {
        mCtx         = ctx;
        mUsagePrefs  = ctx.getSharedPreferences(PREF_NAME,              Context.MODE_PRIVATE);
        mWordPrefs   = ctx.getSharedPreferences(PREF_NAME + "_words",   Context.MODE_PRIVATE);
        mBigramPrefs = ctx.getSharedPreferences(PREF_NAME + "_bigrams", Context.MODE_PRIVATE);
        mRejectionPrefs = ctx.getSharedPreferences(PREF_NAME + "_rejections", Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        synchronized (mCategoryUsage) {
            for (int i = 0; i < mCategoryUsage.length; i++)
                mCategoryUsage[i] = mUsagePrefs.getLong(KEY_CAT_USAGE + i, 0);
        }
        for (Map.Entry<String, ?> e : mWordPrefs.getAll().entrySet())
            if (e.getValue() instanceof Integer) mWordUsage.put(e.getKey(), (Integer) e.getValue());
        loadBigrams();
        for (Map.Entry<String, ?> e : mRejectionPrefs.getAll().entrySet())
            if (e.getValue() instanceof Integer) mRejections.put(e.getKey(), (Integer) e.getValue());
    }

    private void loadBigrams() {
        for (Map.Entry<String, ?> e : mBigramPrefs.getAll().entrySet()) {
            String encoded = (String) e.getValue();
            Map<String, Integer> seconds = new HashMap<>();
            for (String pair : encoded.split(",")) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    try { seconds.put(parts[0], Integer.parseInt(parts[1])); }
                    catch (NumberFormatException ignored) {}
                }
            }
            mBigramUsage.put(e.getKey(), seconds);
        }
    }

    public long getCategoryUsage(int category) {
        synchronized (mCategoryUsage) {
            return category < mCategoryUsage.length ? mCategoryUsage[category] : 0;
        }
    }

    public int getWordUsage(String lowerWord)               { return mWordUsage.getOrDefault(lowerWord, 0); }
    public Map<String, Integer> getBigramsFor(String lower) { return mBigramUsage.get(lower); }
    public Map<String, Integer> getAllWordUsage()            { return mWordUsage; }
    public int getRejectionCount(String lowerWord)          { return mRejections.getOrDefault(lowerWord, 0); }

    public void learnWordUse(String word, MmapDictionary dictionary) {
        if (TextUtils.isEmpty(word) || dictionary == null) return;
        String lower = word.toLowerCase(Locale.ROOT);
        synchronized (mCategoryUsage) {
            int idx = dictionary.binarySearch(lower);
            if (idx >= 0) {
                int cat = dictionary.getCategory(idx) & 0xFF;
                if (cat < mCategoryUsage.length) {
                    mCategoryUsage[cat]++;
                    mUsagePrefs.edit().putLong(KEY_CAT_USAGE + cat, mCategoryUsage[cat]).apply();
                }
            }
        }
        int count = mWordUsage.getOrDefault(lower, 0) + 1;
        mWordUsage.put(lower, count);
        mWordPrefs.edit().putInt(lower, count).apply();
    }

    public void learnBigram(String first, String second) {
        if (TextUtils.isEmpty(first) || TextUtils.isEmpty(second)) return;
        String f = first.toLowerCase(Locale.ROOT);
        String s = second.toLowerCase(Locale.ROOT);
        Map<String, Integer> seconds = mBigramUsage.computeIfAbsent(f, k -> new HashMap<>());
        int count = seconds.getOrDefault(s, 0) + 1;
        seconds.put(s, count);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : seconds.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        mBigramPrefs.edit().putString(f, sb.toString()).apply();
    }

    public void learnRejection(String word, SuggestionEngine engine) {
        if (TextUtils.isEmpty(word)) return;
        String lower = word.toLowerCase(Locale.ROOT);
        int count = mRejections.getOrDefault(lower, 0) + 1;
        mRejections.put(lower, count);
        mRejectionPrefs.edit().putInt(lower, count).apply();
        if (count >= 3) {
            SharedPreferences prefs = mCtx.getSharedPreferences("keyboard_theme", Context.MODE_PRIVATE);
            String userDict = prefs.getString("user_dictionary", "");
            boolean found = false;
            if (!userDict.isEmpty()) {
                for (String w : userDict.split("\n")) if (w.equalsIgnoreCase(word)) { found = true; break; }
            }
            if (!found) {
                prefs.edit().putString("user_dictionary",
                        userDict.isEmpty() ? word : userDict + "\n" + word).apply();
                engine.reloadUserDictionary();
            }
        }
    }
}
