package com.swiftlite.keyboard.ime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;

import androidx.core.content.FileProvider;

import com.swiftlite.keyboard.clipboard.ClipboardRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ClipboardMonitor {
    public static final String FILE_PROVIDER_AUTHORITY = "com.swiftlite.keyboard.fileprovider";
    public static final String CLIP_IMG_DIR = "clipboard_images";

    private static final int SCREENSHOT_RETRY_DELAY_MS = 600;
    private static final int SCREENSHOT_MAX_RETRIES    = 4;
    private static final int MAX_SAVE_PX = 1280;
    private static final int JPEG_QUALITY = 72;

    private final Context          mContext;
    private final ExecutorService  mExecutor;
    private final Handler          mMainHandler;
    private final ClipboardManager mClipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener mClipListener;
    private android.database.ContentObserver             mScreenshotObserver;
    private String mLastSelfCopiedUri = null;
    private long   mClipboardCaptureSequence = 0;

    private final ClipboardRepository mClipboardRepo;
    private KeyboardView mKeyboardView;

    public ClipboardMonitor(Context context, ClipboardRepository repo, ExecutorService executor) {
        mContext          = context;
        mClipboardRepo    = repo;
        mExecutor         = executor;
        mMainHandler      = new Handler(Looper.getMainLooper());
        mClipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        File clipDir = new File(mContext.getFilesDir(), CLIP_IMG_DIR);
        if (!clipDir.exists() && !clipDir.mkdirs()) {
            Log.w("SwiftLite", "Could not create clip directory");
        }
    }

    public void setKeyboardView(KeyboardView view) {
        mKeyboardView = view;
    }

    public void setup() {
        if (mClipboardManager == null || mClipListener != null) return;
        evictStaleFileProviderClip();
        mScreenshotObserver = new android.database.ContentObserver(mMainHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                final long captureSequence = ++mClipboardCaptureSequence;
                Uri target = (uri != null && uri.toString().contains("images/media"))
                        ? uri
                        : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                mExecutor.execute(() -> retryReadScreenshot(target, captureSequence, 0));
            }
        };
        try {
            mContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mScreenshotObserver);
        } catch (Exception ignored) {}

        mClipListener = () -> {
            if (mContext instanceof SwiftLiteIME) {
                EditorInfo info = ((SwiftLiteIME) mContext).getCurrentInputEditorInfo();
                if (PrivacyHandler.isSensitiveField(info)) return;
            }
            final long captureSequence = ++mClipboardCaptureSequence;
            ClipData clip = mClipboardManager.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) {
                mExecutor.execute(() -> { if (mClipboardRepo != null) mClipboardRepo.removeAllUnpinnedText(); notifyView(); });
                return;
            }
            ClipData.Item item = clip.getItemAt(0);
            CharSequence text = item.getText();
            if (text != null && TextUtils.isEmpty(text.toString().trim())) {
                mExecutor.execute(() -> { if (mClipboardRepo != null) mClipboardRepo.removeAllUnpinnedText(); notifyView(); });
                return;
            }
            Uri uri = item.getUri();
            if (uri != null) {
                if (FILE_PROVIDER_AUTHORITY.equals(uri.getAuthority())) { evictStaleFileProviderClip(); return; }
                if (uri.toString().equals(mLastSelfCopiedUri)) return;
                mExecutor.execute(() -> { byte[] bytes = readUriBytes(uri); if (bytes != null) saveImageClipFromBytes(bytes, captureSequence); });
            } else if (!TextUtils.isEmpty(text)) {
                processTextFallback(text.toString());
            }
        };
        mClipboardManager.addPrimaryClipChangedListener(mClipListener);
    }

    private void retryReadScreenshot(Uri baseUri, long captureSequence, int attempt) {
        if (captureSequence != mClipboardCaptureSequence) return;
        Uri toRead = baseUri;
        if (Objects.equals(baseUri, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) {
            String[] proj = {MediaStore.Images.Media._ID};
            try (Cursor cursor = mContext.getContentResolver().query(baseUri, proj, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
                if (cursor != null && cursor.moveToFirst()) toRead = Uri.withAppendedPath(baseUri, "" + cursor.getLong(0));
            } catch (Exception ignored) {}
        }
        byte[] bytes = readUriBytes(toRead);
        if (bytes != null) { saveImageClipFromBytes(bytes, captureSequence); return; }
        if (attempt < SCREENSHOT_MAX_RETRIES) {
            long delay = SCREENSHOT_RETRY_DELAY_MS * (long) Math.pow(2, attempt);
            mMainHandler.postDelayed(() -> mExecutor.execute(() -> retryReadScreenshot(baseUri, captureSequence, attempt + 1)), delay);
        }
    }

    private void saveImageClipFromBytes(byte[] bytes, long captureSequence) {
        try {
            Bitmap src = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (src == null) return;
            Bitmap bmp = scaledDown(src, MAX_SAVE_PX);
            if (bmp != src) src.recycle();
            File dir  = new File(mContext.getFilesDir(), CLIP_IMG_DIR);
            if (!dir.exists() && !dir.mkdirs()) { bmp.recycle(); return; }
            File file = new File(dir, System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos); }
            bmp.recycle();
            Uri contentUri = FileProvider.getUriForFile(mContext, FILE_PROVIDER_AUTHORITY, file);
            if (mClipboardRepo == null) return;
            if (captureSequence != mClipboardCaptureSequence) { file.delete(); return; }
            final String uriString = contentUri.toString();
            mExecutor.execute(() -> {
                mClipboardRepo.addImageItem(uriString, file);
                mMainHandler.post(() -> {
                    if (captureSequence != mClipboardCaptureSequence) return;
                    mClipboardManager.setPrimaryClip(ClipData.newUri(mContext.getContentResolver(), "image", contentUri));
                    notifyView();
                });
            });
        } catch (Exception ignored) {}
    }

    private static Bitmap scaledDown(Bitmap src, int maxPx) {
        if (maxPx <= 0) return src;
        int w = src.getWidth(), h = src.getHeight();
        int longEdge = Math.max(w, h);
        if (longEdge <= maxPx) return src;
        float scale = (float) maxPx / longEdge;
        return Bitmap.createScaledBitmap(src, Math.round(w * scale), Math.round(h * scale), true);
    }

    private void evictStaleFileProviderClip() {
        if (mClipboardManager == null) return;
        try {
            ClipData clip = mClipboardManager.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0) return;
            ClipData.Item item = clip.getItemAt(0);
            Uri uri = item.getUri();
            if (uri != null && FILE_PROVIDER_AUTHORITY.equals(uri.getAuthority())) {
                mClipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
            }
        } catch (Exception e) { Log.w("SwiftLite", "Failed to evict stale clipboard entry", e); }
    }

    private byte[] readUriBytes(Uri uri) {
        try (InputStream is = mContext.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int n;
            while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
            return buf.toByteArray();
        } catch (Exception e) { return null; }
    }

    private void processTextFallback(String text) {
        if (mClipboardRepo == null) return;
        mExecutor.execute(() -> { mClipboardRepo.addItem(text); mMainHandler.post(this::notifyView); });
    }

    private void notifyView() { if (mKeyboardView != null) { mKeyboardView.setShowingIdleItems(true); mKeyboardView.notifyClipboardChanged(); } }
    public void setLastSelfCopiedUri(String uri) { mLastSelfCopiedUri = uri; }
    public void destroy() {
        if (mScreenshotObserver != null) try { mContext.getContentResolver().unregisterContentObserver(mScreenshotObserver); } catch (Exception ignored) {}
        if (mClipboardManager != null && mClipListener != null) { mClipboardManager.removePrimaryClipChangedListener(mClipListener); mClipListener = null; }
    }
}
