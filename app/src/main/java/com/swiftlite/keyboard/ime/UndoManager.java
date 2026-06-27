package com.swiftlite.keyboard.ime;

import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;

public class UndoManager {

    public void undo(InputConnection ic) {
        if (ic == null) return;
        
        long now = android.os.SystemClock.uptimeMillis();
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, 
                KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON));
        ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, 
                KeyEvent.KEYCODE_Z, 0, KeyEvent.META_CTRL_ON));
    }
}
