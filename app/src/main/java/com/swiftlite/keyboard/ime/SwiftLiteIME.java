package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.inputmethodservice.InputMethodService;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

import com.swiftlite.keyboard.clipboard.ClipboardDatabase;
import com.swiftlite.keyboard.clipboard.ClipboardRepository;
import com.swiftlite.keyboard.suggestions.SuggestionEngine;
import com.swiftlite.keyboard.theme.ThemeManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SwiftLiteIME extends InputMethodService {

    private KeyboardView mKeyboardView;
    private ThemeManager mThemeManager;
    private volatile ClipboardRepository mClipboardRepo;
    private SuggestionEngine mSuggestionEngine;
    private EmojiHistoryManager mEmojiHistory;
    private ClipboardMonitor mClipboardMonitor;
    private InputLogicHandler mInputLogic;
    private UndoManager mUndoManager;

    private boolean mCapsLock = false, mShiftOn = false, mNumberMode = false;
    private long mLastShiftTime = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 400;
    private boolean mMonitorRequested = true;
    private EditorInfo mCurrentEditorInfo;
    private String mLastCommittedWord = null;

    private boolean mHasTypedThisSession = false;

    private ExecutorService mExecutor;
    private Handler mMainHandler;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefListener;

    private final Handler mIdleHandler = new Handler(Looper.getMainLooper());
    private final Runnable mIdleRunnable = () -> {
        if (!mHasTypedThisSession && mKeyboardView != null)
            mKeyboardView.setShowingIdleItems(true);
    };

    private void resetIdleTimer() {
        mIdleHandler.removeCallbacks(mIdleRunnable);
        if (!mHasTypedThisSession)
            mIdleHandler.postDelayed(mIdleRunnable, 30000);
    }

    private void userInteraction() {
        mHasTypedThisSession = true;
        mIdleHandler.removeCallbacks(mIdleRunnable);
        if (mKeyboardView != null) {
            mKeyboardView.setShowingIdleItems(false);
            mKeyboardView.dismissGooglyEyes();
        }
    }

    public boolean hasTypedThisSession()  { return mHasTypedThisSession; }
    public KeyboardView getKeyboardView() { return mKeyboardView; }

    public ExecutorService getExecutor() { return mExecutor; }
    public Handler getMainHandler()      { return mMainHandler; }
    public ThemeManager getThemeManager(){ return mThemeManager; }

    @Override public void onCreate() {
        super.onCreate();
        mThemeManager = new ThemeManager(this);
        mSuggestionEngine = new SuggestionEngine(this, mThemeManager);
        mEmojiHistory = new EmojiHistoryManager(this);
        com.swiftlite.keyboard.ime.ContractionHelper.init(this);
        mInputLogic = new InputLogicHandler(this, mSuggestionEngine, mThemeManager);
        mUndoManager = new UndoManager();
        mExecutor = Executors.newSingleThreadExecutor(); mMainHandler = new Handler(Looper.getMainLooper());
        mPrefListener = (prefs, key) -> mMainHandler.post(() -> {
            if (key == null) return;
            if (key.equals("theme_id") || key.equals("accent_color") || key.equals("font_size_multiplier") || key.equals("number_row")) {
                mThemeManager.reload(); if (mKeyboardView != null) mKeyboardView.setTheme(mThemeManager.getCurrentTheme());
            } else if (key.equals("user_dictionary")) {
                if (mSuggestionEngine != null) mSuggestionEngine.reloadUserDictionary();
            }
        });
        mThemeManager.registerListener(mPrefListener);
        mExecutor.execute(() -> {
            mClipboardRepo = new ClipboardRepository(ClipboardDatabase.getInstance(this), this);
            mMainHandler.post(() -> {
                mClipboardMonitor = new ClipboardMonitor(this, mClipboardRepo, mExecutor);
                applyMonitorState();
                if (mKeyboardView != null) mClipboardMonitor.setKeyboardView(mKeyboardView);
            });
        });
    }

    @Override public View onCreateInputView() {
        mKeyboardView = new KeyboardView(this, this);
        mKeyboardView.setTheme(mThemeManager.getCurrentTheme());
        if (mClipboardMonitor != null) mClipboardMonitor.setKeyboardView(mKeyboardView);
        return mKeyboardView;
    }

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        mCurrentEditorInfo = attribute; mLastCommittedWord = null;
        updatePrivacyAndPanel(attribute);
    }

    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting); mCurrentEditorInfo = info;
        if (mSuggestionEngine != null) mSuggestionEngine.ensureLoaded();
        if (mKeyboardView != null) {
            mKeyboardView.setVisibility(View.VISIBLE);
            if (!PrivacyHandler.isSensitiveField(info)) mKeyboardView.maybeShowGooglyEyes();
        }
        updatePrivacyAndPanel(info);
    }

    @Override public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        mIdleHandler.removeCallbacks(mIdleRunnable);
        if (mKeyboardView != null) {
            mKeyboardView.trimMemory();
            mKeyboardView.setVisibility(View.GONE);
        }
    }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_MODERATE && mKeyboardView != null) mKeyboardView.trimMemory();
    }

    private void updatePrivacyAndPanel(EditorInfo info) {
        if (info == null) return;
        mHasTypedThisSession = false;
        mNumberMode = PrivacyHandler.isNumericField(info);
        if (mKeyboardView != null) {
            mKeyboardView.showPanel(mNumberMode ? KeyboardView.PANEL_NUMBERS : KeyboardView.PANEL_KEYS, false);
            mKeyboardView.updateEditorInfo(info);
            mKeyboardView.updateShiftState(mShiftOn, mCapsLock);
            mKeyboardView.setShowingIdleItems(true);
            resetIdleTimer();
        }
        boolean isSensitive = PrivacyHandler.isSensitiveField(info);
        if (isSensitive) { clearSuggestions(); if (mKeyboardView != null) mKeyboardView.closePanels(); }
        mMonitorRequested = !isSensitive;
        applyMonitorState();
        if (mInputLogic != null) mInputLogic.updateAutoCap(getCurrentInputConnection());
    }

    private void applyMonitorState() {
        if (mClipboardMonitor != null) {
            if (mMonitorRequested) mClipboardMonitor.setup(); else mClipboardMonitor.destroy();
        }
    }

    @Override public void onFinishInput() {
        super.onFinishInput();
        if (mKeyboardView != null) mKeyboardView.closePanels();
        mIdleHandler.removeCallbacks(mIdleRunnable);
    }

    public void onKeyPress(int code, String label) {
        userInteraction();
        if (code != KeyboardView.KEY_SHIFT) mLastShiftTime = 0;
        InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        boolean isSensitive = PrivacyHandler.isSensitiveField(mCurrentEditorInfo);

        if (code == 0 && label != null && label.startsWith("@") && label.contains(".")) {
            ic.commitText(label, 1);
            mShiftOn = false;
            if (mKeyboardView != null) mKeyboardView.updateShiftState(false, mCapsLock);
            if (mInputLogic != null) mInputLogic.updateAutoCap(ic);
            return;
        }

        switch (code) {
            case KeyboardView.KEY_DELETE:    mInputLogic.handleDelete(ic); break;
            case KeyboardView.KEY_ENTER:     mInputLogic.handleEnter(ic, mCurrentEditorInfo); break;
            case KeyboardView.KEY_SPACE:     mInputLogic.handleSpace(ic, mShiftOn, mCapsLock); break;
            case KeyboardView.KEY_SHIFT:     handleShift(); break;
            case KeyboardView.KEY_UNDO:      mUndoManager.undo(ic); break;
            case KeyboardView.KEY_EMOJI:     if (!isSensitive) togglePanel(KeyboardView.PANEL_EMOJI); break;
            case KeyboardView.KEY_CLIPBOARD: if (!isSensitive) togglePanel(KeyboardView.PANEL_CLIPBOARD); break;
            case KeyboardView.KEY_NUMBERS:   togglePanel(KeyboardView.PANEL_NUMBERS); break;
            case KeyboardView.KEY_SWITCH_LANG:
                ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(); break;
            default: mInputLogic.handleCharacter(ic, code, label, mShiftOn, mCapsLock); break;
        }
        if (mKeyboardView != null) mKeyboardView.updateShiftState(mShiftOn, mCapsLock);
    }

    private void handleShift() {
        long now = System.currentTimeMillis();
        if (mCapsLock) {
            mCapsLock = false; mShiftOn = false;
        } else if (now - mLastShiftTime < DOUBLE_TAP_TIMEOUT) {
            mCapsLock = true; mShiftOn = true;
        } else {
            mShiftOn = !mShiftOn;
        }
        mLastShiftTime = now;
    }

    public void setShift(boolean on) { mShiftOn = on; if (mKeyboardView != null) mKeyboardView.updateShiftState(mShiftOn, mCapsLock); }
    public boolean isCapsLock() { return mCapsLock; }

    public void commitSuggestion(String word) {
        userInteraction(); InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        mInputLogic.commitSuggestion(ic, word); mLastCommittedWord = word;
    }

    public void commitEmoji(String emoji) {
        userInteraction(); InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        ic.commitText(emoji, 1);
        if (!PrivacyHandler.isSensitiveField(mCurrentEditorInfo)) mEmojiHistory.trackRecentEmoji(emoji);
        if (mKeyboardView != null) mKeyboardView.notifyEmojiUsed();
    }

    public void commitClipboard(String text) {
        userInteraction(); InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        ic.commitText(text, 1); if (mKeyboardView != null) mKeyboardView.showPanel(KeyboardView.PANEL_KEYS);
        mInputLogic.updateAutoCap(ic);
    }

    public void commitClipboardImage(String uriStr) {
        userInteraction();
        if (uriStr == null || uriStr.isEmpty()) return;
        InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        Uri uri = Uri.parse(uriStr);
        if (mClipboardMonitor != null) mClipboardMonitor.setLastSelfCopiedUri(uriStr);
        android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) {
            android.content.ClipData clip = android.content.ClipData.newUri(getContentResolver(), "image", uri);
            cm.setPrimaryClip(clip);
        }
        EditorInfo editorInfo = mCurrentEditorInfo;
        if (editorInfo == null || editorInfo.packageName == null) return;
        try { grantUriPermission(editorInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        String[] supportedMimes = EditorInfoCompat.getContentMimeTypes(editorInfo);
        String bestMime = null;
        for (String m : supportedMimes) {
            if (m.startsWith("image/")) { if (bestMime == null || m.equals("image/jpeg")) bestMime = m; }
        }
        if (bestMime != null) {
            InputContentInfoCompat info = new InputContentInfoCompat(uri, new android.content.ClipDescription("image", new String[]{bestMime}), null);
            InputConnectionCompat.commitContent(ic, editorInfo, info, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        }
        if (mKeyboardView != null) mKeyboardView.showPanel(KeyboardView.PANEL_KEYS);
    }

    public void resetLastCommittedWord() { mLastCommittedWord = null; }
    public void learnWordUse(String word) { if (mSuggestionEngine != null) mSuggestionEngine.learnWordUse(word); }
    public EditorInfo getCurrentInputEditorInfo() { return mCurrentEditorInfo; }

    public void updateSuggestions(String last, String word) {
        int w = (mKeyboardView != null) ? mKeyboardView.getSuggestionWidth() : 0;
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        mSuggestionEngine.getSuggestions(last, word, w, mKeyboardView.getSuggestionPaint(false), mKeyboardView.getSuggestionPaint(true), mKeyboardView.getSuggestionPadding(), mMainHandler, s -> { if (mKeyboardView != null) mKeyboardView.updateSuggestions(s); });
    }

    public void updatePredictions(String last) {
        int w = (mKeyboardView != null) ? mKeyboardView.getSuggestionWidth() : 0;
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        mSuggestionEngine.getPredictions(last, w, mKeyboardView.getSuggestionPaint(false), mKeyboardView.getSuggestionPaint(true), mKeyboardView.getSuggestionPadding(), mMainHandler, p -> { if (mKeyboardView != null) mKeyboardView.updateSuggestions(p); });
    }

    public void clearSuggestions() { if (mKeyboardView != null) mKeyboardView.updateSuggestions(new String[0]); }
    public boolean isNumberMode() { return mNumberMode; }
    public String[] getRecentEmojis() { return mEmojiHistory.getRecentEmojis(); }
    public String getSelectedEmojiSkin() { return mEmojiHistory.getSelectedSkinTone(); }
    public void saveSelectedEmojiSkin(String tone) { mEmojiHistory.saveSelectedSkinTone(tone); }
    public ClipboardRepository getClipboardRepository() { return mClipboardRepo; }

    private void togglePanel(int panel) {
        if (panel == KeyboardView.PANEL_CLIPBOARD && PrivacyHandler.isSensitiveField(mCurrentEditorInfo)) return;
        if (mKeyboardView != null) mKeyboardView.togglePanel(panel);
    }

    @Override public void onDestroy() {
        if (mClipboardMonitor != null) mClipboardMonitor.destroy();
        if (mThemeManager != null && mPrefListener != null) mThemeManager.unregisterListener(mPrefListener);
        if (mExecutor != null) mExecutor.shutdownNow();
        super.onDestroy();
    }
}
