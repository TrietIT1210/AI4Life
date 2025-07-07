package com.example.ai4life;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.File;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private Context context;
    private List<HistoryImage> historyImageList;

    public HistoryAdapter(Context context, List<HistoryImage> historyImageList) {
        this.context = context;
        this.historyImageList = historyImageList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryImage item = historyImageList.get(position);
        holder.tvPrompt.setText(item.getPrompt());

        File imgFile = new File(item.getImagePath());
        if(imgFile.exists()){
            Glide.with(context)
                    .load(Uri.fromFile(imgFile))
                    .placeholder(R.drawable.shape_image_container)
                    .into(holder.ivImage);
        }
    }

    @Override
    public int getItemCount() {
        return historyImageList.size();
    }

    public void setData(List<HistoryImage> newList) {
        this.historyImageList = newList;
        notifyDataSetChanged();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvPrompt;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivHistoryImage);
            tvPrompt = itemView.findViewById(R.id.tvHistoryPrompt);
        }
    }
}