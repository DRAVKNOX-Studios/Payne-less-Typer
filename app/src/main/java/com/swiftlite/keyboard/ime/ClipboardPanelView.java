package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.swiftlite.keyboard.clipboard.ClipboardAdapter;
import com.swiftlite.keyboard.clipboard.ClipboardItem;
import com.swiftlite.keyboard.clipboard.ClipboardRepository;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.VibrationUtils;

import java.util.List;

public class ClipboardPanelView extends LinearLayout {

    private final SwiftLiteIME mIME;
    private TextView mClipboardTitle;
    private ClipboardAdapter mClipboardAdapter;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public ClipboardPanelView(Context context, SwiftLiteIME ime, KeyboardView parent,
                              int heightPx, KeyboardTheme theme) {
        super(context);
        mIME = ime;
        init(heightPx, theme);
    }

    private void init(int heightPx, KeyboardTheme theme) {
        setOrientation(VERTICAL);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, heightPx));
        setBackgroundColor(theme.keyboardBg);

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(4));
        header.setGravity(Gravity.CENTER_VERTICAL);

        mClipboardTitle = new TextView(getContext());
        mClipboardTitle.setText("Clipboard");
        mClipboardTitle.setTextSize(13);
        mClipboardTitle.setTypeface(null, Typeface.BOLD);
        mClipboardTitle.setTextColor(theme.keyText);
        mClipboardTitle.setLayoutParams(new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1));
        header.addView(mClipboardTitle);
        addView(header);

        RecyclerView rv = new RecyclerView(getContext());
        rv.setLayoutParams(new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1));
        rv.setLayoutManager(new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, false));

        mClipboardAdapter = new ClipboardAdapter(
                theme,
                item -> {
                    vibrate(VibrationUtils.VIBE_UTIL);
                    if (item.isImage()) mIME.commitClipboardImage(item.imageUri);
                    else               mIME.commitClipboard(item.content);
                },
                item -> {
                    vibrate(VibrationUtils.VIBE_UTIL);
                    mIME.getExecutor().execute(() -> {
                        ClipboardRepository repo = mIME.getClipboardRepository();
                        if (repo != null) repo.togglePin(item);
                    });
                    mHandler.postDelayed(this::refreshClipboard, 200);
                },
                item -> {
                    vibrate(VibrationUtils.VIBE_UTIL);
                    mIME.getExecutor().execute(() -> {
                        ClipboardRepository repo = mIME.getClipboardRepository();
                        if (repo != null) repo.delete(item);
                    });
                    mHandler.postDelayed(this::refreshClipboard, 200);
                }
        );
        rv.setAdapter(mClipboardAdapter);
        addView(rv);
    }

    private void vibrate(int duration) {
        if (mIME != null && mIME.getThemeManager().isVibrateEnabled()) {
            VibrationUtils.vibrate(getContext(), duration);
        }
    }

    public void setTheme(KeyboardTheme theme) {
        setBackgroundColor(theme.keyboardBg);
        if (mClipboardTitle != null) mClipboardTitle.setTextColor(theme.keyText);
        if (mClipboardAdapter != null) mClipboardAdapter.setTheme(theme);
    }

    public void updateHeight(int heightPx) {
        if (getLayoutParams() != null) {
            getLayoutParams().height = heightPx;
            requestLayout();
        }
    }

    public void refreshClipboard() {
        ClipboardRepository repo = mIME.getClipboardRepository();
        if (repo == null) return;
        mIME.getExecutor().execute(() -> {
            List<ClipboardItem> items = repo.getAll();
            mHandler.post(() -> mClipboardAdapter.setItems(items));
        });
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getContext().getResources().getDisplayMetrics().density);
    }
}
