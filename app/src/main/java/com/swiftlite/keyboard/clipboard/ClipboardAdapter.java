package com.swiftlite.keyboard.clipboard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.swiftlite.keyboard.ime.KeyIcons;
import com.swiftlite.keyboard.theme.KeyboardTheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ClipboardAdapter extends RecyclerView.Adapter<ClipboardAdapter.VH> {

    public interface OnItemClick { void onClick(ClipboardItem item); }

    private final OnItemClick mOnCommit;
    private final OnItemClick mOnPin;
    private final OnItemClick mOnDelete;
    private List<ClipboardItem> mItems = new ArrayList<>();
    private KeyboardTheme mTheme;

    public ClipboardAdapter(KeyboardTheme theme,
                            OnItemClick onCommit,
                            OnItemClick onPin,
                            OnItemClick onDelete) {
        mTheme   = theme;
        mOnCommit = onCommit;
        mOnPin    = onPin;
        mOnDelete = onDelete;
    }

    public void setItems(List<ClipboardItem> items) {
        mItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setTheme(KeyboardTheme theme) {
        mTheme = theme;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        int rowH = viewType == 1 ? dp(parent, 56) : dp(parent, 44);
        row.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, rowH));
        row.setPadding(dp(parent, 8), dp(parent, 2), dp(parent, 4), dp(parent, 2));
        row.setGravity(Gravity.CENTER_VERTICAL);
        return new VH(row, viewType == 1);
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).isImage() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(mItems.get(position), mTheme, mOnCommit, mOnPin, mOnDelete);
    }

    @Override public int getItemCount() { return mItems.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final boolean isImageType;
        TextView tvContent;
        ImageView ivThumb;
        IconBtn pinBtn, delBtn;
        View pinBar;

        VH(LinearLayout row, boolean isImageType) {
            super(row);
            this.row = row;
            this.isImageType = isImageType;
            build();
        }

        private void build() {
            pinBar = new View(row.getContext());
            LinearLayout.LayoutParams barLp =
                    new LinearLayout.LayoutParams(dp(row, 3), ViewGroup.LayoutParams.MATCH_PARENT);
            barLp.setMarginEnd(dp(row, 6));
            pinBar.setLayoutParams(barLp);
            row.addView(pinBar);

            if (isImageType) {
                int thumbSz = dp(row, 40);
                ivThumb = new ImageView(row.getContext());
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(thumbSz, thumbSz);
                tlp.setMarginEnd(dp(row, 8));
                ivThumb.setLayoutParams(tlp);
                ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                row.addView(ivThumb);

                tvContent = new TextView(row.getContext());
                tvContent.setLayoutParams(new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tvContent.setMaxLines(1);
                tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tvContent.setTextSize(11);
                row.addView(tvContent);
            } else {
                tvContent = new TextView(row.getContext());
                tvContent.setLayoutParams(new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tvContent.setMaxLines(1);
                tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tvContent.setTextSize(12);
                row.addView(tvContent);
            }

            int sz = dp(row, 36);
            pinBtn = new IconBtn(row.getContext(), KeyIcons.IC_PIN, sz);
            delBtn = new IconBtn(row.getContext(), KeyIcons.IC_CROSS_RED, sz);
            row.addView(pinBtn);
            row.addView(delBtn);
        }

        void bind(ClipboardItem item, KeyboardTheme theme,
                  OnItemClick onCommit, OnItemClick onPin, OnItemClick onDelete) {
            int textColor   = theme.keyText;
            int mutedColor  = darken(theme.keyText, 0.55f);
            int accentColor = theme.accent;

            if (item.isImage()) {
                tvContent.setText("Image");
                tvContent.setTextColor(mutedColor);
                loadThumbnail(item.imageUri);
            } else {
                tvContent.setText(item.content);
                tvContent.setTextColor(textColor);
                if (ivThumb != null) ivThumb.setImageDrawable(null);
            }

            if (item.pinned) {
                pinBtn.setColor(accentColor);
                pinBar.setBackgroundColor(accentColor);
            } else {
                pinBtn.setColor(mutedColor);
                pinBar.setBackgroundColor(Color.TRANSPARENT);
            }

            row.setOnClickListener(v -> onCommit.onClick(item));
            pinBtn.setOnClickListener(v -> onPin.onClick(item));
            delBtn.setOnClickListener(v -> onDelete.onClick(item));
        }

        private void loadThumbnail(String uriString) {
            if (ivThumb == null || uriString == null) return;
            ivThumb.setImageDrawable(null);
            int targetSz = dp(row, 40);
            new Thread(() -> {
                try {
                    Uri uri = Uri.parse(uriString);
                    Bitmap bmp;
                    try (InputStream is = row.getContext().getContentResolver()
                            .openInputStream(uri)) {
                        if (is == null) return;
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 4;
                        bmp = BitmapFactory.decodeStream(is, null, opts);
                    }
                    if (bmp == null) return;
                    Bitmap scaled = Bitmap.createScaledBitmap(bmp, targetSz, targetSz, true);
                    bmp.recycle();
                    float radius = dp(row, 6) * row.getResources().getDisplayMetrics().density;
                    RoundedBitmapDrawable rd =
                            RoundedBitmapDrawableFactory.create(row.getResources(), scaled);
                    rd.setCornerRadius(radius);
                    rd.setAntiAlias(true);
                    ivThumb.post(() -> ivThumb.setImageDrawable(rd));
                } catch (Exception ignored) {}
            }).start();
        }

        private static int darken(int color, float factor) {
            return Color.rgb(
                (int)(Color.red(color)   * factor),
                (int)(Color.green(color) * factor),
                (int)(Color.blue(color)  * factor));
        }

        private static int dp(View v, int dp) {
            return Math.round(dp * v.getContext().getResources().getDisplayMetrics().density);
        }
    }

    static class IconBtn extends View {
        private final int mIcon;
        private int mColor;
        private final float mDensity;
        private final int mSz;

        IconBtn(android.content.Context ctx, int icon, int sizePx) {
            super(ctx);
            mIcon    = icon;
            mDensity = ctx.getResources().getDisplayMetrics().density;
            mSz      = sizePx;
            mColor   = Color.GRAY;
            setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.TRANSPARENT);
            gd.setCornerRadius(mDensity * 6);
            setBackground(gd);
        }

        void setColor(int color) { mColor = color; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            KeyIcons.draw(canvas, mIcon, mSz / 2f, mSz / 2f, 16, mDensity, mColor);
        }
    }

    private static int dp(View v, int dp) {
        return Math.round(dp * v.getContext().getResources().getDisplayMetrics().density);
    }
}
