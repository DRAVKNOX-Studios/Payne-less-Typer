package com.swiftlite.keyboard.clipboard;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ClipboardItem.class}, version = 3, exportSchema = false)
public abstract class ClipboardDatabase extends RoomDatabase {

    public abstract ClipboardDao clipboardDao();

    private static volatile ClipboardDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE clipboard_items ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE clipboard_items ADD COLUMN imageUri TEXT");
        }
    };

    public static ClipboardDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ClipboardDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    ClipboardDatabase.class,
                                    "clipboard_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
