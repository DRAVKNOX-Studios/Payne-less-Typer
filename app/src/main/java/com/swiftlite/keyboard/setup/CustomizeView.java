package com.swiftlite.keyboard.setup;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.UIUtils;

public class CustomizeView extends LinearLayout {

    private final SetupActivity mActivity;
    private final ThemeManager mThemeManager;
    private final KeyboardTheme mTheme;

    public CustomizeView(SetupActivity activity, ThemeManager themeManager, KeyboardTheme theme) {
        super(activity);
        mActivity = activity;
        mThemeManager = themeManager;
        mTheme = theme;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(UIUtils.dp(getContext(), 16), UIUtils.dp(getContext(), 16), 
                   UIUtils.dp(getContext(), 16), UIUtils.dp(getContext(), 32));

        addView(UIUtils.makeLabel(getContext(), "Theme", mTheme.accent));
        addView(ThemePickerView.buildThemePicker(getContext(), mThemeManager, mTheme, mActivity));
        
        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 16), 0));
        addView(UIUtils.makeLabel(getContext(), "Accent Color", mTheme.accent));
        addView(ThemePickerView.buildAccentPicker(getContext(), mThemeManager, mTheme, mActivity));

        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 24), 0));

        addCheckBox("Auto-correction", mThemeManager.isAutoCorrectEnabled(), (v, checked) -> mThemeManager.setAutoCorrectEnabled(checked));
        addCheckBox("Auto-capitalization", mThemeManager.isAutoCapEnabled(), (v, checked) -> mThemeManager.setAutoCapEnabled(checked));
        addCheckBox("Auto-spacing", mThemeManager.isAutoSpaceEnabled(), (v, checked) -> mThemeManager.setAutoSpaceEnabled(checked));
        addCheckBox("Auto-apostrophe", mThemeManager.isAutoApostropheEnabled(), (v, checked) -> mThemeManager.setAutoApostropheEnabled(checked));
        addCheckBox("Number row", mThemeManager.isNumberRowEnabled(), (v, checked) -> mThemeManager.setNumberRowEnabled(checked));
        addCheckBox("Vibrate on keypress", mThemeManager.isVibrateEnabled(), (v, checked) -> mThemeManager.setVibrateEnabled(checked));

        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 16), 0));
        addView(UIUtils.makeLabel(getContext(), "Keyboard Font Size", mTheme.accent));

        SeekBar fontSlider = new SeekBar(getContext());
        fontSlider.setMax(100);
        fontSlider.setProgress((int) ((mThemeManager.getFontSizeMultiplier() - 0.5f) * 100));
        fontSlider.setThumbTintList(ColorStateList.valueOf(mTheme.accent));
        fontSlider.setProgressTintList(ColorStateList.valueOf(mTheme.accent));
        fontSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) mThemeManager.setFontSizeMultiplier(0.5f + (progress / 100f));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        addView(fontSlider);

        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 24), 0));

        addView(UIUtils.makeLabel(getContext(), "User Dictionary", mTheme.accent));
        addView(new UserDictionaryView(getContext(), mThemeManager, mTheme));
    }

    private void addCheckBox(String label, boolean initial, OnCheckedChangeListener listener) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UIUtils.dp(getContext(), 8), 0, UIUtils.dp(getContext(), 8));

        TextView tv = new TextView(getContext());
        tv.setText(label);
        tv.setTextColor(mTheme.keyText);
        tv.setTextSize(15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        CheckBox cb = new CheckBox(getContext());
        cb.setButtonTintList(ColorStateList.valueOf(mTheme.accent));
        cb.setChecked(initial);
        cb.setOnCheckedChangeListener((v, checked) -> listener.onCheckedChanged(cb, checked));
        row.addView(cb);
        addView(row);
    }
    
    private interface OnCheckedChangeListener {
        void onCheckedChanged(CheckBox v, boolean checked);
    }
}
