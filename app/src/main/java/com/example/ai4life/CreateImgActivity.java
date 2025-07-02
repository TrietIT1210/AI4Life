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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
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

    private ViewPager2 exampleImagesViewPager;
    private NewsAdapter exampleImagesAdapter;
    private List<NewsItem> exampleImageList;
    private Timer slideTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_img_activity);
        // Ánh xạ các thành phần UI
        btnFetchImage = findViewById(R.id.btnFetchImg);
        btnSave = findViewById(R.id.btnSaveImg);
        imageView = findViewById(R.id.ivPicture);
        progressBar = findViewById(R.id.pbCreateImg);
        tvStatus = findViewById(R.id.tvStatusCreateImg);
        etPrompt = findViewById(R.id.etPromptCreateImg);
        exampleImagesViewPager = findViewById(R.id.exampleImagesViewPager);

        client = new OkHttpClient();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        btnSave.setEnabled(false);

        exampleImageList = new ArrayList<>();
        exampleImageList.add(new NewsItem("Một con mèo phi hành gia trên sao hỏa", "https://picsum.photos/800/400?random=4"));
        exampleImageList.add(new NewsItem("Rừng cây phát sáng vào ban đêm", "https://picsum.photos/800/400?random=5"));
        exampleImageList.add(new NewsItem("Thành phố tương lai dưới nước", "https://picsum.photos/800/400?random=6"));
        exampleImageList.add(new NewsItem("Robot tự học vẽ tranh như Picasso", "https://picsum.photos/800/400?random=7"));
        exampleImageList.add(new NewsItem("Trạm không gian quay quanh sao Thổ", "https://picsum.photos/800/400?random=8"));
        exampleImageList.add(new NewsItem("AI phát hiện sinh vật biển chưa từng thấy", "https://picsum.photos/800/400?random=9"));
        exampleImageList.add(new NewsItem("Người máy nấu ăn 5 món trong 10 phút", "https://picsum.photos/800/400?random=10"));
        exampleImageList.add(new NewsItem("Ngôi làng nổi trên mây", "https://picsum.photos/800/400?random=11"));
        exampleImageList.add(new NewsItem("Cửa hàng ảo dùng kính AR đầu tiên", "https://picsum.photos/800/400?random=12"));
        exampleImageList.add(new NewsItem("Khám phá khu rừng số hóa bởi drone", "https://picsum.photos/800/400?random=13"));
        exampleImageList.add(new NewsItem("Ô tô bay đầu tiên được thử nghiệm", "https://picsum.photos/800/400?random=14"));
        exampleImageList.add(new NewsItem("Trí tuệ nhân tạo chơi đàn cổ Việt Nam", "https://picsum.photos/800/400?random=15"));
        exampleImageList.add(new NewsItem("Kính thực tế ảo thay thế màn hình máy tính", "https://picsum.photos/800/400?random=16"));


        exampleImagesAdapter = new NewsAdapter(exampleImageList);
        exampleImagesViewPager.setAdapter(exampleImagesAdapter);

        // Bắt đầu tự động trượt
        startAutoSlider();

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
        String prompt = etPrompt.getText().toString().trim();
        if (TextUtils.isEmpty(prompt)) {
            Toast.makeText(this, "Vui lòng nhập mô tả ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- SỬA ĐỔI: Quản lý giao diện ---
        // Dừng và ẩn slideshow
        if (slideTimer != null) {
            slideTimer.cancel();
            slideTimer = null;
        }
        exampleImagesViewPager.setVisibility(View.GONE);

        // Ẩn ảnh cũ (nếu có) và hiển thị ProgressBar
        imageView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Đang tạo ảnh với prompt:\n\"" + prompt + "\"");

        // Vô hiệu hóa các nút
        btnFetchImage.setEnabled(false);
        etPrompt.setEnabled(false);
        btnSave.setEnabled(false);

        executorService.execute(() -> {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("prompt", prompt)
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
                    mainHandler.post(() -> {
                        // --- SỬA ĐỔI: Xử lý lỗi ---
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("Lỗi: " + e.getMessage());
                        Toast.makeText(CreateImgActivity.this, "Lỗi khi lấy ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnFetchImage.setEnabled(true);
                        etPrompt.setEnabled(true);
                        // Khi lỗi, có thể hiện lại slideshow
                        exampleImagesViewPager.setVisibility(View.VISIBLE);
                        startAutoSlider();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        currentImageData = response.body().bytes();
                        mainHandler.post(() -> {
                            // --- SỬA ĐỔI: Xử lý thành công ---
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setVisibility(View.GONE);

                            Glide.with(CreateImgActivity.this)
                                    .load(currentImageData)
                                    .into(imageView);

                            imageView.setVisibility(View.VISIBLE);
                            Toast.makeText(CreateImgActivity.this, "Tải ảnh thành công!", Toast.LENGTH_SHORT).show();
                            btnFetchImage.setEnabled(true);
                            etPrompt.setEnabled(true);
                            btnSave.setEnabled(true);
                        });
                    } else {
                        final String errorMessage = response.body() != null ? response.body().string() : "Lỗi không xác định";
                        mainHandler.post(() -> {
                            // --- SỬA ĐỔI: Xử lý lỗi API ---
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("Lỗi API: " + response.code() + " - " + errorMessage);
                            Toast.makeText(CreateImgActivity.this, "Lỗi API: " + response.code() + "\n" + errorMessage, Toast.LENGTH_LONG).show();
                            btnFetchImage.setEnabled(true);
                            etPrompt.setEnabled(true);
                            // Khi lỗi, có thể hiện lại slideshow
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
        // Hủy timer cũ nếu có để tránh chạy nhiều timer cùng lúc
        if (slideTimer != null) {
            slideTimer.cancel();
        }

        Runnable sliderRunnable = () -> {
            int currentItem = exampleImagesViewPager.getCurrentItem();
            if (exampleImagesAdapter.getItemCount() > 0) {
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
        }, 3000, 3000); // Bắt đầu sau 3s, lặp lại mỗi 3s
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (slideTimer != null) {
            slideTimer.cancel();
        }
    }
}
