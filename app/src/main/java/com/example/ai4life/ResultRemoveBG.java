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
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ResultRemoveBG extends AppCompatActivity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private ImageView imageViewResult;
    private ImageView ivBack;
    private Button btnBack, btnSave;
    private Uri imageUri;
    private File tempResultFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_remove_bg);

        imageViewResult = findViewById(R.id.imageViewResult);
        btnBack = findViewById(R.id.btnBack);
        btnSave = findViewById(R.id.btnSave);
        ivBack = findViewById(R.id.ivBackToolbar);
        imageUri = getIntent().getData();

        if (imageUri != null && "file".equals(imageUri.getScheme())) {
            tempResultFile = new File(imageUri.getPath());
        }

        if (imageUri != null) {
            Picasso.get().load(imageUri).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).networkPolicy(NetworkPolicy.NO_CACHE).into(imageViewResult, new Callback() {
                @Override
                public void onSuccess() {
                    btnSave.setEnabled(true);
                }
                @Override
                public void onError(Exception e) {
                    btnSave.setEnabled(false);
                    showErrorDialog("Lỗi Tải Ảnh", "Không thể hiển thị ảnh kết quả.");
                }
            });
        } else {
            btnSave.setEnabled(false);
            showErrorDialog("Không có dữ liệu", "Không nhận được ảnh để hiển thị.");
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSave.setOnClickListener(v -> saveImageToGallery());
        ivBack.setOnClickListener(v -> onBackPressed());
        btnSave.setEnabled(false);
    }

    private void saveImageToGallery() {
        if (imageViewResult.getDrawable() == null) {
            showErrorDialog("Lỗi", "Không có ảnh để lưu.");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            saveImage();
        }
    }

    private void saveImage() {
        Bitmap bitmap = ((BitmapDrawable) imageViewResult.getDrawable()).getBitmap();
        String fileName = "removebg_image_" + System.currentTimeMillis() + ".png";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI4Life");
        }
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try {
            Uri savedImageUri = getContentResolver().insert(collection, values);
            OutputStream os = getContentResolver().openOutputStream(savedImageUri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            if (os != null) os.close();
            showSaveSuccessDialog();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Lỗi Lưu Ảnh", "Không thể lưu ảnh vào thư viện.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                showErrorDialog("Quyền bị từ chối", "Không thể lưu ảnh nếu không cấp quyền.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tempResultFile != null && tempResultFile.exists()) {
            tempResultFile.delete();
        }
    }

    private void showSaveSuccessDialog() {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.SUCCESS);
        dialog.setTitle("Đã lưu!");
        dialog.setMessage("Ảnh của bạn đã được lưu thành công vào thư viện.");
        dialog.setConfirmButton("OK", v -> dialog.dismiss());
        dialog.show();
    }

    private void showErrorDialog(String title, String message) {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.WARNING);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setConfirmButton("Đã hiểu", v -> dialog.dismiss());
        dialog.show();
    }
}