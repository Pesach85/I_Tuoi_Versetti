package com.testing.ituoiversetti;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {VerseEntity.class}, version = 1, exportSchema = false)
public abstract class BibleDb extends RoomDatabase {

    private static volatile BibleDb INSTANCE;

    public abstract VerseDao verseDao();

    public static BibleDb get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (BibleDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(ctx.getApplicationContext(), BibleDb.class, "bible.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

