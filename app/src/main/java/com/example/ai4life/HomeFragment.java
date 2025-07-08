package com.example.ai4life;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HomeFragment extends Fragment {

    private CardView cardCreateImage, cardRemoveBackground, cardCreateAnime, cardCreateMeme;
    private ViewPager2 newsViewPager;
    private NewsAdapter newsAdapter;
    private List<NewsItem> newsItemList;
    private Handler handler;
    private Runnable sliderRunnable;
    private Timer swipeTimer;
    private TextView tvUserEmail;
    private FirebaseAuth mAuth;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        tvUserEmail = view.findViewById(R.id.tvUserEmail);

        handler = new Handler(Looper.getMainLooper());
        newsViewPager = view.findViewById(R.id.newsViewPager);

        newsItemList = new ArrayList<>();
        newsItemList.add(new NewsItem("OpenAI ra mắt Sora", "https://picsum.photos/800/400?random=1"));
        newsItemList.add(new NewsItem("Google giới thiệu Gemini", "https://picsum.photos/800/400?random=2"));
        newsItemList.add(new NewsItem("NVIDIA công bố chip AI mới", "https://picsum.photos/800/400?random=3"));
        newsItemList.add(new NewsItem("OpenAI phát triển mô hình AGI", "https://picsum.photos/800/400?random=4"));
        newsItemList.add(new NewsItem("DeepSeek vừa được ra mắt!", "https://picsum.photos/800/400?random=5"));

        newsAdapter = new NewsAdapter(newsItemList);
        newsViewPager.setAdapter(newsAdapter);
        setupAutoSlide();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        cardCreateImage = view.findViewById(R.id.cardCreateImage);
        cardCreateImage.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateImgActivity.class);
            startActivity(intent);
        });

        cardRemoveBackground = view.findViewById(R.id.cardRemoveBackground);
        cardRemoveBackground.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RemoveBackgroundActivity.class);
            startActivity(intent);
        });

        cardCreateAnime = view.findViewById(R.id.cardCreateAnime);
        cardCreateAnime.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateAnimeActivity.class);
            startActivity(intent);
        });

        cardCreateMeme = view.findViewById(R.id.cardCreateMeme);
        cardCreateMeme.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ComingSoonActivity.class);
            startActivity(intent);
        });
    }

    public void setupAutoSlide() {
        sliderRunnable = () -> {
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
                if (handler != null) {
                    handler.post(sliderRunnable);
                }
            }
        }, 2000, 2000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (swipeTimer != null) {
            swipeTimer.cancel();
            swipeTimer = null;
        }
        handler = null;
    }
}