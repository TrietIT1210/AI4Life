package com.example.ai4life;

public class NewsItem {

    private String title;
    private String imageUrl;
    private String articleUrl;

    public NewsItem(String title, String imageUrl, String articleUrl) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.articleUrl = articleUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public String getArticleUrl() {
        return articleUrl;
    }
    public void setArticleUrl(String articleUrl) {
        this.articleUrl = articleUrl;
    }
}