package com.swiftlite.keyboard.suggestions;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import com.swiftlite.keyboard.utils.SuggestionUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestionEngine implements SpellCheckerSession.SpellCheckerSessionListener {

    public interface SuggestionCallback {
        void onSuggestions(String[] suggestions);
    }

    private static final int MAX_SUGGESTIONS = 8;
    private static float[] CAT_DEFAULT_BOOST;

    private void loadConfig(Context ctx) {
        try {
            String json = readAsset(ctx, "engine_config.json");
            JSONObject obj = new JSONObject(json);
            JSONArray arr = obj.getJSONArray("cat_default_boost");
            CAT_DEFAULT_BOOST = new float[arr.length()];
            for (int i = 0; i < arr.length(); i++) CAT_DEFAULT_BOOST[i] = (float) arr.getDouble(i);
        } catch (Exception e) {
            throw new RuntimeException("Critical: Failed to load engine_config.json", e);
        }
    }

    private String readAsset(Context ctx, String path) throws Exception {
        try (InputStream is = ctx.getAssets().open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        }
    }

    private final Context mCtx;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private volatile SpellCheckerSession mSpellSession;
    private final AtomicBoolean mReady = new AtomicBoolean(false);

    private volatile MmapDictionary mWords;
    private final UsageManager mUsageManager;
    private final CorrectionManager mCorrectionManager;

    private volatile SuggestionCallback mPendingCallback;
    private volatile Handler            mPendingHandler;
    private volatile String             mPendingWord;
    private volatile long               mPendingSeq;
    private volatile int                mPendingWidth;
    private volatile android.graphics.Paint mPendingRegularPaint, mPendingBoldPaint;
    private volatile int                mPendingChipPad;
    private final Object                mPendingLock = new Object();

    public SuggestionEngine(Context ctx, com.swiftlite.keyboard.theme.ThemeManager themeManager) {
        mCtx = ctx.getApplicationContext();
        mUsageManager = new UsageManager(mCtx);
        mCorrectionManager = new CorrectionManager(mCtx, themeManager);
    }

    public void reloadUserDictionary() {
        new Thread(() -> mWords = DictionaryLoader.load(mCtx)).start();
    }

    public void ensureLoaded() {
        if (mReady.get()) return;
        new Thread(this::initAsync).start();
    }

    public void initAsync() {
        if (mReady.getAndSet(true)) return;
        loadConfig(mCtx);
        PredictionData.init(mCtx);
        mWords = DictionaryLoader.load(mCtx);
        try {
            TextServicesManager tsm = (TextServicesManager)
                    mCtx.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (tsm != null)
                mSpellSession = tsm.newSpellCheckerSession(null, Locale.getDefault(), this, true);
        } catch (Exception ignored) {}
    }

    public void destroy() {
        mReady.set(false);
        mExecutor.shutdownNow();
        if (mSpellSession != null) { mSpellSession.close(); mSpellSession = null; }
        if (mWords != null) { try { mWords.close(); } catch (Exception ignored) {} mWords = null; }
    }

    public void getSuggestions(String lastWord, String word, int availableWidth,
                               android.graphics.Paint regularPaint,
                               android.graphics.Paint boldPaint,
                               int chipPadPx,
                               Handler handler, SuggestionCallback callback) {
        if (TextUtils.isEmpty(word) || handler == null || callback == null) {
            if (callback != null && handler != null)
                handler.post(() -> callback.onSuggestions(new String[0]));
            return;
        }
        final long seq;
        synchronized (mPendingLock) {
            seq = ++mPendingSeq; mPendingWord = word; mPendingHandler = handler; mPendingCallback = callback;
            mPendingWidth = availableWidth; mPendingRegularPaint = regularPaint; mPendingBoldPaint = boldPaint; mPendingChipPad = chipPadPx;
        }
        mExecutor.execute(() -> {
            String[] prefixResults = buildResults(lastWord, word, null);
            deliverIfCurrent(seq, SuggestionUtils.filterToFit(prefixResults, availableWidth, regularPaint, boldPaint, chipPadPx, 1));
        });
        SpellCheckerSession session = mSpellSession;
        if (session != null && mReady.get()) {
            try { session.getSuggestions(new TextInfo(word), MAX_SUGGESTIONS - 1); }
            catch (Exception ignored) {}
        }
    }

    public void getPredictions(String lastWord, int availableWidth,
                               android.graphics.Paint regularPaint,
                               android.graphics.Paint boldPaint,
                               int chipPadPx,
                               Handler handler, SuggestionCallback callback) {
        if (TextUtils.isEmpty(lastWord) || handler == null || callback == null) {
            if (callback != null && handler != null) handler.post(() -> callback.onSuggestions(new String[0]));
            return;
        }
        final long seq;
        synchronized (mPendingLock) {
            seq = ++mPendingSeq; mPendingWord = null; mPendingHandler = handler; mPendingCallback = callback;
        }
        mExecutor.execute(() -> {
            List<String> results = PredictionEngine.predict(lastWord, MAX_SUGGESTIONS, mUsageManager);
            deliverIfCurrent(seq, SuggestionUtils.filterToFit(results.toArray(new String[0]), availableWidth, regularPaint, boldPaint, chipPadPx, 1));
        });
    }

    public String getTopCorrection(String lastWord, String word) {
        return mCorrectionManager.getTopCorrection(lastWord, word, mUsageManager, this);
    }

    public String getContextualCorrection(String lastWord, String word) {
        return mCorrectionManager.getContextualCorrection(lastWord, word, mUsageManager);
    }

    public void learnRejection(String word)  { mUsageManager.learnRejection(word, this); }
    public void learnWordUse(String word)    { if (mReady.get()) mUsageManager.learnWordUse(word, mWords); }
    public void learnBigram(String f, String s) { if (mReady.get()) mUsageManager.learnBigram(f, s); }

    public boolean isValidWord(String word) {
        if (mWords == null || word == null) return false;
        return mWords.binarySearch(word.toLowerCase(Locale.ROOT)) >= 0;
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        final String[] spellSuggestions;
        if (results != null && results.length > 0) {
            int count = results[0].getSuggestionsCount();
            spellSuggestions = new String[count];
            for (int i = 0; i < count; i++) spellSuggestions[i] = results[0].getSuggestionAt(i);
        } else {
            spellSuggestions = null;
        }
        final String word; final long seq;
        final int width, chipPad;
        final android.graphics.Paint reg, bold;
        synchronized (mPendingLock) {
            word = mPendingWord; seq = mPendingSeq;
            width = mPendingWidth; chipPad = mPendingChipPad;
            reg = mPendingRegularPaint; bold = mPendingBoldPaint;
        }
        if (word == null) return;
        mExecutor.execute(() -> {
            String[] res = buildResults(null, word, spellSuggestions);
            deliverIfCurrent(seq, SuggestionUtils.filterToFit(res, width, reg, bold, chipPad, 1));
        });
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {}

    private void deliverIfCurrent(long seq, String[] results) {
        final SuggestionCallback cb; final Handler h;
        synchronized (mPendingLock) {
            if (seq != mPendingSeq) return;
            cb = mPendingCallback; h = mPendingHandler;
        }
        if (cb != null && h != null) h.post(() -> cb.onSuggestions(results));
    }

    public String[] buildResults(String lastWord, String word, String[] spellSuggestions) {
        return buildResults(lastWord, word, spellSuggestions, true);
    }

    public String[] buildResults(String lastWord, String word, String[] spellSuggestions, boolean includeCorrection) {
        List<String> results = new ArrayList<>();
        String correction = includeCorrection
                ? mCorrectionManager.getTopCorrection(lastWord, word, mUsageManager, this)
                : null;
        boolean hasCorrection = correction != null && !correction.equalsIgnoreCase(word);
        if (hasCorrection) { results.add(correction); results.add(word); }
        else                results.add(word);

        if (spellSuggestions != null) {
            for (String s : spellSuggestions) {
                if (!s.equalsIgnoreCase(word) && (correction == null || !s.equalsIgnoreCase(correction))) {
                    String matched = SuggestionUtils.matchCase(word, s);
                    if (!results.contains(matched)) {
                        results.add(matched);
                        if (results.size() >= MAX_SUGGESTIONS) break;
                    }
                }
            }
        }

        int remaining = MAX_SUGGESTIONS - results.size();
        if (remaining > 0) {
            List<String> prefixSugs = SuggestionSearcher.getPrefixSuggestions(lastWord, word, remaining + 5, results, mWords, this);
            for (String s : prefixSugs) {
                String matched = SuggestionUtils.matchCase(word, s);
                if (!results.contains(matched)) {
                    results.add(matched);
                    if (results.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }

        remaining = MAX_SUGGESTIONS - results.size();
        if (remaining > 0 && results.size() < 3) {
            List<String> fuzzy = SuggestionSearcher.getFuzzyMatches(lastWord, word, 3, results, mWords, this);
            for (String s : fuzzy) {
                String matched = SuggestionUtils.matchCase(word, s);
                if (!results.contains(matched)) {
                    results.add(matched);
                    if (results.size() >= MAX_SUGGESTIONS) break;
                }
            }
        }
        return results.toArray(new String[0]);
    }

    float calculateScore(String lastWord, int wordIndex) {
        byte category = mWords.getCategory(wordIndex);
        float base = (category < CAT_DEFAULT_BOOST.length) ? CAT_DEFAULT_BOOST[category] : 0f;
        String lowerWord = mWords.getLowerWord(wordIndex);
        float usageBoost  = mUsageManager.getWordUsage(lowerWord) * 20000f;
        float bigramBoost = 0f;
        if (lastWord != null) {
            Map<String, Integer> bigrams = mUsageManager.getBigramsFor(lastWord.toLowerCase(Locale.ROOT));
            if (bigrams != null) {
                Integer count = bigrams.get(lowerWord);
                if (count != null) bigramBoost = count * 100000f;
            }
        }
        float catUsage      = mUsageManager.getCategoryUsage(category) * 20f;
        float lengthPenalty = mWords.getWord(wordIndex).length() * 10f;
        return base + usageBoost + bigramBoost + catUsage - lengthPenalty;
    }

    public MmapDictionary getDictionary() { return mWords; }
}
