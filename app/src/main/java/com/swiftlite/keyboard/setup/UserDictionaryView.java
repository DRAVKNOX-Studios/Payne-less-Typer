package com.swiftlite.keyboard.setup;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDictionaryView extends LinearLayout {

    private final ThemeManager mThemeManager;
    private final KeyboardTheme mTheme;
    private LinearLayout mDictList;

    public UserDictionaryView(Context context, ThemeManager themeManager, KeyboardTheme theme) {
        super(context);
        mThemeManager = themeManager;
        mTheme = theme;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);

        LinearLayout addRow = new LinearLayout(getContext());
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText dictInput = new EditText(getContext());
        dictInput.setHint("Add new word...");
        dictInput.setHintTextColor(mTheme.isDark ? 0x55FFFFFF : 0x55000000);
        dictInput.setTextColor(mTheme.keyText);
        dictInput.setTextSize(14);
        dictInput.setPadding(UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 10), 
                             UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 10));
        dictInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        UIUtils.roundBg(dictInput, mTheme.keyBg, UIUtils.dp(getContext(), 8));
        addRow.addView(dictInput);

        addRow.addView(new View(getContext()){{ setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(getContext(), 8), 1)); }});

        Button addBtn = new Button(getContext());
        addBtn.setText("Add");
        addBtn.setAllCaps(false);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(getContext(), 70), UIUtils.dp(getContext(), 40)));
        UIUtils.roundBg(addBtn, mTheme.accent, UIUtils.dp(getContext(), 20));
        addBtn.setOnClickListener(v -> {
            String word = dictInput.getText().toString().trim();
            if (!word.isEmpty()) {
                String current = mThemeManager.getUserDictionary();
                String updated = current.isEmpty() ? word : current + "\n" + word;
                mThemeManager.setUserDictionary(updated);
                dictInput.setText("");
                refreshDictList();
            }
        });
        addRow.addView(addBtn);
        addView(addRow);

        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 12), 0));

        mDictList = new LinearLayout(getContext());
        mDictList.setOrientation(LinearLayout.VERTICAL);
        addView(mDictList);
        refreshDictList();
    }

    public void refreshDictList() {
        if (mDictList == null) return;
        mDictList.removeAllViews();
        String dict = mThemeManager.getUserDictionary();
        if (dict == null || dict.isEmpty()) return;

        String[] words = dict.split("\\n");
        for (String w : words) {
            String trimmed = w.trim();
            if (trimmed.isEmpty()) continue;
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(UIUtils.dp(getContext(), 4), UIUtils.dp(getContext(), 6), 
                            UIUtils.dp(getContext(), 4), UIUtils.dp(getContext(), 6));

            TextView tv = new TextView(getContext());
            tv.setText(trimmed);
            tv.setTextColor(mTheme.keyText);
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(tv);

            TextView delete = new TextView(getContext());
            delete.setText("✕");
            delete.setTextColor(mTheme.isDark ? 0x66FFFFFF : 0x66000000);
            delete.setPadding(UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 8), 
                              UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 8));
            final String finalDict = dict;
            delete.setOnClickListener(v -> {
                List<String> list = new ArrayList<>(Arrays.asList(finalDict.split("\\n")));
                list.remove(w);
                StringBuilder sb = new StringBuilder();
                for (String s : list) { if (sb.length() > 0) sb.append("\n"); sb.append(s); }
                mThemeManager.setUserDictionary(sb.toString());
                refreshDictList();
            });
            row.addView(delete);

            mDictList.addView(row);
            
            View div = new View(getContext());
            div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
            div.setBackgroundColor(mTheme.isDark ? 0x11FFFFFF : 0x11000000);
            mDictList.addView(div);
        }
    }
}
