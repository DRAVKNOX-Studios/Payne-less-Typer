package com.swiftlite.keyboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.swiftlite.keyboard.ime.IconView;
import com.swiftlite.keyboard.ime.KeyIcons;
import com.swiftlite.keyboard.setup.CustomizeView;
import com.swiftlite.keyboard.setup.SetupView;
import com.swiftlite.keyboard.setup.TesterView;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.theme.ThemeManager;
import com.swiftlite.keyboard.utils.UIUtils;

public class SetupActivity extends AppCompatActivity {

    private static int sSelectedTab = 0;

    private ThemeManager mThemeManager;
    private KeyboardTheme mTheme;

    private SetupView mSetupPage;
    private CustomizeView mCustomizePage;
    private TesterView mTesterPage;
    private TextView mTabSetup, mTabCustomize, mTabTester;
    private LinearLayout mPagesContainer;

    private int mLastThemeId = -1;
    private int mLastAccent = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mThemeManager = new ThemeManager(this);
        mTheme = mThemeManager.getCurrentTheme();

        getWindow().setBackgroundDrawable(new ColorDrawable(mTheme.keyboardBg));
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(mTheme.keyboardBg);

        root.addView(makeTopBar());

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        mPagesContainer = new LinearLayout(this);
        mPagesContainer.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(mPagesContainer);
        root.addView(scroll);

        setContentView(root);

        switchTab(getIntent().getIntExtra("target_tab", sSelectedTab));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int targetTab = intent.getIntExtra("target_tab", sSelectedTab);
        switchTab(targetTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mThemeManager.reload();
        int tid = mThemeManager.getThemeId();
        int acc = mThemeManager.getAccentColor();
        if (mLastThemeId != -1 && (mLastThemeId != tid || mLastAccent != acc)) {
            recreate();
        } else {
            mLastThemeId = tid;
            mLastAccent = acc;
            if (mSetupPage != null) mSetupPage.updateStatus();
        }
    }

    public void setLastThemeId(int id) { this.mLastThemeId = id; }
    public void setLastAccent(int accent) { this.mLastAccent = accent; }

    @SuppressLint("SetTextI18n")
    private View makeTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(mTheme.keyBg);
        bar.setPadding(UIUtils.dp(this, 16), UIUtils.dp(this, 16), UIUtils.dp(this, 16), 0);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        IconView logo = new IconView(this);
        logo.setIcon(KeyIcons.IC_LOGO);
        logo.setColor(mTheme.accent);
        logo.setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(this, 54), UIUtils.dp(this, 54)));
        bar.addView(logo);

        LinearLayout rightSide = new LinearLayout(this);
        rightSide.setOrientation(LinearLayout.VERTICAL);
        rightSide.setPadding(UIUtils.dp(this, 12), 0, 0, 0);
        rightSide.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(this);
        title.setText("Payne-less: Typer");
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(mTheme.keyText);
        rightSide.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Light · Private · Fast · Free");
        sub.setTextSize(11);
        sub.setTextColor(mTheme.isDark ? 0x88FFFFFF : 0x88000000);
        rightSide.addView(sub);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, UIUtils.dp(this, 4), 0, 0);

        mTabSetup     = makeTabLabel("Setup",     sSelectedTab == 0);
        mTabCustomize = makeTabLabel("Customize", sSelectedTab == 1);
        mTabTester    = makeTabLabel("Tester",    sSelectedTab == 2);

        mTabSetup    .setOnClickListener(v -> switchTab(0));
        mTabCustomize.setOnClickListener(v -> switchTab(1));
        mTabTester   .setOnClickListener(v -> switchTab(2));

        tabs.addView(mTabSetup);
        tabs.addView(mTabCustomize);
        tabs.addView(mTabTester);
        rightSide.addView(tabs);

        bar.addView(rightSide);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(mTheme.keyBg);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UIUtils.dp(this, 1)));
        divider.setBackgroundColor(mTheme.isDark ? 0x22FFFFFF : 0x22000000);

        wrapper.addView(bar);
        wrapper.addView(divider);
        return wrapper;
    }

    private TextView makeTabLabel(String text, boolean active) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(active ? mTheme.accent : (mTheme.isDark ? 0x88FFFFFF : 0x88000000));
        tv.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        tv.setPadding(0, UIUtils.dp(this, 8), UIUtils.dp(this, 16), UIUtils.dp(this, 12));
        return tv;
    }

    private void switchTab(int index) {
        sSelectedTab = index;
        if (mTesterPage != null && mTesterPage.getVisibility() == View.VISIBLE && index != 2) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }

        if (index == 0 && mSetupPage == null) {
            mSetupPage = new SetupView(this, mTheme);
            mPagesContainer.addView(mSetupPage);
        }
        if (index == 1 && mCustomizePage == null) {
            mCustomizePage = new CustomizeView(this, mThemeManager, mTheme);
            mPagesContainer.addView(mCustomizePage);
        }
        if (index == 2 && mTesterPage == null) {
            mTesterPage = new TesterView(this, mTheme);
            mPagesContainer.addView(mTesterPage);
        }

        if (mSetupPage != null) mSetupPage.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (mCustomizePage != null) mCustomizePage.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        if (mTesterPage != null) mTesterPage.setVisibility(index == 2 ? View.VISIBLE : View.GONE);

        int active = mTheme.accent;
        int inactive = mTheme.isDark ? 0x88FFFFFF : 0x88000000;

        mTabSetup.setTextColor(index == 0 ? active : inactive);
        mTabCustomize.setTextColor(index == 1 ? active : inactive);
        mTabTester.setTextColor(index == 2 ? active : inactive);

        mTabSetup.setTypeface(null, index == 0 ? Typeface.BOLD : Typeface.NORMAL);
        mTabCustomize.setTypeface(null, index == 1 ? Typeface.BOLD : Typeface.NORMAL);
        mTabTester.setTypeface(null, index == 2 ? Typeface.BOLD : Typeface.NORMAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mSetupPage != null) mSetupPage.updateStatus();
    }
}
