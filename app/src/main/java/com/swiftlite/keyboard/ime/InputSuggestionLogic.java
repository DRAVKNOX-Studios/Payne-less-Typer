package com.swiftlite.keyboard.ime;

import android.text.TextUtils;
import android.view.inputmethod.InputConnection;
import com.swiftlite.keyboard.suggestions.SuggestionEngine;
import com.swiftlite.keyboard.utils.SuggestionUtils;

public class InputSuggestionLogic {

    private final SwiftLiteIME mIME;
    private final SuggestionEngine mSuggestionEngine;

    public InputSuggestionLogic(SwiftLiteIME ime, SuggestionEngine engine) {
        mIME = ime;
        mSuggestionEngine = engine;
    }

    public void updateSuggestionsFromCursor(InputConnection ic) {
        if (PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) {
            mIME.clearSuggestions(); return;
        }
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        if (TextUtils.isEmpty(before)) { mIME.clearSuggestions(); return; }
        
        String currentWord = SuggestionUtils.getCurrentWord(before);
        int start = currentWord != null ? before.length() - currentWord.length() : before.length();
        String prevWord    = SuggestionUtils.findWordBefore(before, start);
        if (currentWord != null && currentWord.length() >= 1) {
            mIME.updateSuggestions(prevWord, currentWord);
        } else {
            String lastWord = SuggestionUtils.findWordBefore(before, before.length());
            if (lastWord != null) mIME.updatePredictions(lastWord);
            else mIME.clearSuggestions();
        }
    }

    public void commitSuggestion(InputConnection ic, String suggestion) {
        String lastWordTyped = null;
        CharSequence before = ic.getTextBeforeCursor(50, 0);
        CharSequence after = ic.getTextAfterCursor(50, 0);

        int beforeDelete = 0, afterDelete = 0;
        if (!TextUtils.isEmpty(before)) {
            int end = before.length(), start = end;
            while (start > 0 && (Character.isLetter(before.charAt(start - 1)) 
                    || before.charAt(start - 1) == '\'' 
                    || before.charAt(start - 1) == ':')) start--;
            if (start < end) {
                lastWordTyped = before.subSequence(start, end).toString();
                beforeDelete = end - start;
            }
        }
        if (!TextUtils.isEmpty(after)) {
            int end = 0;
            while (end < after.length() && (Character.isLetter(after.charAt(end)) 
                    || after.charAt(end) == '\'' 
                    || after.charAt(end) == ':')) end++;
            afterDelete = end;
        }

        ic.deleteSurroundingText(beforeDelete, afterDelete);
        ic.commitText(suggestion + " ", 1);

        if (!PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) mIME.learnWordUse(suggestion);

        if (before != null && !TextUtils.isEmpty(before) && !PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) {
            int wordStart = (lastWordTyped != null) ? (before.length() - lastWordTyped.length()) : before.length();
            String prevWord = SuggestionUtils.findWordBefore(before, wordStart);
            if (prevWord != null) mSuggestionEngine.learnBigram(prevWord, suggestion);
        }
        mIME.updatePredictions(suggestion);
    }
    
    public void learnBigram(CharSequence before, String typedWord) {
        if (typedWord != null && !PrivacyHandler.isSensitiveField(mIME.getCurrentInputEditorInfo())) {
            String wordBeforeThat = SuggestionUtils.findWordBefore(before, before.length() - typedWord.length());
            if (wordBeforeThat != null) mSuggestionEngine.learnBigram(wordBeforeThat, typedWord);
        }
    }
}
