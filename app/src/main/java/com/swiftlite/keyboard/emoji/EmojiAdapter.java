package com.swiftlite.keyboard.emoji;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.swiftlite.keyboard.theme.KeyboardTheme;

public class EmojiAdapter extends BaseAdapter {
    private final Context mCtx;
    public String[] mEmojis;
    private final EmojiPanel mPanel;
    public String mSkinModifier = null;
    private final int mCellSizePx;
    private KeyboardTheme mTheme;

    public EmojiAdapter(Context ctx, String[] emojis, EmojiPanel panel, KeyboardTheme theme) {
        mCtx = ctx;
        mEmojis = emojis;
        mPanel = panel;
        mTheme = theme;
        mCellSizePx = Math.round(40 * ctx.getResources().getDisplayMetrics().density);
    }

    public void setEmojis(String[] emojis) { mEmojis = emojis; notifyDataSetChanged(); }
    public void setSkinModifier(String mod) { mSkinModifier = mod; notifyDataSetChanged(); }
    public void setTheme(KeyboardTheme theme) { mTheme = theme; notifyDataSetChanged(); }

    @Override public int getCount()              { return mEmojis.length; }
    @Override public Object getItem(int i)       { return mEmojis[i]; }
    @Override public long getItemId(int i)       { return i; }
    @Override public boolean hasStableIds()      { return false; }

    @Override
    public View getView(int pos, View cv, ViewGroup parent) {
        TextView tv;
        if (cv instanceof TextView) {
            tv = (TextView) cv;
        } else {
            tv = new TextView(mCtx);
            tv.setLayoutParams(new ViewGroup.LayoutParams(mCellSizePx, mCellSizePx));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(22);
            tv.setTextIsSelectable(false);
        }
        tv.setTextColor(mTheme != null ? mTheme.keyText : Color.WHITE);

        final String base     = mEmojis[pos];
        final String toCommit = (mSkinModifier != null && EmojiSkinToneHelper.isToneSupportedEmoji(base))
                ? EmojiSkinToneHelper.applyTone(base, mSkinModifier) : base;

        tv.setText(EmojiPanel.processEmoji(toCommit));
        tv.setOnTouchListener((v, ev) -> {
            mPanel.onCellTouch(ev, base, toCommit, v);
            return true;
        });
        return tv;
    }
}
