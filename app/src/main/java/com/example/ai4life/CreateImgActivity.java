package com.example.ai4life;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateImgActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private byte[] currentImageData;
    private Button btnFetchImage;
    private Button btnBack;
    private Button btnSave;
    private ImageView imageView;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private EditText etPrompt; // Thêm EditText

    private static final String CLIPDROP_API_KEY = "14397954d114e92139b3028b68d3ecda23dfdcce86fd804706279d2047c1682bb489f0928d4046df586f5b94632561d9";
    private static final String API_URL = "https://clipdrop-api.co/text-to-image/v1";

    private OkHttpClient client;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_img_activity);
        // Ánh xạ các thành phần UI
        btnFetchImage = findViewById(R.id.btnFetchImage);
        btnSave = findViewById(R.id.saveBtn);
        imageView = findViewById(R.id.imageView);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        etPrompt = findViewById(R.id.etPrompt);

        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnSave.setEnabled(false);
        btnFetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchImage();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentImageData != null) {
                    saveImageToGallery(currentImageData);
                } else {
                    Toast.makeText(CreateImgActivity.this, "Không có ảnh để lưu.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void saveImageToGallery(byte[] imageData) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        if (bitmap == null) {
            Toast.makeText(this, "Không thể giải mã ảnh.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Image_" + System.currentTimeMillis() + ".png"; // Tên file duy nhất
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) trở lên
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "YourAppImages"); // Tên thư mục con trong Pictures
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1); // Đánh dấu là đang ghi

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri != null) {
                try (OutputStream fos = resolver.openOutputStream(imageUri)) {
                    if (fos != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        contentValues.clear();
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0); // Bỏ đánh dấu pending
                        resolver.update(imageUri, contentValues, null, null);
                        Toast.makeText(this, "Đã lưu ảnh vào thư viện!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không thể mở luồng ghi ảnh.", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("SaveImage", "Error saving image (Q+): " + e.getMessage());
                }
            } else {
                Toast.makeText(this, "Không thể tạo URI để lưu ảnh.", Toast.LENGTH_SHORT).show();
            }
        } else { // Dưới Android 10 (API 28 trở xuống)
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "YourAppImages";
            File appSpecificAlbum = new File(imagesDir);
            if (!appSpecificAlbum.exists()) {
                appSpecificAlbum.mkdirs();
            }
            File imageFile = new File(appSpecificAlbum, fileName);

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(this, "Đã lưu ảnh vào thư viện!", Toast.LENGTH_SHORT).show();

                // Cần làm mới MediaStore để ảnh hiển thị ngay lập tức
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            } catch (IOException e) {
                Toast.makeText(this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("SaveImage", "Error saving image (<Q): " + e.getMessage());
            }
        }
    }
    private void fetchImage() {
        // Lấy prompt từ EditText
        String prompt = etPrompt.getText().toString().trim();

        // Kiểm tra nếu prompt rỗng
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "Vui lòng nhập mô tả ảnh!", Toast.LENGTH_SHORT).show();
            return; // Dừng lại nếu prompt rỗng
        }

        // Ẩn ảnh cũ và hiển thị ProgressBar
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Đang tạo ảnh với prompt:\n\"" + prompt + "\""); // Hiển thị prompt đang dùng
        btnFetchImage.setEnabled(false); // Tắt nút để tránh click liên tục
        etPrompt.setEnabled(false); // Tắt EditText khi đang xử lý
        btnSave.setEnabled(false);

        // Chạy tác vụ mạng trên luồng nền
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("prompt", prompt) // Dùng prompt từ người dùng
                        .build();

                Request request = new Request.Builder()
                        .url(API_URL)
                        .header("x-api-key", CLIPDROP_API_KEY)
                        .post(requestBody)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                tvStatus.setText("Lỗi: " + e.getMessage());
                                Toast.makeText(CreateImgActivity.this, "Lỗi khi lấy ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                btnFetchImage.setEnabled(true);
                                etPrompt.setEnabled(true); // Bật lại EditText
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            currentImageData = response.body().bytes();

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    tvStatus.setVisibility(View.GONE);

                                    Glide.with(CreateImgActivity.this)
                                            .load(currentImageData)
                                            .into(imageView);

                                    imageView.setVisibility(View.VISIBLE);
                                    Toast.makeText(CreateImgActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
                                    btnFetchImage.setEnabled(true);
                                    etPrompt.setEnabled(true); // Bật lại EditText
                                    btnSave.setEnabled(true);
                                }
                            });
                        } else {
                            final String errorMessage = response.body() != null ? response.body().string() : "Lỗi không xác định";
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                    tvStatus.setText("Lỗi API: " + response.code() + " - " + errorMessage);
                                    Toast.makeText(CreateImgActivity.this, "Lỗi API: " + response.code() + "\n" + errorMessage, Toast.LENGTH_LONG).show();
                                    btnFetchImage.setEnabled(true);
                                    etPrompt.setEnabled(true); // Bật lại EditText
                                }
                            });
                        }
                        if (response.body() != null) {
                            response.body().close();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
