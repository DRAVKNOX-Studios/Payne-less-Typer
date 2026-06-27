package com.swiftlite.keyboard.suggestions;

import java.util.Locale;

public class DictWord {
    public final String word;
    private final String lower;
    public final byte category;

    public DictWord(String word, int category) {
        this.word = word;
        this.category = (byte) category;
        String l = word.toLowerCase(java.util.Locale.getDefault());
        this.lower = l.equals(word) ? null : l;
    }

    public String getLower() {
        return lower == null ? word : lower;
    }
}
