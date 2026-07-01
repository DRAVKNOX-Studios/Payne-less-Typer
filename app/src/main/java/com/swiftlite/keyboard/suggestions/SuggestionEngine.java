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
import com.swiftlite.keyboard.utils.ProfanityFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SuggestionEngine implements SpellCheckerSession.SpellCheckerSessionListener {

    public interface SuggestionCallback {
        void onSuggestions(String[] suggestions);
    }

    private static final int MAX_SUGGESTIONS = 8;
    private static float[] CAT_DEFAULT_BOOST;

    private final Context mCtx;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private volatile SpellCheckerSession mSpellSession;
    private final AtomicBoolean mReady = new AtomicBoolean(false);

    private volatile MmapDictionary mWords;
    private final UsageManager mUsageManager;
    private final CorrectionManager mCorrectionManager;
    private final EmojiSuggestionProvider mEmojiProvider;
    private final ProfanityFilter mProfanityFilter;
    private final com.swiftlite.keyboard.theme.ThemeManager mThemeManager;

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
        mThemeManager = themeManager;
        mUsageManager = new UsageManager(mCtx);
        mCorrectionManager = new CorrectionManager(mCtx, themeManager);
        mEmojiProvider = new EmojiSuggestionProvider(mCtx);
        mProfanityFilter = new ProfanityFilter(mCtx);
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
        CAT_DEFAULT_BOOST = SuggestionResultBuilder.loadConfig(mCtx);
        PredictionData.init(mCtx);
        mWords = DictionaryLoader.load(mCtx);
        try {
            TextServicesManager tsm = (TextServicesManager) mCtx.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
            if (tsm != null) mSpellSession = tsm.newSpellCheckerSession(null, Locale.getDefault(), this, true);
        } catch (Exception ignored) {}
    }

    public void destroy() {
        mReady.set(false);
        mExecutor.shutdownNow();
        if (mSpellSession != null) { mSpellSession.close(); mSpellSession = null; }
        if (mWords != null) { try { mWords.close(); } catch (Exception ignored) {} mWords = null; }
    }

    public void getSuggestions(String lastWord, String word, int availableWidth, android.graphics.Paint regularPaint, android.graphics.Paint boldPaint, int chipPadPx, Handler handler, SuggestionCallback callback) {
        if (TextUtils.isEmpty(word) || handler == null || callback == null) {
            if (callback != null && handler != null) handler.post(() -> callback.onSuggestions(new String[0]));
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
            try {
                session.getSentenceSuggestions(new TextInfo[]{new TextInfo(word)}, MAX_SUGGESTIONS - 1);
            } catch (Exception ignored) {}
        }
    }

    public void getPredictions(String lastWord, int availableWidth, android.graphics.Paint regularPaint, android.graphics.Paint boldPaint, int chipPadPx, Handler handler, SuggestionCallback callback) {
        if (TextUtils.isEmpty(lastWord) || handler == null || callback == null) {
            if (callback != null && handler != null) handler.post(() -> callback.onSuggestions(new String[0]));
            return;
        }
        final long seq;
        synchronized (mPendingLock) { seq = ++mPendingSeq; mPendingWord = null; mPendingHandler = handler; mPendingCallback = callback; }
        mExecutor.execute(() -> {
            List<String> results = PredictionEngine.predict(lastWord, MAX_SUGGESTIONS, mUsageManager);
            if (mThemeManager.isProfanityFilterEnabled()) {
                List<String> filtered = new ArrayList<>();
                for (String s : results) if (!mProfanityFilter.isProfane(s)) filtered.add(s);
                results = filtered;
            }
            deliverIfCurrent(seq, SuggestionUtils.filterToFit(results.toArray(new String[0]), availableWidth, regularPaint, boldPaint, chipPadPx, 1));
        });
    }

    public String getTopCorrection(String lastWord, String word) { return mCorrectionManager.getTopCorrection(lastWord, word, mUsageManager, this); }
    public String getContextualCorrection(String lastWord, String word) { return mCorrectionManager.getContextualCorrection(lastWord, word, mUsageManager); }
    public void learnRejection(String word)  { mUsageManager.learnRejection(word, this); }
    public void learnWordUse(String word)    { if (mReady.get()) mUsageManager.learnWordUse(word, mWords); }
    public void learnBigram(String f, String s) { if (mReady.get()) mUsageManager.learnBigram(f, s); }

    public boolean isValidWord(String word) {
        if (mWords == null || word == null) return false;
        return mWords.binarySearch(word.toLowerCase(Locale.ROOT)) >= 0;
    }

    public boolean isProfane(String word) { return mProfanityFilter != null && mProfanityFilter.isProfane(word); }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        handleSpellCheckerResults(results);
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        if (results == null || results.length == 0) return;
        SentenceSuggestionsInfo ssi = results[0];
        SuggestionsInfo[] si = new SuggestionsInfo[ssi.getSuggestionsCount()];
        for (int i = 0; i < ssi.getSuggestionsCount(); i++) {
            si[i] = ssi.getSuggestionsInfoAt(i);
        }
        handleSpellCheckerResults(si);
    }

    private void handleSpellCheckerResults(SuggestionsInfo[] results) {
        final String[] spellSuggestions;
        if (results != null && results.length > 0) {
            int count = results[0].getSuggestionsCount();
            List<String> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String s = results[0].getSuggestionAt(i);
                if (s != null) list.add(s);
            }
            spellSuggestions = list.toArray(new String[0]);
        } else spellSuggestions = null;
        final String word; final long seq; final int width, chipPad; final android.graphics.Paint reg, bold;
        synchronized (mPendingLock) {
            word = mPendingWord; seq = mPendingSeq; width = mPendingWidth; chipPad = mPendingChipPad;
            reg = mPendingRegularPaint; bold = mPendingBoldPaint;
        }
        if (word == null) return;
        mExecutor.execute(() -> {
            String[] res = buildResults(null, word, spellSuggestions);
            deliverIfCurrent(seq, SuggestionUtils.filterToFit(res, width, reg, bold, chipPad, 1));
        });
    }

    private void deliverIfCurrent(long seq, String[] results) {
        final SuggestionCallback cb; final Handler h;
        synchronized (mPendingLock) { if (seq != mPendingSeq) return; cb = mPendingCallback; h = mPendingHandler; }
        if (cb != null && h != null) h.post(() -> cb.onSuggestions(results));
    }

    public String[] buildResults(String lastWord, String word, String[] spellSuggestions) { return buildResults(lastWord, word, spellSuggestions, true); }

    public String[] buildResults(String lastWord, String word, String[] spellSuggestions, boolean includeCorrection) {
        return SuggestionResultBuilder.build(this, lastWord, word, spellSuggestions, includeCorrection, mCorrectionManager, mThemeManager, mEmojiProvider);
    }

    float calculateScore(String lastWord, int wordIndex) {
        return SuggestionResultBuilder.calculateScore(this, lastWord, wordIndex, CAT_DEFAULT_BOOST, mUsageManager);
    }

    public MmapDictionary getDictionary() { return mWords; }
    public UsageManager getUsageManager() { return mUsageManager; }
    public com.swiftlite.keyboard.theme.ThemeManager getThemeManager() { return mThemeManager; }
}
