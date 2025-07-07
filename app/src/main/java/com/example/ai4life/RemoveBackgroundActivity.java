package com.example.ai4life;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.example.ai4life.api.RemoveBgService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RemoveBackgroundActivity extends AppCompatActivity {
    private VideoView videoView;
    private Button btnUploadImage;
    private ImageView ivBack;
    private LottieAnimationView progressBar;
    private static final String REMOVE_BG_API_KEY = "aa4JQtmoY4vgx539hWSZQGtR";
    private RemoveBgService removeBgService;
    private ActivityResultLauncher<String> pickImageLauncher;
    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_background);

        videoView = findViewById(R.id.videoView);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBarRemoveBg);
        videoPath = "android.resource://" + getPackageName() + "/" + R.raw.intro_remove_bg;

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.remove.bg/").addConverterFactory(GsonConverterFactory.create()).build();
        removeBgService = retrofit.create(RemoveBgService.class);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uriSelected -> {
            if (uriSelected != null) {
                try {
                    File imageFile = getFileFromUri(uriSelected);
                    if (imageFile != null) {
                        uploadImageAndRemoveBackground(imageFile);
                    } else {
                        showErrorDialog("Lỗi File", "Không thể lấy file ảnh từ thiết bị.");
                    }
                } catch (Exception e) {
                    showErrorDialog("Lỗi Xử Lý Ảnh", e.getMessage());
                }
            }
        });

        btnUploadImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        ivBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videoView != null && videoPath != null) {
            Uri uri = Uri.parse(videoPath);
            videoView.setVideoURI(uri);
            videoView.start();
            videoView.setOnCompletionListener(mp -> videoView.start());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoView != null && videoView.isPlaying()) {
            videoView.stopPlayback();
        }
    }

    private File getFileFromUri(Uri uri) {
        File file = null;
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                file = new File(getCacheDir(), "temp_upload_image.jpg");
                OutputStream outputStream = new FileOutputStream(file);
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private void uploadImageAndRemoveBackground(File imageFile) {
        setLoadingState(true);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image_file", imageFile.getName(), requestFile);

        removeBgService.removeBackground(REMOVE_BG_API_KEY, body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setLoadingState(false);
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        byte[] imageBytes = response.body().bytes();
                        File tempResultFile = new File(getCacheDir(), "removed_bg_image.png");
                        FileOutputStream fos = new FileOutputStream(tempResultFile);
                        fos.write(imageBytes);
                        fos.close();
                        Intent intent = new Intent(RemoveBackgroundActivity.this, ResultRemoveBG.class);
                        intent.setData(Uri.fromFile(tempResultFile));
                        startActivity(intent);
                    } else {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                        showErrorDialog("Lỗi API", "Lỗi: " + response.code() + " - " + errorBody);
                    }
                } catch (Exception e) {
                    showErrorDialog("Lỗi Xử Lý", "Không thể xử lý phản hồi từ server.");
                } finally {
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoadingState(false);
                showErrorDialog("Lỗi Mạng", "Không thể kết nối. Vui lòng kiểm tra lại.");
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnUploadImage.setEnabled(!isLoading);
        ivBack.setEnabled(!isLoading);
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