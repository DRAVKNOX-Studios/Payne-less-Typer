package com.swiftlite.keyboard.clipboard;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ClipboardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ClipboardItem item);

    @Query("SELECT * FROM clipboard_items ORDER BY pinned DESC, timestamp DESC")
    List<ClipboardItem> getAll();

    @Query("SELECT * FROM clipboard_items WHERE pinned = 0 ORDER BY timestamp DESC LIMIT 1")
    ClipboardItem getLatestUnpinned();

    @Query("DELETE FROM clipboard_items WHERE pinned = 0 AND id NOT IN (SELECT id FROM clipboard_items WHERE pinned = 0 ORDER BY timestamp DESC LIMIT :keep)")
    void pruneUnpinned(int keep);

    @Query("SELECT * FROM clipboard_items WHERE pinned = 0 AND imageUri IS NOT NULL")
    List<ClipboardItem> getUnpinnedImageItems();

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM clipboard_items WHERE pinned = 0 AND imageUri IS NULL AND content = :content")
    void deleteByContent(String content);

    @Query("SELECT COUNT(*) FROM clipboard_items WHERE pinned = 0")
    int getUnpinnedCount();

    @Update
    void update(ClipboardItem item);
}
