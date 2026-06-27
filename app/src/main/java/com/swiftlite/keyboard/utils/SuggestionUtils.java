package com.swiftlite.keyboard.utils;

import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestionUtils {

    private static final String QWERTY = "qwertyuiop asdfghjkl  zxcvbnm  ";

    public static boolean isAdjacent(char a, char b) {
        int i1 = QWERTY.indexOf(Character.toLowerCase(a));
        int i2 = QWERTY.indexOf(Character.toLowerCase(b));
        if (i1 == -1 || i2 == -1) return false;
        int r1 = i1 / 11, c1 = i1 % 11;
        int r2 = i2 / 11, c2 = i2 % 11;
        return Math.abs(r1 - r2) <= 1 && Math.abs(c1 - c2) <= 1;
    }

    public static int editDistance(String a, String b) {
        int n = a.length(), m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int j = 0; j <= m; j++) prev[j] = j;

        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[m];
    }

    public static String matchCase(String original, String replacement) {
        if (original == null || replacement == null || original.isEmpty()) return replacement;
        if (isAllCaps(original) && original.length() > 1) {
            return replacement.toUpperCase(Locale.getDefault());
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }

    public static boolean isAllCaps(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c) && Character.isLowerCase(c)) return false;
        }
        return true;
    }

    public static boolean isPunctuation(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ':' || c == ';' || c == ')' || c == ']' || c == '}';
    }

    public static String findWordBefore(CharSequence text, int end) {
        if (end <= 0) return null;
        int i = end - 1;
        while (i >= 0 && !Character.isLetter(text.charAt(i))) i--;
        if (i < 0) return null;
        int wordEnd = i + 1;
        while (i > 0 && (Character.isLetter(text.charAt(i - 1)) || text.charAt(i - 1) == '\'')) i--;
        return text.subSequence(i, wordEnd).toString();
    }

    public static String getCurrentWord(CharSequence before) {
        if (before == null || before.length() == 0) return null;
        int end = before.length(), start = end;
        while (start > 0 && (Character.isLetter(before.charAt(start - 1)) || before.charAt(start - 1) == '\'')) start--;
        if (start < end) return before.subSequence(start, end).toString();
        return null;
    }

    public static String getTypedWord(CharSequence before) {
        return getCurrentWord(before);
    }

    public static String[] filterToFit(String[] suggestions, int maxWidth,
                                       Paint regularPaint,
                                       Paint boldPaint,
                                       int chipPadPx, int sepPx) {
        if (suggestions == null || suggestions.length == 0 || maxWidth <= 0) return new String[0];
        List<String> fitting = new ArrayList<>();
        int currentWidth = 0;
        for (int i = 0; i < suggestions.length; i++) {
            Paint p = (i == 0) ? boldPaint : regularPaint;
            int wordWidth = (int) Math.ceil(p.measureText(suggestions[i])) + chipPadPx * 2;
            int sepWidth  = (i == 0) ? 0 : sepPx;
            if (i == 0 || currentWidth + sepWidth + wordWidth <= maxWidth) {
                fitting.add(suggestions[i]);
                currentWidth += sepWidth + wordWidth;
            } else break;
        }
        return fitting.toArray(new String[0]);
    }
}
