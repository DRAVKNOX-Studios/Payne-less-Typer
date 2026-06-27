package com.swiftlite.keyboard.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DictionaryLoader {

    public static final int CAT_CORE      = 0;
    public static final int CAT_GRAMMAR   = 1;
    public static final int CAT_COMMON    = 2;
    public static final int CAT_UNCOMMON  = 3;
    public static final int CAT_RARE      = 4;
    public static final int CAT_EXTRA     = 5;
    public static final int CAT_SLANG     = 6;
    public static final int CAT_BRAINROT  = 7;
    public static final int CAT_DEV       = 8;
    public static final int CAT_PROPER    = 9;
    public static final int CAT_NAMED     = 10;

    public static final Set<String> GRAMMAR_ARTICLES      = new HashSet<>();
    public static final Set<String> GRAMMAR_AUX_VERBS     = new HashSet<>();
    public static final Set<String> GRAMMAR_CONJUNCTIONS  = new HashSet<>();
    public static final Set<String> GRAMMAR_PREPOSITIONS  = new HashSet<>();
    public static final Set<String> GRAMMAR_PRONOUNS      = new HashSet<>();

    private static final String DICT_ASSET   = "dicts/en.dict";
    private static final String DICT_FILE    = "en.dict";
    private static final String TAG          = "SwiftLite";

    public static MmapDictionary load(Context ctx) {
        populateGrammarSets(ctx);
        File dictFile = new File(ctx.getFilesDir(), DICT_FILE);
        if (!dictFile.exists() || isAssetNewer(ctx, dictFile)) {
            extractAsset(ctx, dictFile);
        }
        try {
            MmapDictionary dict = new MmapDictionary(dictFile);
            appendUserWords(ctx, dict);
            return dict;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dictionary", e);
        }
    }

    private static boolean isAssetNewer(Context ctx, File existing) {
        try {
            long assetMtime = ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).lastUpdateTime;
            return assetMtime > existing.lastModified();
        } catch (Exception e) {
            return false;
        }
    }

    private static void extractAsset(Context ctx, File dest) {
        try (InputStream in  = ctx.getAssets().open(DICT_ASSET);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            dest.setLastModified(System.currentTimeMillis());
            Log.i(TAG, "Dict extracted to " + dest.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract dictionary asset", e);
        }
    }

    private static void appendUserWords(Context ctx, MmapDictionary dict) {
        SharedPreferences prefs = ctx.getSharedPreferences("keyboard_theme", Context.MODE_PRIVATE);
        String raw = prefs.getString("user_dictionary", "");
        if (raw == null || raw.isEmpty()) return;
        String[] lines = raw.split("\n");
        String[] userWords = Arrays.stream(lines)
                .map(String::trim)
                .filter(w -> !w.isEmpty())
                .toArray(String[]::new);
        dict.setUserWords(userWords);
    }

    private static void populateGrammarSets(Context ctx) {
        GRAMMAR_ARTICLES.clear();
        GRAMMAR_AUX_VERBS.clear();
        GRAMMAR_CONJUNCTIONS.clear();
        GRAMMAR_PREPOSITIONS.clear();
        GRAMMAR_PRONOUNS.clear();
        readIntoSet(ctx, "dicts/articles.txt",        GRAMMAR_ARTICLES);
        readIntoSet(ctx, "dicts/auxiliary_verbs.txt", GRAMMAR_AUX_VERBS);
        readIntoSet(ctx, "dicts/conjunctions.txt",    GRAMMAR_CONJUNCTIONS);
        readIntoSet(ctx, "dicts/prepositions.txt",    GRAMMAR_PREPOSITIONS);
        readIntoSet(ctx, "dicts/pronouns.txt",        GRAMMAR_PRONOUNS);
    }

    private static void readIntoSet(Context ctx, String asset, Set<String> set) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(ctx.getAssets().open(asset), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim().toLowerCase(Locale.ROOT);
                if (!line.isEmpty() && !line.startsWith("#")) set.add(line);
            }
        } catch (IOException ignored) {}
    }
}
