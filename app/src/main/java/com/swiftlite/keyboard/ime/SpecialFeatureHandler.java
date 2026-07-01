package com.swiftlite.keyboard.ime;

import android.view.inputmethod.InputConnection;

public class SpecialFeatureHandler {
    private static final String GOOGLY_TRIGGER = "googly eyes on";

    public static void checkTriggers(InputConnection ic, SwiftLiteIME ime) {
        if (ic == null || ime == null) return;
        checkGoogly(ic, ime);
    }

    private static void checkGoogly(InputConnection ic, SwiftLiteIME ime) {
        CharSequence cs = ic.getTextBeforeCursor(GOOGLY_TRIGGER.length(), 0);
        if (cs != null && GOOGLY_TRIGGER.equalsIgnoreCase(cs.toString())) {
            KeyboardView kv = ime.getKeyboardView();
            if (kv != null) {
                kv.forceShowGooglyEyes();
                ic.deleteSurroundingText(GOOGLY_TRIGGER.length(), 0);
            }
        }
    }
}
