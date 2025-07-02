package com.example.ai4life;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private Button btnUploadImage, btnBackToHomePage;

    private static final String REMOVE_BG_API_KEY = "kHh86zN3EBpYgWjcHRXErS5E";

    private RemoveBgService removeBgService;
    private ActivityResultLauncher<String> pickImageLauncher;

    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_remove_background);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        videoView = findViewById(R.id.videoView);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnBackToHomePage = findViewById(R.id.btnBackToHomePage);

        videoPath = "android.resource://" + getPackageName() + "/" + R.raw.intro_remove_bg;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.remove.bg/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        removeBgService = retrofit.create(RemoveBgService.class);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uriSelected -> {
                    if (uriSelected != null) {
                        try {
                            File imageFile = getFileFromUri(uriSelected);
                            if (imageFile != null) {
                                uploadImageAndRemoveBackground(imageFile);
                            } else {
                                Toast.makeText(this, "Không thể lấy file ảnh từ thiết bị.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("MainActivity", "Lỗi khi xử lý URI ảnh: ", e);
                            Toast.makeText(this, "Lỗi khi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        btnUploadImage.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });

        btnBackToHomePage.setOnClickListener(v -> {
            Intent intent = new Intent(RemoveBackgroundActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videoView != null && videoPath != null) {
            Uri uri = Uri.parse(videoPath);
            videoView.setVideoURI(uri);

            // XÓA HOẶC COMMENT CÁC DÒNG NÀY ĐỂ LOẠI BỎ THANH ĐIỀU KHIỂN VIDEO
            // MediaController mediaController = new MediaController(this);
            // videoView.setMediaController(mediaController);
            // mediaController.setAnchorView(videoView);

            videoView.start();
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    videoView.start();
                }
            });
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
            Log.e("MainActivity", "Lỗi khi chuyển đổi URI thành File: ", e);
        }
        return file;
    }

    private void uploadImageAndRemoveBackground(File imageFile) {
        Toast.makeText(this, "Đang tải ảnh lên và xóa nền, vui lòng chờ...", Toast.LENGTH_LONG).show();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image_file", imageFile.getName(), requestFile);

        removeBgService.removeBackground(REMOVE_BG_API_KEY, body)
                .enqueue(new retrofit2.Callback<ResponseBody>() { // Sử dụng retrofit2.Callback
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
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

                                Toast.makeText(RemoveBackgroundActivity.this, "Xóa nền thành công!", Toast.LENGTH_SHORT).show();

                            } else {
                                String errorBody = "Không rõ lỗi";
                                try {
                                    if (response.errorBody() != null) {
                                        errorBody = response.errorBody().string();
                                    }
                                } catch (Exception e) {
                                    Log.e("MainActivity", "Lỗi khi đọc thân lỗi API", e);
                                }
                                Log.e("MainActivity", "Lỗi API: " + response.code() + " - " + errorBody);
                                Toast.makeText(RemoveBackgroundActivity.this, "Lỗi API: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e("MainActivity", "Lỗi khi xử lý phản hồi API: " + e.getMessage(), e);
                            Toast.makeText(RemoveBackgroundActivity.this, "Lỗi khi hiển thị kết quả: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        } finally {
                            if (imageFile != null && imageFile.exists()) {
                                boolean deleted = imageFile.delete();
                                if (!deleted) {
                                    Log.w("MainActivity", "Không thể xóa file ảnh tạm thời: " + imageFile.getAbsolutePath());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("MainActivity", "Lỗi mạng hoặc kết nối: ", t);
                        Toast.makeText(RemoveBackgroundActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        if (imageFile != null && imageFile.exists()) {
                            boolean deleted = imageFile.delete();
                            if (!deleted) {
                                Log.w("MainActivity", "Không thể xóa file ảnh tạm thời: " + imageFile.getAbsolutePath());
                            }
                        }
                    }
                });
    }
}