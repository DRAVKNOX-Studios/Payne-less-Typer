package com.swiftlite.keyboard.suggestions;

import com.swiftlite.keyboard.utils.SuggestionUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SuggestionSearcher {

    public static List<String> getFuzzyMatches(String lastWord, String word, int max, List<String> exclude,
                                               MmapDictionary words, SuggestionEngine engine) {
        List<String> out = new ArrayList<>();
        if (words == null || words.size() == 0 || word.length() < 2) return out;
        String lower = word.toLowerCase(Locale.ROOT);
        char first = lower.charAt(0);

        List<Integer> candidates = new ArrayList<>();
        int low = 0, high = words.size() - 1, startIdx = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String midWord = words.getLowerWord(mid);
            if (midWord == null) {
                low = mid + 1;
                continue;
            }
            char c = midWord.charAt(0);
            if (c < first) { low  = mid + 1; }
            else           { startIdx = mid; high = mid - 1; }
        }

        for (int i = startIdx; i < words.size(); i++) {
            char c = words.getWordFirstCharLower(i);
            if (c > first && !SuggestionUtils.isAdjacent(c, first)) break;
            if ((c == first || SuggestionUtils.isAdjacent(c, first))
                    && Math.abs(words.getWordLength(i) - lower.length()) <= 1) {
                candidates.add(i);
                if (candidates.size() > 3000) break;
            }
        }

        if (candidates.isEmpty()) return out;

        final Map<Integer, String> lowerCache = new HashMap<>();
        candidates.sort((a, b) -> {
            String wa = lowerCache.get(a);
            if (wa == null) {
                wa = words.getLowerWord(a);
                if (wa == null) wa = "";
                lowerCache.put(a, wa);
            }
            String wb = lowerCache.get(b);
            if (wb == null) {
                wb = words.getLowerWord(b);
                if (wb == null) wb = "";
                lowerCache.put(b, wb);
            }
            int d1 = SuggestionUtils.editDistance(lower, wa);
            int d2 = SuggestionUtils.editDistance(lower, wb);
            if (d1 != d2) return d1 - d2;
            return Float.compare(engine.calculateScore(lastWord, b), engine.calculateScore(lastWord, a));
        });

        for (int idx : candidates) {
            String dw   = lowerCache.get(idx);
            if (dw == null || dw.isEmpty()) continue;
            int    dist = SuggestionUtils.editDistance(lower, dw);
            if (dist > 1 && lower.length() < 4) continue;
            if (dist > 2) continue;
            String original = words.getWord(idx);
            if (!isExcluded(original, exclude)) {
                out.add(original);
                if (out.size() >= max) break;
            }
        }

        List<String> userMatches = getUserFuzzy(words, lower, max - out.size(), exclude);
        out.addAll(userMatches);
        return out;
    }

    public static List<String> getPrefixSuggestions(String lastWord, String prefix, int max, List<String> exclude,
                                                    MmapDictionary words, SuggestionEngine engine) {
        List<String> out = new ArrayList<>();
        if (words == null || words.size() == 0 || max <= 0 || prefix == null || prefix.isEmpty()) return out;
        String lp = prefix.toLowerCase(Locale.ROOT);
        byte[] lpBytes = lp.getBytes(StandardCharsets.UTF_8);

        int startIdx = words.prefixSearchStart(lpBytes);
        if (startIdx == -1) {
            out.addAll(getUserPrefix(words, lp, max, exclude));
            return out;
        }

        List<Integer> candidates = new ArrayList<>();
        for (int i = startIdx; i < words.size(); i++) {
            String w = words.getLowerWord(i);
            if (w == null) continue;
            if (!w.startsWith(lp)) break;
            if (w.equals(lp)) continue;
            if (!isExcluded(words.getWord(i), exclude)) {
                candidates.add(i);
                if (candidates.size() > 500) break;
            }
        }

        candidates.sort((a, b) ->
                Float.compare(engine.calculateScore(lastWord, b), engine.calculateScore(lastWord, a)));

        for (int i = 0; i < Math.min(candidates.size(), max); i++) {
            out.add(words.getWord(candidates.get(i)));
        }

        if (out.size() < max) out.addAll(getUserPrefix(words, lp, max - out.size(), exclude));
        return out;
    }

    private static List<String> getUserPrefix(MmapDictionary dict, String lp, int max, List<String> exclude) {
        List<String> out = new ArrayList<>();
        String[] user = dict.getUserWords();
        if (user == null) return out;
        for (String w : user) {
            if (w == null) continue;
            if (w.toLowerCase(Locale.ROOT).startsWith(lp) && !isExcluded(w, exclude)) {
                out.add(w);
                if (out.size() >= max) break;
            }
        }
        return out;
    }

    private static List<String> getUserFuzzy(MmapDictionary dict, String lower, int max, List<String> exclude) {
        List<String> out = new ArrayList<>();
        String[] user = dict.getUserWords();
        if (user == null || max <= 0) return out;
        for (String w : user) {
            if (w == null) continue;
            String wl = w.toLowerCase(Locale.ROOT);
            int dist = SuggestionUtils.editDistance(lower, wl);
            if (dist <= 1 && !isExcluded(w, exclude)) {
                out.add(w);
                if (out.size() >= max) break;
            }
        }
        return out;
    }

    private static boolean isExcluded(String word, List<String> exclude) {
        for (String e : exclude) if (word.equalsIgnoreCase(e)) return true;
        return false;
    }
}
