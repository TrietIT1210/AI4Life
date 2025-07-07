package com.example.ai4life;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateImgActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private byte[] currentImageData;
    private Button btnFetchImage;
    private ImageView ivBack;
    private Button btnSave;
    private ImageView imageView;
    private LottieAnimationView progressBar;
    private EditText etPrompt;
    private static final String CLIPDROP_API_KEY = "14397954d114e92139b3028b68d3ecda23dfdcce86fd804706279d2047c1682bb489f0928d4046df586f5b94632561d9";
    private static final String API_URL = "https://clipdrop-api.co/text-to-image/v1";
    private OkHttpClient client;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ViewPager2 exampleImagesViewPager;
    private NewsAdapter exampleImagesAdapter;
    private List<NewsItem> exampleImageList;
    private Timer slideTimer;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_img_activity);
        ivBack = findViewById(R.id.ivBack);
        btnFetchImage = findViewById(R.id.btnFetchImg);
        btnSave = findViewById(R.id.btnSaveImg);
        imageView = findViewById(R.id.ivPicture);
        progressBar = findViewById(R.id.pbCreateImg);
        etPrompt = findViewById(R.id.etPromptCreateImg);
        exampleImagesViewPager = findViewById(R.id.exampleImagesViewPager);
        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        btnSave.setEnabled(false);
        exampleImageList = new ArrayList<>();
        exampleImageList.add(new NewsItem("Một con mèo phi hành gia trên sao hỏa", "https://picsum.photos/800/400?random=4"));
        exampleImagesAdapter = new NewsAdapter(exampleImageList);
        exampleImagesViewPager.setAdapter(exampleImagesAdapter);
        startAutoSlider();

        ivBack.setOnClickListener(v -> finish());

        btnFetchImage.setOnClickListener(v -> fetchImage());

        loadingDialog = new LoadingDialog(this);

        btnSave.setOnClickListener(v -> {
            if (currentImageData != null) {
                saveImageToGallery();

                loadingDialog.startLoadingDialog("Đang lưu vào lịch sử...");
                Bitmap bitmap = BitmapFactory.decodeByteArray(currentImageData, 0, currentImageData.length);
                String prompt = etPrompt.getText().toString().trim();

                LocalHistoryHelper.saveImageToHistory(this, bitmap, prompt, "text-to-image", new LocalHistoryHelper.SaveListener() {
                    @Override
                    public void onSaveComplete() {
                        loadingDialog.dismissDialog();
                        showInfoDialog("Thành công", "Đã lưu ảnh vào lịch sử của bạn.");
                    }

                    @Override
                    public void onSaveFailed(Exception e) {
                        loadingDialog.dismissDialog();
                        showErrorDialog("Lỗi", "Không thể lưu vào lịch sử: " + e.getMessage());
                    }
                });
            } else {
                showErrorDialog("Lỗi", "Không có ảnh để lưu.");
            }
        });
    }

    private void saveImageToGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageQAndAbove();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                saveImageLegacy();
            }
        }
    }

    private void saveImageQAndAbove() {
        Bitmap bitmap = BitmapFactory.decodeByteArray(currentImageData, 0, currentImageData.length);
        if (bitmap == null) {
            showErrorDialog("Lỗi", "Không thể giải mã hình ảnh.");
            return;
        }
        String fileName = "Image_" + System.currentTimeMillis() + ".png";
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "AI4Life");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (imageUri != null) {
            try (OutputStream fos = resolver.openOutputStream(imageUri)) {
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    resolver.update(imageUri, values, null, null);
                    showSaveSuccessDialog();
                }
            } catch (IOException e) {
                showErrorDialog("Lỗi Lưu Ảnh", e.getMessage());
            }
        }
    }

    private void saveImageLegacy() {
        Bitmap bitmap = BitmapFactory.decodeByteArray(currentImageData, 0, currentImageData.length);
        if (bitmap == null) {
            showErrorDialog("Lỗi", "Không thể giải mã hình ảnh.");
            return;
        }
        String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "AI4Life";
        File appDir = new File(imagesDir);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        String fileName = "Image_" + System.currentTimeMillis() + ".png";
        File imageFile = new File(appDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            showSaveSuccessDialog();
        } catch (IOException e) {
            showErrorDialog("Lỗi Lưu Ảnh", e.getMessage());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageLegacy();
            } else {
                showErrorDialog("Quyền bị từ chối", "Không thể lưu ảnh nếu không cấp quyền ghi vào bộ nhớ.");
            }
        }
    }

    private void fetchImage() {
        String prompt = etPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            showErrorDialog("Thiếu thông tin", "Vui lòng nhập mô tả cho ảnh.");
            return;
        }
        if (slideTimer != null) {
            slideTimer.cancel();
            slideTimer = null;
        }
        exampleImagesViewPager.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        btnFetchImage.setEnabled(false);
        etPrompt.setEnabled(false);
        btnSave.setEnabled(false);
        executorService.execute(() -> {
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("prompt", prompt).build();
            Request request = new Request.Builder().url(API_URL).header("x-api-key", CLIPDROP_API_KEY).post(requestBody).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        showErrorDialog("Lỗi Mạng", "Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại kết nối.");
                        btnFetchImage.setEnabled(true);
                        etPrompt.setEnabled(true);
                        exampleImagesViewPager.setVisibility(View.VISIBLE);
                        startAutoSlider();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        currentImageData = response.body().bytes();
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            Glide.with(CreateImgActivity.this).load(currentImageData).into(imageView);
                            imageView.setVisibility(View.VISIBLE);
                            btnFetchImage.setEnabled(true);
                            etPrompt.setEnabled(true);
                            btnSave.setEnabled(true);
                        });
                    } else {
                        final String errorMessage = response.body() != null ? response.body().string() : "Lỗi không xác định";
                        mainHandler.post(() -> {
                            progressBar.setVisibility(View.GONE);
                            showErrorDialog("Lỗi API", "Lỗi: " + response.code() + ". " + errorMessage);
                            btnFetchImage.setEnabled(true);
                            etPrompt.setEnabled(true);
                            exampleImagesViewPager.setVisibility(View.VISIBLE);
                            startAutoSlider();
                        });
                    }
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            });
        });
    }

    private void startAutoSlider() {
        if (slideTimer != null) {
            slideTimer.cancel();
        }
        Runnable sliderRunnable = () -> {
            if (exampleImagesAdapter.getItemCount() > 0) {
                int currentItem = exampleImagesViewPager.getCurrentItem();
                if (currentItem == exampleImagesAdapter.getItemCount() - 1) {
                    exampleImagesViewPager.setCurrentItem(0);
                } else {
                    exampleImagesViewPager.setCurrentItem(currentItem + 1);
                }
            }
        };
        slideTimer = new Timer();
        slideTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainHandler.post(sliderRunnable);
            }
        }, 3000, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slideTimer != null) {
            slideTimer.cancel();
        }
        if (executorService != null) {
            executorService.shutdownNow();
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
    private void showInfoDialog(String title, String message) {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.SUCCESS);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setConfirmButton("OK", v -> dialog.dismiss());
        dialog.show();
    }
}