package com.swiftlite.keyboard.clipboard;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.List;

public class ClipboardRepository {

    private static final int MAX_UNPINNED = 20;

    private static final int HASH_SAMPLE_BYTES = 4096;

    private final ClipboardDao mDao;
    private final Context      mContext;

    public ClipboardRepository(ClipboardDatabase db, Context context) {
        mDao     = db.clipboardDao();
        mContext = context.getApplicationContext();
    }

    public void addItem(String content) {
        if (content == null || content.isEmpty()) return;
        mDao.deleteByContent(content);
        mDao.insert(new ClipboardItem(content, false));
        mDao.pruneUnpinned(MAX_UNPINNED);
    }

    public void addImageItem(String contentUriString, File imageFile) {
        if (contentUriString == null || contentUriString.isEmpty()) return;

        String hash = partialMd5(imageFile);
        if (hash != null) {
            List<ClipboardItem> existing = mDao.getUnpinnedImageItems();
            for (ClipboardItem old : existing) {
                File oldFile = uriToFile(Uri.parse(old.imageUri));
                if (oldFile != null && hash.equals(partialMd5(oldFile))) {
                    mDao.deleteById(old.id);
                    if (oldFile.exists()) oldFile.delete();
                    break;
                }
            }
        }

        mDao.insert(new ClipboardItem(contentUriString, false, true));
        pruneUnpinnedWithImageCleanup();
    }

    private void pruneUnpinnedWithImageCleanup() {
        int count = mDao.getUnpinnedCount();
        if (count <= MAX_UNPINNED) return;
        List<ClipboardItem> unpinnedImages = mDao.getUnpinnedImageItems();
        mDao.pruneUnpinned(MAX_UNPINNED);
        List<ClipboardItem> remaining = mDao.getUnpinnedImageItems();
        for (ClipboardItem old : unpinnedImages) {
            boolean stillExists = false;
            for (ClipboardItem r : remaining) {
                if (r.id == old.id) { stillExists = true; break; }
            }
            if (!stillExists) deleteImageFile(old.imageUri);
        }
    }

    public void removeItemsNotOnClipboard() {
        List<ClipboardItem> all = mDao.getAll();
        for (ClipboardItem item : all) {
            if (!item.pinned && item.imageUri != null) {
                Uri uri = Uri.parse(item.imageUri);
                File file = uriToFile(uri);
                if (file != null && !file.exists()) {
                    mDao.deleteById(item.id);
                }
            }
        }
    }

    public void removeAllUnpinnedText() {
        List<ClipboardItem> all = mDao.getAll();
        for (ClipboardItem item : all) {
            if (!item.pinned && item.imageUri == null) {
                mDao.deleteById(item.id);
            }
        }
    }

    public List<ClipboardItem> getAll() {
        return mDao.getAll();
    }

    public ClipboardItem getLatest() {
        return mDao.getLatestUnpinned();
    }

    public void delete(ClipboardItem item) {
        if (item.imageUri != null) deleteImageFile(item.imageUri);
        mDao.deleteById(item.id);
    }

    public void togglePin(ClipboardItem item) {
        item.pinned = !item.pinned;
        mDao.update(item);
    }

    private void deleteImageFile(String uriString) {
        if (uriString == null) return;
        File file = uriToFile(Uri.parse(uriString));
        if (file != null && file.exists()) file.delete();
    }

    private File uriToFile(Uri uri) {
        if (uri == null) return null;
        try {
            String path = uri.getPath();
            if (path != null) return new File(path);
        } catch (Exception ignored) {}
        return null;
    }

    private static String partialMd5(File file) {
        if (file == null || !file.exists()) return null;
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[HASH_SAMPLE_BYTES];
            int n = fis.read(buf);
            if (n <= 0) return null;
            md.update(buf, 0, n);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return null; }
    }
}
