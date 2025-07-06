package com.example.ai4life;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateAnimeActivity extends AppCompatActivity {

    private ImageView ivBack, ivDownload, ivResult;
    private EditText etPrompt;
    private Button btnSelectImage, btnStart;
    private ProgressBar progressBar;
    private Uri selectedImageUri = null;
    private Bitmap resultBitmap = null;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .build();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String API_KEY = "SG_3c8342b3a826d9da";

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Cần quyền truy cập bộ nhớ để chọn ảnh", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(ivResult);
                    btnStart.setEnabled(true);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_anime);

        // ... (phần ánh xạ view giữ nguyên)
        ivBack = findViewById(R.id.ivBack);
        ivDownload = findViewById(R.id.ivDownload);
        ivResult = findViewById(R.id.ivResult);
        etPrompt = findViewById(R.id.etPrompt);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnStart = findViewById(R.id.btnStart);
        progressBar = findViewById(R.id.progressBar);

        loadInitialGif();

        setupClickListeners();
    }

    private void loadInitialGif() {
        // Thay 'anime_loading' bằng tên file GIF của bạn
        Glide.with(this)
                .asGif()
                .load(R.drawable.anime_generator)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE) // Tối ưu cho việc load GIF từ drawable
                .into(ivResult);
    }

    private void setupClickListeners() {
        // ... (phần setup click listeners giữ nguyên)
        ivBack.setOnClickListener(v -> finish());
        btnSelectImage.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnStart.setOnClickListener(v -> startImageGeneration());
        ivDownload.setOnClickListener(v -> saveResultImage());
    }

    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void startImageGeneration() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Vui lòng chọn ảnh trước", Toast.LENGTH_SHORT).show();
            return;
        }
        String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập prompt", Toast.LENGTH_SHORT).show();
            return;
        }
        generateAnimeImage(prompt);
    }

    private void saveResultImage() {
        if (resultBitmap != null) {
            saveImageToGallery(resultBitmap);
        } else {
            Toast.makeText(this, "Không có ảnh để lưu", Toast.LENGTH_SHORT).show();
        }
    }


    private void generateAnimeImage(String prompt) {
        setLoadingState(true);
        executor.execute(() -> {
            try {
                byte[] imageBytes = getOptimizedImageBytesFromUri(selectedImageUri);
                if (imageBytes == null) {
                    throw new IOException("Không thể xử lý ảnh được chọn.");
                }

                RequestBody imageRequestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "image.jpg", imageRequestBody)
                        .addFormDataPart("prompt", prompt + ", anime style")
                        .addFormDataPart("strength", "0.75")
                        .build();

                Request request = new Request.Builder()
                        .url("https://api.segmind.com/v1/sd1.5-img2img")
                        //.url("https://api.segmind.com/v1/sdxl-img2img") api thay the neu server bận
                        .header("x-api-key", API_KEY)
                        .post(requestBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        throw new IOException("Lỗi từ server: " + response.code() + " - " + errorBody);
                    }

                    InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                    resultBitmap = BitmapFactory.decodeStream(inputStream);

                    handler.post(() -> {
                        Glide.with(this).load(resultBitmap).into(ivResult);
                        setLoadingState(false);
                        ivDownload.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    setLoadingState(false);
                    Toast.makeText(CreateAnimeActivity.this, "Đã xảy ra lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // === HÀM XỬ LÝ ẢNH AN TOÀN NHẤT ĐỂ TRÁNH OUTOFMEMORY ===
    private byte[] getOptimizedImageBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = null;
        try {
            // Bước 1: Đọc kích thước ảnh mà không tải vào bộ nhớ
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close(); // Quan trọng: Đóng stream sau khi đọc bounds

            int originalWidth = options.outWidth;
            int originalHeight = options.outHeight;
            int targetSize = 1024; // Kích thước tối đa cho chiều rộng hoặc cao

            // Bước 2: Tính toán tỉ lệ thu nhỏ (inSampleSize)
            int inSampleSize = 1;
            if (originalHeight > targetSize || originalWidth > targetSize) {
                final int halfHeight = originalHeight / 2;
                final int halfWidth = originalWidth / 2;
                while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2;
                }
            }

            // Bước 3: Tải ảnh vào bộ nhớ với kích thước đã được thu nhỏ
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            inputStream = getContentResolver().openInputStream(uri); // Mở lại stream
            if (inputStream == null) return null;

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) return null;

            // Bước 4: Nén ảnh thành mảng byte
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void setLoadingState(boolean isLoading) {
        // ... (giữ nguyên)
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
        ivBack.setEnabled(!isLoading);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        // ... (giữ nguyên)
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "anime_image_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    Toast.makeText(this, "Đã lưu ảnh vào thư viện!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show();
        }
    }
}