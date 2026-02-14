package com.testing.ituoiversetti;

import androidx.room.Entity;
import androidx.room.Index;

@Entity(
        tableName = "verses",
        primaryKeys = {"bookKey", "chapter", "verse"},
        indices = {@Index(value = {"bookKey", "chapter", "verse"}, unique = true)}
)
public class VerseEntity {
    public String bookKey;   // chiave normalizzata del libro (dal PDF)
    public int chapter;
    public int verse;
    public String text;
}

