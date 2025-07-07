package com.example.ai4life;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_table")
public class HistoryImage {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "image_path")
    public String imagePath;

    @ColumnInfo(name = "prompt")
    public String prompt;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public HistoryImage() { }

    public HistoryImage(String imagePath, String prompt, String type, long timestamp) {
        this.imagePath = imagePath;
        this.prompt = prompt;
        this.type = type;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}