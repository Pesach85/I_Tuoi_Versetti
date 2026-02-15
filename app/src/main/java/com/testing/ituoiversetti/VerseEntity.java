package com.testing.ituoiversetti;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "verses",
        primaryKeys = {"bookKey", "chapter", "verse"},
        indices = {@Index(value = {"bookKey", "chapter", "verse"}, unique = true)}
)
public class VerseEntity {

    @NonNull
    public String bookKey;   // chiave normalizzata del libro (dal PDF)

    public int chapter;
    public int verse;

    @NonNull
    public String text;
}
