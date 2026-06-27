package com.swiftlite.keyboard.theme;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ThemeManager {

    private static final String PREFS      = "keyboard_theme";
    private static final String KEY_THEME  = "theme_id";
    private static final String KEY_ACCENT = "accent_color";
    private static final String KEY_AUTOCORRECT     = "autocorrect";
    private static final String KEY_AUTOCAP         = "autocap";
    private static final String KEY_AUTOSPACE       = "autospace";
    private static final String KEY_AUTOAPOSTROPHE  = "autoapostrophe";
    private static final String KEY_DICTIONARY      = "user_dictionary";
    private static final String KEY_VIBRATE         = "vibrate_on_keypress";
    private static final String KEY_FONT_SIZE       = "font_size_multiplier";

    public static final int THEME_LIGHT  = 0;
    public static final int THEME_DARK   = 1;
    public static final int THEME_AMOLED = 2;
    public static final int THEME_OCEAN  = 3;
    public static final int THEME_FOREST = 4;
    public static final int THEME_DUSK   = 5;

    private String[] mThemeNames;
    private int[]    mAccentColors;
    private String[] mAccentNames;

    private static boolean sLoaded = false;

    public static void init(Context ctx) {
        if (sLoaded) return;
        sLoaded = true;
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

    public ThemeManager(Context ctx) {
        loadAssets(ctx);
        mPrefs   = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        mCurrent = buildTheme(getThemeId(), getAccentColor());
    }

    private void loadAssets(Context ctx) {
        try {
            String json = readAsset(ctx, "themes.json");
            JSONObject obj = new JSONObject(json);

            JSONArray themes = obj.getJSONArray("themes");
            mThemeNames = new String[themes.length()];
            for (int i = 0; i < themes.length(); i++) mThemeNames[i] = themes.getString(i);

            JSONArray accents = obj.getJSONArray("accents");
            mAccentColors = new int[accents.length()];
            mAccentNames  = new String[accents.length()];
            for (int i = 0; i < accents.length(); i++) {
                JSONObject a = accents.getJSONObject(i);
                mAccentNames[i]  = a.getString("name");
                mAccentColors[i] = (int) Long.decode(a.getString("color")).longValue();
            }
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to load themes.json from assets", e);
        }
    }

    private final SharedPreferences mPrefs;
    private KeyboardTheme mCurrent;

    public void reload() {
        mCurrent = buildTheme(getThemeId(), getAccentColor());
    }

    public KeyboardTheme getCurrentTheme() { return mCurrent; }
    public int getThemeId()               { return mPrefs.getInt(KEY_THEME, THEME_LIGHT); }
    public int getAccentColor()           { return mPrefs.getInt(KEY_ACCENT, mAccentColors[0]); }

    public String[] getThemeNames()  { return mThemeNames.clone(); }
    public String[] getAccentNames() { return mAccentNames.clone(); }
    public int[]    getAccentColors() { return mAccentColors.clone(); }

    public void setTheme(int themeId) {
        mPrefs.edit().putInt(KEY_THEME, themeId).apply();
        mCurrent = buildTheme(themeId, getAccentColor());
    }

    public void setAccent(int color) {
        mPrefs.edit().putInt(KEY_ACCENT, color).apply();
        mCurrent = buildTheme(getThemeId(), color);
    }

    public boolean isAutoCorrectEnabled()   { return mPrefs.getBoolean(KEY_AUTOCORRECT,    true); }
    public boolean isAutoCapEnabled()       { return mPrefs.getBoolean(KEY_AUTOCAP,        true); }
    public boolean isAutoSpaceEnabled()     { return mPrefs.getBoolean(KEY_AUTOSPACE,      true); }
    public boolean isAutoApostropheEnabled(){ return mPrefs.getBoolean(KEY_AUTOAPOSTROPHE, true); }
    public boolean isVibrateEnabled()       { return mPrefs.getBoolean(KEY_VIBRATE,        true); }

    public String getUserDictionary()         { return mPrefs.getString(KEY_DICTIONARY, ""); }
    public float  getFontSizeMultiplier()     { return mPrefs.getFloat(KEY_FONT_SIZE, 1.0f); }

    public void setAutoCorrectEnabled(boolean v)    { mPrefs.edit().putBoolean(KEY_AUTOCORRECT,    v).apply(); }
    public void setAutoCapEnabled(boolean v)        { mPrefs.edit().putBoolean(KEY_AUTOCAP,        v).apply(); }
    public void setAutoSpaceEnabled(boolean v)      { mPrefs.edit().putBoolean(KEY_AUTOSPACE,      v).apply(); }
    public void setAutoApostropheEnabled(boolean v) { mPrefs.edit().putBoolean(KEY_AUTOAPOSTROPHE, v).apply(); }
    public void setVibrateEnabled(boolean v)        { mPrefs.edit().putBoolean(KEY_VIBRATE,        v).apply(); }
    public void setUserDictionary(String words)     { mPrefs.edit().putString(KEY_DICTIONARY, words).apply(); }
    public void setFontSizeMultiplier(float v)      { mPrefs.edit().putFloat(KEY_FONT_SIZE,  v).apply(); }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        mPrefs.registerOnSharedPreferenceChangeListener(l);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        mPrefs.unregisterOnSharedPreferenceChangeListener(l);
    }

    private KeyboardTheme buildTheme(int id, int accent) {
        switch (id) {
            case THEME_DARK:   return KeyboardTheme.dark(accent);
            case THEME_AMOLED: return KeyboardTheme.amoled(accent);
            case THEME_OCEAN:  return KeyboardTheme.ocean(accent);
            case THEME_FOREST: return KeyboardTheme.forest(accent);
            case THEME_DUSK:   return KeyboardTheme.dusk(accent);
            default:           return KeyboardTheme.light(accent);
        }
    }
}
