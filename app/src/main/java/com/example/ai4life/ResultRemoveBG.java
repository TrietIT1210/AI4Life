package com.example.ai4life;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy; // Import này
import com.squareup.picasso.NetworkPolicy; // Import này

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ResultRemoveBG extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private ImageView imageViewResult;
    private Button btnBack;
    private Button btnSave;
    private Uri imageUri;
    private File tempResultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_remove_bg);

        imageViewResult = findViewById(R.id.imageViewResult);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);

        imageUri = getIntent().getData();

        if (imageUri != null && "file".equals(imageUri.getScheme())) {
            tempResultFile = new File(imageUri.getPath());
        }

        if (imageUri != null) {
            Picasso.get()
                    .load(imageUri)
                    // THÊM CÁC DÒNG NÀY ĐỂ VÔ HIỆU HÓA CACHE CỦA PICASSO
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .into(imageViewResult, new Callback() {
                        @Override
                        public void onSuccess() {
                            btnSave.setEnabled(true);
                        }

                        @Override
                        public void onError(Exception e) {
                            imageViewResult.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                            Toast.makeText(ResultRemoveBG.this, "Lỗi khi tải ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            btnSave.setEnabled(false);
                        }
                    });
        } else {
            imageViewResult.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            Toast.makeText(this, "Không có ảnh để hiển thị", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(false);
        }

        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

        btnSave.setOnClickListener(v -> {
            saveImageToGallery();
        });

        btnSave.setEnabled(false);
    }

    private void saveImageToGallery() {
        if (imageViewResult.getDrawable() == null) {
            Toast.makeText(this, "Không có ảnh để lưu.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageQAndAbove();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                saveImageLegacy();
            }
        }
    }

    private void saveImageQAndAbove() {
        Bitmap bitmap = ((BitmapDrawable) imageViewResult.getDrawable()).getBitmap();
        String fileName = "removebg_image_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/RemoveBg");

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri savedImageUri = getContentResolver().insert(collection, values);

        if (savedImageUri != null) {
            try (OutputStream os = getContentResolver().openOutputStream(savedImageUri)) {
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Toast.makeText(this, "Ảnh đã được lưu vào thư viện!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Không thể mở luồng ghi ảnh.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("ResultRemoveBG", "Lỗi lưu ảnh Q+: ", e);
            }
        } else {
            Toast.makeText(this, "Không thể tạo URI ảnh.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageLegacy() {
        Bitmap bitmap = ((BitmapDrawable) imageViewResult.getDrawable()).getBitmap();
        File imagesDir = new File(getExternalFilesDir(null), "RemoveBgImages");
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        String fileName = "removebg_image_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(imagesDir, fileName);

        try (OutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Toast.makeText(this, "Ảnh đã được lưu vào thư viện!", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("ResultRemoveBG", "Lỗi lưu ảnh Legacy: ", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageLegacy();
            } else {
                Toast.makeText(this, "Quyền ghi vào bộ nhớ ngoài bị từ chối. Không thể lưu ảnh.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tempResultFile != null && tempResultFile.exists()) {
            boolean deleted = tempResultFile.delete();
            if (!deleted) {
                Log.w("ResultRemoveBG", "Không thể xóa file kết quả tạm thời: " + tempResultFile.getAbsolutePath());
            } else {
                Log.d("ResultRemoveBG", "Đã xóa file kết quả tạm thời: " + tempResultFile.getAbsolutePath());
            }
        }
    }
}