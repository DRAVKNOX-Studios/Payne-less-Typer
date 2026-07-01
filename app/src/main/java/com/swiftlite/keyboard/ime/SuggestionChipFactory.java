package com.swiftlite.keyboard.ime;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swiftlite.keyboard.clipboard.ClipboardItem;
import com.swiftlite.keyboard.theme.KeyboardTheme;
import com.swiftlite.keyboard.utils.UIUtils;

import java.io.InputStream;

public class SuggestionChipFactory {

    public interface ChipCallback {
        void onChipClicked(ClipboardItem item);
    }

    public static View createClipboardChip(Context context, ClipboardItem item, KeyboardTheme theme, SwiftLiteIME ime, Handler handler, ChipCallback callback) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(UIUtils.dp(context, 12), 0, UIUtils.dp(context, 12), 0);
        
        chip.setOnClickListener(v -> {
            if (callback != null) callback.onChipClicked(item);
        });

        if (item.isImage()) {
            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new LinearLayout.LayoutParams(UIUtils.dp(context, 28), UIUtils.dp(context, 28)));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(0, 0, UIUtils.dp(context, 4), 0);
            GradientDrawable gd = new GradientDrawable();
            gd.setCornerRadius(UIUtils.dp(context, 4));
            iv.setClipToOutline(true);
            iv.setBackground(gd);

            ime.getExecutor().execute(() -> {
                try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(item.imageUri))) {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inSampleSize = 2;
                    Bitmap bmp = BitmapFactory.decodeStream(is, null, o);
                    if (bmp != null) handler.post(() -> iv.setImageBitmap(bmp));
                } catch (Exception ignored) {}
            });
            chip.addView(iv);

            TextView tv = new TextView(context);
            tv.setText("Image");
            tv.setTextSize(12);
            if (theme != null) tv.setTextColor(theme.keyText);
            chip.addView(tv);
        } else if (item.content != null) {
            TextView tv = new TextView(context);
            String t = item.content.replace("\n", " ");
            tv.setText(t.length() > 30 ? t.substring(0, 27) + "..." : t);
            tv.setTextSize(12);
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            if (theme != null) tv.setTextColor(theme.keyText);
            chip.addView(tv);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1f;
        chip.setLayoutParams(lp);
        
        return chip;
    }
}
