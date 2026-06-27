package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class EmojiHistoryManager {
    public static final String PREF_RECENTS = "emoji_recents";
    public static final String PREF_SKIN    = "emoji_skin";
    public static final String PREF_FILE    = "swiftlite_emoji";
    public static final int    MAX_RECENTS  = 32;

    private final Context mContext;
    private final LinkedList<String> mRecentEmojis = new LinkedList<>();

    public EmojiHistoryManager(Context context) {
        mContext = context;
        loadRecentEmojis();
    }

    public String[] getRecentEmojis() {
        return mRecentEmojis.toArray(new String[0]);
    }

    public void trackRecentEmoji(String emoji) {
        mRecentEmojis.remove(emoji);
        mRecentEmojis.addFirst(emoji);
        if (mRecentEmojis.size() > MAX_RECENTS) mRecentEmojis.removeLast();
        saveRecentEmojis();
    }

    public String getSelectedSkinTone() {
        return mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).getString(PREF_SKIN, null);
    }

    public void saveSelectedSkinTone(String tone) {
        mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit().putString(PREF_SKIN, tone).apply();
    }

    private void loadRecentEmojis() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        String raw = prefs.getString(PREF_RECENTS, "");
        if (!raw.isEmpty()) {
            List<String> list = new ArrayList<>(Arrays.asList(raw.split("\u001F")));
            mRecentEmojis.addAll(list.subList(0, Math.min(list.size(), MAX_RECENTS)));
        }
    }

    private void saveRecentEmojis() {
        StringBuilder sb = new StringBuilder();
        for (String e : mRecentEmojis) {
            if (sb.length() > 0) sb.append('\u001F');
            sb.append(e);
        }
        mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
                .putString(PREF_RECENTS, sb.toString()).apply();
    }
}
