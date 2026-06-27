package com.swiftlite.keyboard.clipboard;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "clipboard_items")
public class ClipboardItem {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @Nullable
    public String content;

    @Nullable
    public String imageUri;

    public boolean pinned;
    public long timestamp;

    public ClipboardItem() {}

    public ClipboardItem(String content, boolean pinned) {
        this.content   = content;
        this.imageUri  = null;
        this.pinned    = pinned;
        this.timestamp = System.currentTimeMillis();
    }

    public ClipboardItem(String imageUri, boolean pinned, boolean isImage) {
        this.content   = null;
        this.imageUri  = imageUri;
        this.pinned    = pinned;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isImage() { return imageUri != null && !imageUri.isEmpty(); }
}
