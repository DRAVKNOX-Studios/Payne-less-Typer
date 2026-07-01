package com.swiftlite.keyboard.suggestions;

import android.content.Context;
import com.swiftlite.keyboard.theme.ThemeManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages text corrections, including grammar fixes and spelling adjustments. it uses a
 * proximity map of the keyboard layout to calculate spatial distances between typed
 * characters and candidate words. It also handles common contractions and contextual
 * corrections for ambiguous words like "were" versus "we're".
 */
public class CorrectionManager {

    private final Map<String, String> mGrammarFixes = new HashMap<>();
    private static final Map<Character, String> PROXIMITY_MAP = new HashMap<>();

    static {
        PROXIMITY_MAP.put('q', "wa");
        PROXIMITY_MAP.put('w', "qeas");
        PROXIMITY_MAP.put('e', "wrsd");
        PROXIMITY_MAP.put('r', "etdf");
        PROXIMITY_MAP.put('t', "ryfg");
        PROXIMITY_MAP.put('y', "tugh");
        PROXIMITY_MAP.put('u', "yijh");
        PROXIMITY_MAP.put('i', "uokj");
        PROXIMITY_MAP.put('o', "iplk");
        PROXIMITY_MAP.put('p', "ol");
        PROXIMITY_MAP.put('a', "qwsz");
        PROXIMITY_MAP.put('s', "wedazx");
        PROXIMITY_MAP.put('d', "erfsxc");
        PROXIMITY_MAP.put('f', "rtgvcd");
        PROXIMITY_MAP.put('g', "tyhbvf");
        PROXIMITY_MAP.put('h', "yujnbg");
        PROXIMITY_MAP.put('j', "uikmnh");
        PROXIMITY_MAP.put('k', "iolmj");
        PROXIMITY_MAP.put('l', "opk");
        PROXIMITY_MAP.put('z', "asx");
        PROXIMITY_MAP.put('x', "sdcz");
        PROXIMITY_MAP.put('c', "dfvx");
        PROXIMITY_MAP.put('v', "fgbc");
        PROXIMITY_MAP.put('b', "ghnv");
        PROXIMITY_MAP.put('n', "hjmb");
        PROXIMITY_MAP.put('m', "jkn");
    }

    private final ThemeManager mThemeManager;

    public CorrectionManager(Context context, ThemeManager themeManager) {
        mThemeManager = themeManager;
        loadContractions(context);
    }

    private void loadContractions(Context context) {
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("dicts/contractions.txt"), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String lowerNoApos = line.replace("'", "").toLowerCase(Locale.getDefault());
                mGrammarFixes.put(lowerNoApos, line);
            }
            br.close();
        } catch (Exception ignored) {}
        
        mGrammarFixes.put("im", "I'm");
        mGrammarFixes.put("ive", "I've");
        mGrammarFixes.put("id", "I'd");
        mGrammarFixes.put("ill", "I'll");
    }

    public String getTopCorrection(String lastWord, String word, UsageManager usageManager, SuggestionEngine engine) {
        if (word == null || word.isEmpty()) return null;
        String lower = word.toLowerCase(Locale.getDefault());

        if (usageManager.getRejectionCount(lower) >= 3) return word;

        if (lower.equals("i")) return "I";

        if (mThemeManager.isAutoApostropheEnabled() && mGrammarFixes.containsKey(lower)) {
            boolean isAmbiguous = lower.equals("were") || lower.equals("well")
                               || lower.equals("its")  || lower.equals("hell");
            if (!isAmbiguous) return matchCase(word, mGrammarFixes.get(lower));
        }

        String contextual = getContextualCorrection(lastWord, word, usageManager);
        if (!contextual.equalsIgnoreCase(word)) return matchCase(word, contextual);

        if (engine.isValidWord(lower)) {
             if (mThemeManager.isProfanityFilterEnabled() && engine.isProfane(lower)) {
             } else {
                 return word;
             }
        }

        if (!mThemeManager.isAutoCorrectEnabled()) return word;

        if (word.length() <= 1) return word;

        String[] suggestions = engine.buildResults(lastWord, word, null, false);
        if (suggestions.length > 0) {
            boolean wordProfane = mThemeManager.isProfanityFilterEnabled() && engine.isProfane(lower);
            
            if (wordProfane && !suggestions[0].equalsIgnoreCase(word)) {
                return matchCase(word, suggestions[0]);
            }

            if (suggestions.length > 1) {
                String best = null;
                double bestScore = Double.MAX_VALUE;

                for (int i = (wordProfane ? 0 : 1); i < Math.min(suggestions.length, 4); i++) {
                    String candidate = suggestions[i];
                    if (candidate == null || (candidate.length() <= 1 && !candidate.equals("i") && !candidate.equals("I"))) continue;
                    double dist = spatialDistance(lower, candidate.toLowerCase(Locale.getDefault()));
                    dist += Math.abs(word.length() - candidate.length()) * 0.5;

                    if (dist < bestScore) {
                        bestScore = dist;
                        best = candidate;
                    }
                }

                if (best != null && (wordProfane || bestScore <= 1.2)) {
                    return matchCase(word, best);
                }
            }
        }

        return word;
    }

    private String matchCase(String original, String replacement) {
        if (original == null || replacement == null || original.isEmpty()) return replacement;
        
        if (!mThemeManager.isAutoCapEnabled()) {
            return replacement;
        }

        if (isAllCaps(original) && original.length() > 1) {
            return replacement.toUpperCase(Locale.getDefault());
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }

    private boolean isAllCaps(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c) && Character.isLowerCase(c)) return false;
        }
        return true;
    }

    public String getContextualCorrection(String lastWord, String word, UsageManager usageManager) {
        if (word == null) return null;
        String lower     = word.toLowerCase(Locale.getDefault());
        if (usageManager.getRejectionCount(lower) >= 3) return word;

        String lastLower = (lastWord != null) ? lastWord.toLowerCase(Locale.getDefault()) : "";

        if (lower.equals("were")) {
            if (DictionaryLoader.GRAMMAR_PRONOUNS.contains(lastLower) || 
                DictionaryLoader.GRAMMAR_CONJUNCTIONS.contains(lastLower)) {
                if (lastLower.equals("we") || lastLower.equals("they") || lastLower.equals("you"))
                    return "were";
                if (lastLower.equals("if") || lastLower.equals("who") || lastLower.equals("there"))
                    return "were";
            }
            if (mThemeManager.isAutoApostropheEnabled() && (lastLower.isEmpty() || lastLower.equals("think") || lastLower.equals("hope")))
                return "we're";
        }
        if (lower.equals("well")) {
            if (mThemeManager.isAutoApostropheEnabled() && (lastLower.equals("we") || lastLower.equals("i") || lastLower.equals("they")
             || lastLower.equals("hope") || lastLower.equals("think"))) return "we'll";
            return "well";
        }
        if (lower.equals("its")) {
            if (mThemeManager.isAutoApostropheEnabled() && (lastLower.isEmpty() || DictionaryLoader.GRAMMAR_AUX_VERBS.contains(lastLower) || 
                lastLower.equals("think") || lastLower.equals("hope") || lastLower.equals("said"))) 
                return "it's";
            return "its";
        }
        if (lower.equals("hell")) {
            if (mThemeManager.isAutoApostropheEnabled() && (lastLower.equals("he") || lastLower.equals("she") || lastLower.equals("they") ||
                lastLower.equals("think") || lastLower.equals("hope")))
                return "he'll";
            return "hell";
        }
        
        if (lower.equals("i")) return "I";

        return word;
    }

    private double spatialDistance(String typed, String candidate) {
        int n = typed.length();
        int m = candidate.length();
        if (n == 0) return m;
        if (m == 0) return n;

        double[] pprev = new double[m + 1];
        double[] prev = new double[m + 1];
        double[] curr = new double[m + 1];

        for (int j = 0; j <= m; j++) prev[j] = j;

        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            for (int j = 1; j <= m; j++) {
                char t = typed.charAt(i - 1);
                char c = candidate.charAt(j - 1);
                double cost = (t == c) ? 0 : (isProximate(t, c) ? 0.5 : 1.0);

                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);

                if (i > 1 && j > 1 && typed.charAt(i - 1) == candidate.charAt(j - 2)
                        && typed.charAt(i - 2) == candidate.charAt(j - 1)) {
                    curr[j] = Math.min(curr[j], pprev[j - 2] + 0.7);
                }
            }
            System.arraycopy(prev, 0, pprev, 0, m + 1);
            System.arraycopy(curr, 0, prev, 0, m + 1);
        }
        return prev[m];
    }

    private boolean isProximate(char a, char b) {
        String prox = PROXIMITY_MAP.get(Character.toLowerCase(a));
        return prox != null && prox.indexOf(Character.toLowerCase(b)) != -1;
    }
}
