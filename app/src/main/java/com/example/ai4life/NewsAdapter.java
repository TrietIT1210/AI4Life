package com.example.ai4life;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsItems;

    public NewsAdapter(List<NewsItem> newsItems) {
        this.newsItems = newsItems;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Hãy chắc chắn rằng bạn có file layout res/layout/item_news.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem currentItem = newsItems.get(position);
        holder.bind(currentItem); // Gọi hàm bind để gán dữ liệu và xử lý click
    }

    @Override
    public int getItemCount() {
        return newsItems.size();
    }


    // ViewHolder đã được nâng cấp
    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView newsImageView;
        TextView newsTitleTextView;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            // Hãy chắc chắn ID của các View trong item_news.xml là đúng
            newsImageView = itemView.findViewById(R.id.newsImageView);
            newsTitleTextView = itemView.findViewById(R.id.newsTitleTextView);
        }

        // Hàm bind để xử lý mọi thứ cho một item
        public void bind(final NewsItem newsItem) {
            // Gán dữ liệu
            newsTitleTextView.setText(newsItem.getTitle());
            Glide.with(itemView.getContext())
                    .load(newsItem.getImageUrl())
                    .centerCrop()
                    .placeholder(R.color.home_page_bar_gray) // Màu nền tạm thời khi ảnh đang tải
                    .into(newsImageView);

            // Đặt sự kiện click cho toàn bộ item
            itemView.setOnClickListener(v -> {
                String url = newsItem.getArticleUrl();
                // Chỉ mở khi URL không rỗng
                if (url != null && !url.isEmpty()) {
                    Context context = itemView.getContext();

                    try {
                        // Tạo và khởi chạy Chrome Custom Tab
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        CustomTabsIntent customTabsIntent = builder.build();
                        customTabsIntent.launchUrl(context, Uri.parse(url));
                    } catch (Exception e) {
                        // Xử lý lỗi nếu không thể mở URL
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}