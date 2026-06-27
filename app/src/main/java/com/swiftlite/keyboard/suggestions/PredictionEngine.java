package com.swiftlite.keyboard.suggestions;

import com.swiftlite.keyboard.utils.SuggestionUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PredictionEngine {

    public static List<String> predict(String lastWord, int maxSuggestions, 
                                       UsageManager usageManager) {
        if (lastWord == null) return new ArrayList<>();
        String lowerLast = lastWord.toLowerCase(Locale.getDefault());
        List<String> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Map<String, Integer> learned = usageManager.getBigramsFor(lowerLast);
        if (learned != null) {
            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(learned.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (Map.Entry<String, Integer> e : sorted) {
                String word = e.getKey();
                if (seen.add(word)) results.add(word);
                if (results.size() >= 5) break; 
            }
        }

        String[] logical = PredictionData.LOGICAL_PREDICTIONS.get(lowerLast);
        if (logical != null) {
            for (String s : logical) {
                if (seen.add(s)) results.add(s);
                if (results.size() >= maxSuggestions) break;
            }
        }

        if (results.size() < maxSuggestions) {
            if (DictionaryLoader.GRAMMAR_PREPOSITIONS.contains(lowerLast)) {
                for (String s : new String[]{"the", "a", "my", "your", "his", "her"}) {
                    if (seen.add(s)) results.add(s);
                    if (results.size() >= maxSuggestions) break;
                }
            }
        }

        if (results.size() < maxSuggestions) {
            List<Map.Entry<String, Integer>> globalSorted = new ArrayList<>(usageManager.getAllWordUsage().entrySet());
            globalSorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (Map.Entry<String, Integer> e : globalSorted) {
                if (seen.add(e.getKey())) {
                    results.add(e.getKey());
                    if (results.size() >= maxSuggestions) break;
                }
            }
        }

        if (results.isEmpty()) {
            String[] defaults = {"the", "i", "and", "a", "to", "it", "is"};
            for (String d : defaults) {
                if (seen.add(d)) results.add(d);
                if (results.size() >= maxSuggestions) break;
            }
        }

        boolean shouldCap = false;
        if (lastWord.length() > 0) {
            char lastChar = lastWord.charAt(lastWord.length() - 1);
            if (lastChar == '.' || lastChar == '!' || lastChar == '?') shouldCap = true;
        }

        for (int i = 0; i < results.size(); i++) {
            String r = results.get(i);
            if (shouldCap) {
                if (!r.isEmpty()) {
                    results.set(i, Character.toUpperCase(r.charAt(0)) + r.substring(1));
                }
            } else if (lowerLast.equals("i")) {
                results.set(i, r);
            } else {
                results.set(i, SuggestionUtils.matchCase(lastWord, r));
            }
        }

        return results;
    }
}
