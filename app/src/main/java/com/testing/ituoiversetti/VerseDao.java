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

    @Query("SELECT bookKey, chapter, verse, text FROM verses ORDER BY bookKey, chapter, verse LIMIT :limit OFFSET :offset")
    List<VerseEntity> pageAll(int limit, int offset);

        @Query("SELECT bookKey, chapter, verse, text FROM verses " +
            "WHERE text LIKE '%' || :term || '%' " +
            "ORDER BY bookKey, chapter, verse LIMIT :limit")
        List<TopicVerseRow> searchByTopicTerm(String term, int limit);

    // ---- TOOL: ispezione DB ----
    @Query("SELECT bookKey AS bookKey, COUNT(*) AS cnt FROM verses GROUP BY bookKey ORDER BY bookKey")
    List<BookCount> listBooks();

    @Query("SELECT bookKey AS bookKey, chapter AS chapter, verse AS verse, substr(text,1,120) AS text " +
           "FROM verses ORDER BY bookKey, chapter, verse LIMIT :limit")
    List<VerseDump> dump(int limit);

}



