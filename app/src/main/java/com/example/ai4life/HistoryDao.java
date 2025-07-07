package com.example.ai4life;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    void insert(HistoryImage historyImage);

    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    List<HistoryImage> getAllHistory();
}