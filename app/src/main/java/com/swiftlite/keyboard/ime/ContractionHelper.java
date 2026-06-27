package com.swiftlite.keyboard.ime;

import android.content.Context;
import com.swiftlite.keyboard.utils.SuggestionUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ContractionHelper {

    private static final Map<String, String> CONTRACTIONS = new HashMap<>();
    private static boolean sLoaded = false;

    public static void init(Context context) {
        if (sLoaded) return;
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("dicts/contractions.txt"), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String lowerNoApos = line.replace("'", "").toLowerCase(Locale.getDefault());
                CONTRACTIONS.put(lowerNoApos, line);
            }
            br.close();
            
            CONTRACTIONS.put("im", "I'm");
            CONTRACTIONS.put("ive", "I've");
            CONTRACTIONS.put("id", "I'd");
            CONTRACTIONS.put("ill", "I'll");
            
            sLoaded = true;
        } catch (Exception ignored) {}
    }

    public static String getContraction(String word) {
        if (word == null || word.isEmpty()) return null;
        String lowerWord = word.toLowerCase(Locale.getDefault());
        String correction = CONTRACTIONS.get(lowerWord);
        
        if (correction != null) {
            return SuggestionUtils.matchCase(word, correction);
        }
        return null;
    }
}
