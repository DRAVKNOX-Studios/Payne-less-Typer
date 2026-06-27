package com.swiftlite.keyboard.ime;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.swiftlite.keyboard.utils.SuggestionUtils;

public class InputLogicHandler {

    private final SwiftLiteIME mIME;
    private final com.swiftlite.keyboard.suggestions.SuggestionEngine mSuggestionEngine;
    private final com.swiftlite.keyboard.theme.ThemeManager mThemeManager;
    private final InputCorrectionLogic mCorrectionLogic;
    private final InputSuggestionLogic mSuggestionLogic;
    private boolean mJustReverted = false;

    private static final String GOOGLY_TRIGGER = "googly eyes on";

    public InputLogicHandler(SwiftLiteIME ime,
            com.swiftlite.keyboard.suggestions.SuggestionEngine engine,
            com.swiftlite.keyboard.theme.ThemeManager themeManager) {
        mIME = ime;
        mSuggestionEngine = engine;
        mThemeManager = themeManager;
        mCorrectionLogic = new InputCorrectionLogic(ime, engine, themeManager);
        mSuggestionLogic = new InputSuggestionLogic(ime, engine);
    }

    public void handleCharacter(InputConnection ic, int code, String label, boolean shiftOn, boolean capsLock) {
        mJustReverted = false;
        String res = (label != null && !label.isEmpty()) ? label : (code > 0 ? "" + (char) code : null);
        if (res == null) return;
        String text = res;
        if (shiftOn || capsLock) { text = text.toUpperCase(); if (shiftOn && !capsLock) mIME.setShift(false); }

        if (text.length() == 1 && SuggestionUtils.isPunctuation(text.charAt(0))) pullPunctuation(ic);

        ic.commitText(text, 1);
        mCorrectionLogic.clearOnType(text);

        if (text.equals("'") && !mIME.isNumberMode()) mCorrectionLogic.handleAutoApostrophe(ic);

        if (text.length() == 1 && mThemeManager.isAutoSpaceEnabled() && SuggestionUtils.isPunctuation(text.charAt(0)) && !mIME.isNumberMode()) handleAutoSpace(ic);

        if (text.length() == 1 && !Character.isLetter(text.charAt(0)) && text.charAt(0) != '\'') mIME.resetLastCommittedWord();
        mSuggestionLogic.updateSuggestionsFromCursor(ic);
        updateAutoCap(ic);
    }

    private void pullPunctuation(InputConnection ic) {
        EditorInfo info = mIME.getCurrentInputEditorInfo();
        if (PrivacyHandler.isUriField(info) || PrivacyHandler.isEmailField(info) || PrivacyHandler.isSearchField(info) || PrivacyHandler.isNumericField(info) || PrivacyHandler.isSensitiveField(info)) return;
        CharSequence before = ic.getTextBeforeCursor(5, 0);
        if (TextUtils.isEmpty(before)) return;
        int spaces = 0;
        while (spaces < before.length() && before.charAt(before.length() - 1 - spaces) == ' ') spaces++;
        if (spaces > 0) ic.deleteSurroundingText(spaces, 0);
    }

    private void handleAutoSpace(InputConnection ic) {
        EditorInfo info = mIME.getCurrentInputEditorInfo();
        if (PrivacyHandler.isUriField(info) || PrivacyHandler.isEmailField(info) || PrivacyHandler.isSearchField(info) || PrivacyHandler.isNumericField(info)) return;
        CharSequence before = ic.getTextBeforeCursor(2, 0);
        if (before != null && before.length() >= 2 && (Character.isLetterOrDigit(before.charAt(0)) || before.charAt(0) == '\'' || before.charAt(0) == '\"')) ic.commitText(" ", 1);
    }

    public void handleSpace(InputConnection ic, boolean shiftOn, boolean capsLock) {
        mJustReverted = false;
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        String typedWord = SuggestionUtils.getTypedWord(before);

        checkGooglyTrigger(ic);

        if (!PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) mCorrectionLogic.performAutoCorrect(ic);

        ic.commitText(" ", 1);
        mJustReverted = false;
        mCorrectionLogic.onSpace(typedWord);
        mSuggestionLogic.learnBigram(before, typedWord);

        mSuggestionLogic.updateSuggestionsFromCursor(ic);
        updateAutoCap(ic);
    }

    private void checkGooglyTrigger(InputConnection ic) {
        if (PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) return;
        int len = GOOGLY_TRIGGER.length() + 1;
        CharSequence before = ic.getTextBeforeCursor(len, 0);
        if (before == null || before.length() < len) return;
        if (!before.toString().toLowerCase().equals(GOOGLY_TRIGGER + " ")) return;

        KeyboardView kv = mIME.getKeyboardView();
        if (kv != null) kv.forceShowGooglyEyes();
    }

    public void handleEnter(InputConnection ic, EditorInfo info) {
        if (!PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) mCorrectionLogic.performAutoCorrect(ic);
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        String lastWord = SuggestionUtils.getTypedWord(before);
        if (lastWord != null && !PrivacyHandler.isSensitiveField(info)) {
            mIME.learnWordUse(lastWord);
            mSuggestionLogic.learnBigram(before, lastWord);
        }
        if (info == null) { ic.commitText("\n", 1); updateAutoCap(ic); return; }
        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED || (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            ic.commitText("\n", 1); updateAutoCap(ic);
        } else { ic.performEditorAction(action); }
    }

    public void commitSuggestion(InputConnection ic, String suggestion) {
        mSuggestionLogic.commitSuggestion(ic, suggestion);
    }

    public void handleDelete(InputConnection ic) {
        if (!mJustReverted && mCorrectionLogic.tryRevert(ic)) {
            mJustReverted = true;
            mSuggestionLogic.updateSuggestionsFromCursor(ic);
            updateAutoCap(ic);
            return;
        }
        mJustReverted = false;
        mCorrectionLogic.clear();
        CharSequence sel = ic.getSelectedText(0);
        if (!TextUtils.isEmpty(sel)) { ic.commitText("", 1); }
        else { ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)); ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)); }
        mSuggestionLogic.updateSuggestionsFromCursor(ic);
        updateAutoCap(ic);
    }

    public void updateSuggestionsFromCursor(InputConnection ic) {
        mSuggestionLogic.updateSuggestionsFromCursor(ic);
    }

    public void updateAutoCap(InputConnection ic) {
        if (ic == null) return;
        if (!mThemeManager.isAutoCapEnabled()) { if (!mIME.isCapsLock()) mIME.setShift(false); return; }
        if (mIME.isCapsLock()) return;
        EditorInfo info = mIME.getCurrentInputEditorInfo();
        if (PrivacyHandler.isSensitiveField(info) || PrivacyHandler.isEmailField(info)) return;
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        boolean cap = false;
        if (TextUtils.isEmpty(before)) { cap = true; }
        else { String s = before.toString().trim(); if (s.isEmpty()) cap = true; else { char last = s.charAt(s.length() - 1); cap = (last == '.' || last == '!' || last == '?'); } }
        mIME.setShift(cap);
    }
}
