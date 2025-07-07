package com.example.ai4life;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;

public class LocalHistoryHelper {

    public interface SaveListener {
        void onSaveComplete();
        void onSaveFailed(Exception e);
    }

    private static String saveImageToInternalStorage(Context context, Bitmap bitmap, String type) throws Exception {
        String fileName = type + "_" + System.currentTimeMillis() + ".jpg";
        File directory = context.getDir("history_images", Context.MODE_PRIVATE);
        File filePath = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
        return filePath.getAbsolutePath();
    }

    public static void saveImageToHistory(Context context, Bitmap bitmap, String prompt, String type, SaveListener listener) {
        new Thread(() -> {
            try {
                String imagePath = saveImageToInternalStorage(context, bitmap, type);
                HistoryImage historyEntry = new HistoryImage(imagePath, prompt, type, System.currentTimeMillis());
                AppDatabase.getDatabase(context).historyDao().insert(historyEntry);
                new Handler(Looper.getMainLooper()).post(listener::onSaveComplete);
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onSaveFailed(e));
            }
        }).start();
    }
}