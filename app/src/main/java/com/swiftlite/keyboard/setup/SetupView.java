package com.swiftlite.keyboard.setup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.ime.IconView;
import com.swiftlite.keyboard.ime.KeyIcons;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.UIUtils;

public class SetupView extends LinearLayout {

    private final SetupActivity mActivity;
    private final KeyboardTheme mTheme;
    private TextView mStep1Status, mStep2Status, mStep3Status;

    public SetupView(SetupActivity activity, KeyboardTheme theme) {
        super(activity);
        mActivity = activity;
        mTheme = theme;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(UIUtils.dp(getContext(), 20), UIUtils.dp(getContext(), 20), 
                   UIUtils.dp(getContext(), 20), UIUtils.dp(getContext(), 24));

        addView(UIUtils.vspace(getContext(), 0, 1f));

        addView(makeStepCard(KeyIcons.IC_ALPHA, "STEP 1",
                "Enable Payne-less: Typer in Input Method settings",
                "Open Settings", mTheme.accent,
                v -> getContext().startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        mStep1Status = makeStatusText();
        addView(mStep1Status);
        
        addView(UIUtils.vspace(getContext(), 0, 1f));

        addView(makeStepCard(KeyIcons.IC_LOGO, "STEP 2",
                "Select Payne-less: Typer as your default keyboard",
                "Switch Keyboard", mTheme.specialKey,
                v -> ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showInputMethodPicker()));
        mStep2Status = makeStatusText();
        addView(mStep2Status);

        addView(UIUtils.vspace(getContext(), 0, 1f));

        addView(makeStepCard(KeyIcons.IC_CLIPBOARD, "STEP 3",
                "Grant permission to save copied screenshots",
                "Grant Permission", mTheme.accent,
                v -> requestStoragePermission()));
        mStep3Status = makeStatusText();
        addView(mStep3Status);

        addView(UIUtils.vspace(getContext(), 0, 1.5f));
        
        updateStatus();
    }

    public void updateStatus() {
        boolean enabled  = isKeyboardEnabled();
        boolean selected = isKeyboardDefault();
        boolean perm     = hasStoragePermission();
        mStep1Status.setText(enabled  ? "✓ Keyboard enabled" : "○ Not yet enabled");
        mStep1Status.setTextColor(enabled  ? 0xFF10B981 : 0xFFEF4444);
        mStep2Status.setText(selected ? "✓ Set as default"   : "○ Not yet selected");
        mStep2Status.setTextColor(selected ? 0xFF10B981 : 0xFFEF4444);
        mStep3Status.setText(perm ? "✓ Storage permission granted" : "○ Storage permission required");
        mStep3Status.setTextColor(perm ? 0xFF10B981 : 0xFFEF4444);
    }

    private boolean isKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        List<InputMethodInfo> enabledInputMethodList = imm.getEnabledInputMethodList();
        String packageName = getContext().getPackageName();
        for (InputMethodInfo info : enabledInputMethodList) {
            if (info.getPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKeyboardDefault() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                InputMethodInfo currentImeInfo = imm.getCurrentInputMethodInfo();
                return currentImeInfo != null && currentImeInfo.getPackageName().equals(getContext().getPackageName());
            }
        }
        String s = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return s != null && s.contains(getContext().getPackageName());
    }

    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 123);
        } else {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
        }
    }

    private View makeStepCard(int icon, String stepLabel, String desc, String btnText, int btnColor, View.OnClickListener l) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(UIUtils.dp(getContext(), 16), UIUtils.dp(getContext(), 16), 
                        UIUtils.dp(getContext(), 16), UIUtils.dp(getContext(), 16));
        UIUtils.roundBg(card, mTheme.keyBg, UIUtils.dp(getContext(), 12));

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, UIUtils.dp(getContext(), 8));

        IconView iv = new IconView(getContext());
        iv.setIcon(icon);
        iv.setColor(mTheme.accent);
        iv.setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(getContext(), 22), UIUtils.dp(getContext(), 22)));
        row.addView(iv);

        TextView step = new TextView(getContext());
        step.setText(stepLabel);
        step.setTextSize(11);
        step.setTypeface(null, Typeface.BOLD);
        step.setTextColor(mTheme.accent);
        step.setPadding(UIUtils.dp(getContext(), 10), 0, 0, 0);
        row.addView(step);
        card.addView(row);

        TextView descTv = new TextView(getContext());
        descTv.setText(desc);
        descTv.setTextSize(14);
        descTv.setTextColor(mTheme.keyText);
        descTv.setPadding(0, 0, 0, UIUtils.dp(getContext(), 12));
        card.addView(descTv);

        Button btn = makeBtn(btnText, btnColor);
        btn.setOnClickListener(l);
        card.addView(btn);
        return card;
    }

    private Button makeBtn(String text, int color) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UIUtils.dp(getContext(), 44)));
        UIUtils.roundBg(b, color, UIUtils.dp(getContext(), 22));
        return b;
    }

    private TextView makeStatusText() {
        TextView tv = new TextView(getContext());
        tv.setTextSize(12);
        tv.setPadding(UIUtils.dp(getContext(), 8), UIUtils.dp(getContext(), 4), 0, 0);
        return tv;
    }
}
