package com.swiftlite.keyboard.ime;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

public class PrivacyHandler {

    public static boolean isSensitiveField(EditorInfo info) {
        if (info == null) return false;

        int inputType  = info.inputType;
        int inputClass = inputType & InputType.TYPE_MASK_CLASS;
        int variation  = inputType & InputType.TYPE_MASK_VARIATION;

        boolean isPassword = (inputClass == InputType.TYPE_CLASS_TEXT && (
                variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
                || (inputClass == InputType.TYPE_CLASS_NUMBER
                && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        boolean isNoLearning = (info.imeOptions & 0x01000000) != 0;
        boolean isEmail      = isEmailField(info);
        boolean isSearch     = isSearchField(info);

        boolean isNoSuggestions = !isEmail && !isSearch
                && (inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0;

        boolean isWhatsAppPIN = false;
        if ("com.whatsapp".equals(info.packageName)
                && inputClass == InputType.TYPE_CLASS_NUMBER) {
            boolean noEnterAction = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
            boolean hintMatchesPIN = false;
            if (info.hintText != null) {
                String hint = info.hintText.toString().toLowerCase();
                hintMatchesPIN = hint.contains("pin") || hint.contains("code") || hint.contains("verif");
            }
            isWhatsAppPIN = hintMatchesPIN || noEnterAction;
        }

        if ("com.instagram.android".equals(info.packageName) && !isPassword) return false;

        return isPassword || isNoLearning || isNoSuggestions || isEmail || isWhatsAppPIN;
    }

    public static boolean isNumericField(EditorInfo info) {
        if (info == null) return false;
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        return inputClass == InputType.TYPE_CLASS_NUMBER
                || inputClass == InputType.TYPE_CLASS_PHONE
                || inputClass == InputType.TYPE_CLASS_DATETIME;
    }

    public static boolean isEmailField(EditorInfo info) {
        if (info == null) return false;
        int inputType  = info.inputType;
        int inputClass = inputType & InputType.TYPE_MASK_CLASS;
        int variation  = inputType & InputType.TYPE_MASK_VARIATION;

        if (inputClass == InputType.TYPE_CLASS_TEXT) {
            if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT) {
                return true;
            }
        }

        if (info.hintText != null) {
            String hint = info.hintText.toString().toLowerCase();
            if (hint.contains("email") || hint.contains("e-mail")) return true;
            if (hint.contains("address") || hint.contains("contact") || hint.contains("recipient")) {
                if (variation != InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS) return true;
            }
            if (hint.contains("login") || hint.contains("user") || hint.contains("account") || hint.contains("sign in")) {
                if (inputClass == InputType.TYPE_CLASS_TEXT) return true;
            }
        }

        if (info.fieldName != null) {
            String field = info.fieldName.toLowerCase();
            if (field.contains("email") || field.contains("mail")) return true;
            if ((field.contains("user") || field.contains("login"))
                    && (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)) {
                return true;
            }
        }

        String pkg = info.packageName != null ? info.packageName.toLowerCase() : "";
        boolean isBrowserPkg = pkg.contains("com.google.android.gms")
                || pkg.contains("com.android.chrome")
                || pkg.contains("org.chromium")
                || pkg.contains("browser")
                || pkg.contains("webview");
        if (isBrowserPkg) {
            boolean emailVariation = inputClass == InputType.TYPE_CLASS_TEXT
                    && (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                            || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
            boolean emailHint = info.hintText != null
                    && (info.hintText.toString().toLowerCase().contains("email")
                            || info.hintText.toString().toLowerCase().contains("e-mail"));
            return emailVariation || emailHint;
        }

        return false;
    }

    public static boolean isUriField(EditorInfo info) {
        if (info == null) return false;
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation  = info.inputType & InputType.TYPE_MASK_VARIATION;
        return inputClass == InputType.TYPE_CLASS_TEXT
                && variation == InputType.TYPE_TEXT_VARIATION_URI;
    }

    public static boolean isSearchField(EditorInfo info) {
        if (info == null) return false;
        int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation  = info.inputType & InputType.TYPE_MASK_VARIATION;
        int action     = info.imeOptions & EditorInfo.IME_MASK_ACTION;

        if ((inputClass == InputType.TYPE_CLASS_TEXT
                && (variation == InputType.TYPE_TEXT_VARIATION_FILTER
                    || variation == InputType.TYPE_TEXT_VARIATION_URI))
                || action == EditorInfo.IME_ACTION_SEARCH) {
            return true;
        }

        if (info.hintText != null
                && info.hintText.toString().toLowerCase().contains("search")) {
            return true;
        }

        return false;
    }
}
