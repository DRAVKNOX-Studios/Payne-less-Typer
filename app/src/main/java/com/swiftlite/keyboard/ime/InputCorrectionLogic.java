package com.swiftlite.keyboard.ime;

import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import com.swiftlite.keyboard.suggestions.SuggestionEngine;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.SuggestionUtils;

public class InputCorrectionLogic {

    private final SwiftLiteIME mIME;
    private final SuggestionEngine mSuggestionEngine;
    private final ThemeManager mThemeManager;

    private String mLastTypedWord = null;
    private String mLastCorrectedWord = null;
    private String mLastRevertedWord = null;

    public InputCorrectionLogic(SwiftLiteIME ime, SuggestionEngine engine, ThemeManager themeManager) {
        mIME = ime;
        mSuggestionEngine = engine;
        mThemeManager = themeManager;
    }

    public void performAutoCorrect(InputConnection ic) {
        EditorInfo info = mIME.getCurrentInputEditorInfo();
        if (PrivacyHandler.isUriField(info) || PrivacyHandler.isSearchField(info)) return;

        CharSequence before = ic.getTextBeforeCursor(50, 0);
        if (TextUtils.isEmpty(before)) return;

        String s = before.toString();
        int lastSpace = s.lastIndexOf(' ');
        String currentToken = s.substring(lastSpace + 1);
        if (currentToken.startsWith("http://") || currentToken.startsWith("https://")) return;

        int end = before.length(), start = end;
        while (start > 0 && (Character.isLetter(before.charAt(start - 1)) || before.charAt(start - 1) == '\'')) start--;
        if (start >= end) return;
        if (start > 0 && before.charAt(start - 1) == '.') return;

        String typed = before.subSequence(start, end).toString();
        if (typed.equalsIgnoreCase(mLastRevertedWord)) return;

        String prevWord = SuggestionUtils.findWordBefore(before, start);
        String corrected = mSuggestionEngine.getTopCorrection(prevWord, typed);
        if (corrected != null && !corrected.equals(typed)) {
            int afterDelete = 0;
            CharSequence after = ic.getTextAfterCursor(50, 0);
            if (!TextUtils.isEmpty(after)) {
                int e = 0;
                while (e < after.length() && (Character.isLetter(after.charAt(e)) || after.charAt(e) == '\'')) e++;
                afterDelete = e;
            }
            ic.deleteSurroundingText(end - start, afterDelete);
            ic.commitText(corrected, 1);
            mLastTypedWord = typed;
            mLastCorrectedWord = corrected;
        }
    }

    public boolean tryRevert(InputConnection ic) {
        if (mLastCorrectedWord != null) {
            CharSequence before = ic.getTextBeforeCursor(mLastCorrectedWord.length() + 1, 0);
            if (before != null) {
                String s = before.toString();
                if (s.equals(mLastCorrectedWord + " ") || s.equals(mLastCorrectedWord)) {
                    boolean hadSpace = s.endsWith(" ");
                    ic.deleteSurroundingText(s.length(), 0);
                    ic.commitText(mLastTypedWord + (hadSpace ? " " : ""), 1);
                    mLastRevertedWord = mLastTypedWord;
                    mSuggestionEngine.learnRejection(mLastTypedWord);
                    mLastTypedWord = null;
                    mLastCorrectedWord = null;
                    return true;
                }
            }
        }
        clear();
        return false;
    }

    public void handleAutoApostrophe(InputConnection ic) {
        if (!mThemeManager.isAutoApostropheEnabled()) return;
        CharSequence before = ic.getTextBeforeCursor(32, 0);
        if (TextUtils.isEmpty(before)) return;
        String s = before.toString();
        int len = s.length();
        if (len < 2 || s.charAt(len - 1) != '\'') return;
        
        int i = len - 2;
        while (i >= 0 && Character.isLetter(s.charAt(i))) i--;
        String word = s.substring(i + 1, len - 1);
        if (word.isEmpty()) return;
        
        String finalCorrection = ContractionHelper.getContraction(word);
        if (finalCorrection != null) {
            int afterDelete = 0;
            CharSequence after = ic.getTextAfterCursor(50, 0);
            if (!TextUtils.isEmpty(after)) {
                int e = 0;
                while (e < after.length() && (Character.isLetter(after.charAt(e)) || after.charAt(e) == '\'')) e++;
                afterDelete = e;
            }
            ic.deleteSurroundingText(word.length() + 1, afterDelete);
            ic.commitText(finalCorrection, 1);
            mLastTypedWord = word + "'";
            mLastCorrectedWord = finalCorrection;
        }
    }

    public void clear() {
        mLastTypedWord = null;
        mLastCorrectedWord = null;
        mLastRevertedWord = null;
    }

    public void clearOnType(String text) {
        if (!Character.isLetter(text.charAt(0)) && text.charAt(0) != '\'') {
            mLastRevertedWord = null;
        }
        mLastTypedWord = null;
        mLastCorrectedWord = null;
    }

    public void onSpace(String typedWord) {
        if (typedWord != null && !typedWord.equalsIgnoreCase(mLastRevertedWord)) {
            mLastRevertedWord = null;
        }
    }

    public void setLastRevertedWord(String word) { mLastRevertedWord = word; }
}
