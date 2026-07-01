package com.swiftlite.keyboard.suggestions;

import com.swiftlite.keyboard.utils.SuggestionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SuggestionResultBuilder {
    private static final int MAX_SUGGESTIONS = 8;

    public static float[] loadConfig(android.content.Context ctx) {
        try (InputStream is = ctx.getAssets().open("engine_config.json")) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            JSONObject obj = new JSONObject(new String(buffer, StandardCharsets.UTF_8));
            JSONArray arr = obj.getJSONArray("cat_default_boost");
            float[] boosts = new float[arr.length()];
            for (int i = 0; i < arr.length(); i++) boosts[i] = (float) arr.getDouble(i);
            return boosts;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] build(SuggestionEngine engine, String lastWord, String word, String[] spellSuggestions, boolean includeCorrection, 
                                 CorrectionManager correction, com.swiftlite.keyboard.theme.ThemeManager theme, EmojiSuggestionProvider emoji) {
        List<String> results = new ArrayList<>();
        String corr = includeCorrection ? correction.getTopCorrection(lastWord, word, engine.getUsageManager(), engine) : null;
        boolean filter = theme.isProfanityFilterEnabled();
        if (corr != null && filter && engine.isProfane(corr)) corr = null;

        boolean hasCorrection = corr != null && !corr.equalsIgnoreCase(word);
        if (hasCorrection) {
            results.add(corr);
            if (!(filter && engine.isProfane(word))) results.add(word);
        } else if (!(filter && engine.isProfane(word))) {
            results.add(word);
        }

        List<String> emojis = emoji.getEmojiSuggestions(word);
        for (String e : emojis) if (!results.contains(e)) results.add(e);

        if (spellSuggestions != null) {
            for (String s : spellSuggestions) {
                if (s == null) continue;
                if (filter && engine.isProfane(s)) continue;
                if (!s.equalsIgnoreCase(word) && (corr == null || !s.equalsIgnoreCase(corr))) {
                    String matched = SuggestionUtils.matchCase(word, s);
                    if (!results.contains(matched)) {
                        results.add(matched);
                        if (results.size() >= MAX_SUGGESTIONS) break;
                    }
                }
            }
        }

        if (results.size() < MAX_SUGGESTIONS) {
            List<String> prefixSugs = SuggestionSearcher.getPrefixSuggestions(lastWord, word, MAX_SUGGESTIONS + 2, results, engine.getDictionary(), engine);
            for (String s : prefixSugs) {
                if (s == null) continue;
                if (filter && engine.isProfane(s)) continue;
                String matched = SuggestionUtils.matchCase(word, s);
                if (!results.contains(matched)) {
                    results.add(matched);
                    if (results.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }

        if (results.size() < 3) {
            List<String> fuzzy = SuggestionSearcher.getFuzzyMatches(lastWord, word, 5, results, engine.getDictionary(), engine);
            for (String s : fuzzy) {
                if (s == null) continue;
                if (filter && engine.isProfane(s)) continue;
                String matched = SuggestionUtils.matchCase(word, s);
                if (!results.contains(matched)) {
                    results.add(matched);
                    if (results.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }

        if (results.isEmpty() && !(filter && engine.isProfane(word))) results.add(word);
        return results.toArray(new String[0]);
    }

    public static float calculateScore(SuggestionEngine engine, String lastWord, int wordIndex, float[] boosts, UsageManager usage) {
        MmapDictionary dict = engine.getDictionary();
        byte category = dict.getCategory(wordIndex);
        float base = (category < boosts.length) ? boosts[category] : 0f;
        String word = dict.getWord(wordIndex);
        if (word == null) return -1000f;
        String lowerWord = word.toLowerCase(Locale.ROOT);
        float usageBoost = usage.getWordUsage(lowerWord) * 20000f;
        float bigramBoost = 0f;
        if (lastWord != null) {
            Map<String, Integer> bigrams = usage.getBigramsFor(lastWord.toLowerCase(Locale.ROOT));
            if (bigrams != null && bigrams.containsKey(lowerWord)) bigramBoost = bigrams.get(lowerWord) * 100000f;
        }
        return base + usageBoost + bigramBoost + usage.getCategoryUsage(category) * 20f - word.length() * 10f;
    }
}
