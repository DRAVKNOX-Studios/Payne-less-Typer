package com.swiftlite.keyboard.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.core.view.inputmethod.EditorInfoCompat;
import androidx.core.view.inputmethod.InputConnectionCompat;
import androidx.core.view.inputmethod.InputContentInfoCompat;

public class RichContentHandler {

    public static void commitImage(Context context, InputConnection ic, EditorInfo editorInfo, String uriStr) {
        if (uriStr == null || uriStr.isEmpty() || ic == null || editorInfo == null) return;
        Uri uri = Uri.parse(uriStr);

        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            ClipData clip = ClipData.newUri(context.getContentResolver(), "image", uri);
            cm.setPrimaryClip(clip);
        }

        if (editorInfo.packageName != null) {
            try {
                context.grantUriPermission(editorInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
        }

        String[] supportedMimes = EditorInfoCompat.getContentMimeTypes(editorInfo);
        if (supportedMimes == null) return;

        String bestMime = null;
        for (String m : supportedMimes) {
            if (m.startsWith("image/")) {
                if (bestMime == null || m.equals("image/jpeg")) bestMime = m;
            }
        }

        if (bestMime != null) {
            InputContentInfoCompat info = new InputContentInfoCompat(uri, new android.content.ClipDescription("image", new String[]{bestMime}), null);
            InputConnectionCompat.commitContent(ic, editorInfo, info, InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        }
    }
}
