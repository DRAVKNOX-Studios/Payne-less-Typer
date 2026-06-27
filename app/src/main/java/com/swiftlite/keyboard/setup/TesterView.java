package com.swiftlite.keyboard.setup;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import com.swiftlite.keyboard.SetupActivity;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.UIUtils;

import java.io.InputStream;
import java.util.Locale;

public class TesterView extends LinearLayout {

    private final SetupActivity mActivity;
    private final KeyboardTheme mTheme;

    private long mStartTime = 0;
    private int mCharCount = 0;
    private TextView mWpmText;
    private ImageView mPastedImage;

    public TesterView(SetupActivity activity, KeyboardTheme theme) {
        super(activity);
        mActivity = activity;
        mTheme = theme;
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(UIUtils.dp(getContext(), 14), UIUtils.dp(getContext(), 16), 
                   UIUtils.dp(getContext(), 14), UIUtils.dp(getContext(), 24));

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(getContext());
        label.setText("Keyboard Tester");
        label.setTextSize(14);
        label.setTypeface(null, Typeface.BOLD);
        label.setTextColor(mTheme.keyText);
        header.addView(label);

        View spacer = new View(getContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(spacer);

        mWpmText = new TextView(getContext());
        mWpmText.setText("0 WPM");
        mWpmText.setTextColor(mTheme.accent);
        mWpmText.setTypeface(null, Typeface.BOLD);
        header.addView(mWpmText);

        addView(header);
        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 12), 0));

        EditText tester = new EditText(getContext());
        tester.setHint("Try your keyboard...");
        tester.setHintTextColor(mTheme.isDark ? 0x55FFFFFF : 0x55000000);
        tester.setTextColor(mTheme.keyText);
        tester.setTextSize(15);
        tester.setMinLines(5);
        tester.setGravity(Gravity.TOP | Gravity.START);
        tester.setPadding(UIUtils.dp(getContext(), 14), UIUtils.dp(getContext(), 12), 
                          UIUtils.dp(getContext(), 14), UIUtils.dp(getContext(), 12));
        tester.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        tester.setImeOptions(EditorInfo.IME_ACTION_NONE);

        GradientDrawable testerBg = new GradientDrawable();
        testerBg.setColor(mTheme.keyBg);
        testerBg.setCornerRadius(UIUtils.dp(getContext(), 10));
        testerBg.setStroke(UIUtils.dp(getContext(), 1), mTheme.isDark ? 0x22FFFFFF : 0x22000000);
        tester.setBackground(testerBg);

        tester.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mStartTime == 0) mStartTime = SystemClock.elapsedRealtime();
                mCharCount = s.length();
                updateWpm();
            }
            public void afterTextChanged(Editable s) {}
        });

        ViewCompat.setOnReceiveContentListener(tester, new String[]{"image/*"}, (view, contentInfo) -> {
            Uri uri = null;
            ClipData clip = contentInfo.getClip();
            if (clip != null && clip.getItemCount() > 0) {
                for (int i = 0; i < clip.getItemCount(); i++) {
                    if (clip.getItemAt(i).getUri() != null) {
                        uri = clip.getItemAt(i).getUri();
                        break;
                    }
                }
            }
            if (uri != null) {
                final Uri finalUri = uri;
                new Thread(() -> {
                    try (InputStream is = getContext().getContentResolver().openInputStream(finalUri)) {
                        final Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            mPastedImage.post(() -> {
                                mPastedImage.setImageBitmap(bmp);
                                mPastedImage.setVisibility(View.VISIBLE);
                                GradientDrawable gd = new GradientDrawable();
                                gd.setColor(mTheme.keyBg);
                                gd.setCornerRadius(UIUtils.dp(getContext(), 12));
                                gd.setStroke(UIUtils.dp(getContext(), 2), mTheme.accent);
                                mPastedImage.setBackground(gd);
                            });
                        }
                    } catch (Exception ignored) {}
                }).start();
                Toast.makeText(getContext(), "Image attached!", Toast.LENGTH_SHORT).show();
                return null;
            }
            return contentInfo;
        });

        addView(tester);
        addView(UIUtils.vspace(getContext(), UIUtils.dp(getContext(), 12), 0));

        mPastedImage = new ImageView(getContext());
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, UIUtils.dp(getContext(), 180));
        imgLp.topMargin = UIUtils.dp(getContext(), 8);
        imgLp.bottomMargin = UIUtils.dp(getContext(), 12);
        mPastedImage.setLayoutParams(imgLp);
        mPastedImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mPastedImage.setVisibility(View.GONE);
        mPastedImage.setPadding(UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 12), 
                                UIUtils.dp(getContext(), 12), UIUtils.dp(getContext(), 12));
        UIUtils.roundBg(mPastedImage, mTheme.keyBg, UIUtils.dp(getContext(), 12));
        addView(mPastedImage);

        Button clearBtn = makeBtn("Clear", mTheme.specialKey);
        clearBtn.setOnClickListener(v -> {
            tester.setText("");
            mPastedImage.setVisibility(View.GONE);
            mStartTime = 0;
            mCharCount = 0;
            updateWpm();
        });
        addView(clearBtn);
    }

    private void updateWpm() {
        if (mStartTime == 0 || mCharCount == 0) {
            mWpmText.setText("0 WPM");
            return;
        }
        long elapsed = SystemClock.elapsedRealtime() - mStartTime;
        if (elapsed < 1000) return;
        double mins = elapsed / 60000.0;
        int wpm = (int) ((mCharCount / 5.0) / mins);
        mWpmText.setText(String.format(Locale.getDefault(), "%d WPM", wpm));
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
}
