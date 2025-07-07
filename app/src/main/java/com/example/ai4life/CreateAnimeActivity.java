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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private LottieAnimationView progressBar;
    private Uri selectedImageUri = null;
    private Bitmap resultBitmap = null;

    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(300, TimeUnit.SECONDS).writeTimeout(300, TimeUnit.SECONDS).readTimeout(300, TimeUnit.SECONDS).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String API_KEY = "SG_3c8342b3a826d9da";

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            openGallery();
        } else {
            showErrorDialog("Quyền bị từ chối", "Cần quyền truy cập bộ nhớ để chọn ảnh.");
        }
    });

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            selectedImageUri = result.getData().getData();
            Glide.with(this).load(selectedImageUri).into(ivResult);
            btnStart.setEnabled(true);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_anime);

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
        Glide.with(this).asGif().load(R.drawable.anime_generator).diskCacheStrategy(DiskCacheStrategy.RESOURCE).into(ivResult);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnSelectImage.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnStart.setOnClickListener(v -> startImageGeneration());
        ivDownload.setOnClickListener(v -> saveResultImage());
    }

    private void checkPermissionAndOpenGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
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
            showErrorDialog("Thiếu ảnh", "Vui lòng chọn ảnh gốc trước.");
            return;
        }
        String prompt = etPrompt.getText().toString().trim();
        if (prompt.isEmpty()) {
            showErrorDialog("Thiếu mô tả", "Vui lòng nhập mô tả cho ảnh.");
            return;
        }
        generateAnimeImage(prompt);
    }

    private void saveResultImage() {
        if (resultBitmap != null) {
            saveImageToGallery(resultBitmap);
        } else {
            showErrorDialog("Lỗi", "Không có ảnh để lưu.");
        }
    }

    private void generateAnimeImage(String prompt) {
        setLoadingState(true);
        executor.execute(() -> {
            try {
                byte[] imageBytes = getOptimizedImageBytesFromUri(selectedImageUri);
                if (imageBytes == null) throw new IOException("Không thể xử lý ảnh được chọn.");
                RequestBody imageRequestBody = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", "image.jpg", imageRequestBody).addFormDataPart("prompt", prompt + ", anime style").addFormDataPart("strength", "0.75").build();
                Request request = new Request.Builder().url("https://api.segmind.com/v1/sd1.5-img2img").header("x-api-key", API_KEY).post(requestBody).build();

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
                    showErrorDialog("Đã có lỗi", "Không thể tạo ảnh. Vui lòng thử lại sau.");
                });
            }
        });
    }

    private byte[] getOptimizedImageBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            int targetSize = 1024;
            int inSampleSize = 1;
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= targetSize && (halfWidth / inSampleSize) >= targetSize) {
                    inSampleSize *= 2;
                }
            }
            options.inSampleSize = inSampleSize;
            options.inJustDecodeBounds = false;
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (bitmap == null) return null;
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
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnStart.setEnabled(!isLoading);
        btnSelectImage.setEnabled(!isLoading);
        ivBack.setEnabled(!isLoading);
    }

    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "anime_image_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    showSaveSuccessDialog();
                }
            } catch (Exception e) {
                e.printStackTrace();
                showErrorDialog("Lưu thất bại", "Không thể lưu ảnh vào thư viện.");
            }
        } else {
            showErrorDialog("Lưu thất bại", "Không thể tạo file ảnh.");
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