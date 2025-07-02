package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    CardView cardCreateImage;
    CardView cardRemoveBackground;
    private ViewPager2 newsViewPager;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsItemList;
    private Handler handler;
    private Runnable sliderRunable;
    private Timer swipeTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler(Looper.getMainLooper());

        newsViewPager = findViewById(R.id.newsViewPager);
        newsItemList = new ArrayList<>();
        newsItemList.add(new NewsItem("OpenAI ra mắt Sora", "https://picsum.photos/800/400?random=1"));
        newsItemList.add(new NewsItem("Google giới thiệu Gemini", "https://picsum.photos/800/400?random=2"));
        newsItemList.add(new NewsItem("NVIDIA công bố chip AI mới", "https://picsum.photos/800/400?random=3"));
        newsItemList.add(new NewsItem("OpenAI phát triển mô hình AGI", "https://picsum.photos/800/400?random=4"));
        newsItemList.add(new NewsItem("DeepSeek vừa được ra mắt!", "https://picsum.photos/800/400?random=5"));


        newsAdapter = new NewsAdapter(newsItemList);
        newsViewPager.setAdapter(newsAdapter);

        setupAutoSlide();

        cardCreateImage = findViewById(R.id.cardCreateImage);
        cardCreateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateImgActivity.class);
                startActivity(intent);
            }
        });
        cardRemoveBackground = findViewById(R.id.cardRemoveBackground);
        cardRemoveBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoveBackgroundActivity.class);
                startActivity(intent);
            }
        });
    }

    public void setupAutoSlide() {
        sliderRunable = () -> {
            int currentItem = newsViewPager.getCurrentItem();
            if (currentItem == newsAdapter.getItemCount() - 1) {
                newsViewPager.setCurrentItem(0);
            }
            else {
                newsViewPager.setCurrentItem(currentItem + 1);
            }
        };
        swipeTimer = new Timer();
        swipeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(sliderRunable);
            }
        }, 3000, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (swipeTimer != null) {
            swipeTimer.cancel();
        }
    }
}
