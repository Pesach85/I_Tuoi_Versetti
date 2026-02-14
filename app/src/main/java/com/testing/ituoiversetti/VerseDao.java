package com.testing.ituoiversetti;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface VerseDao {

    @Query("SELECT verse, text FROM verses " +
           "WHERE bookKey IN (:bookKeys) AND chapter=:chapter AND verse BETWEEN :fromV AND :toV " +
           "ORDER BY verse")
    List<VerseRow> getRange(List<String> bookKeys, int chapter, int fromV, int toV);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<VerseEntity> rows);

    @Query("SELECT COUNT(*) FROM verses")
    long countAll();

    @Query("DELETE FROM verses")
    void clearAll();
}



