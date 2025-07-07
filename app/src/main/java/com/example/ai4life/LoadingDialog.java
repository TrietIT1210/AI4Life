package com.example.ai4life;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.TextView;

public class LoadingDialog {

    private Activity activity;
    private Dialog dialog;
    public LoadingDialog(Activity myActivity) {
        activity = myActivity;
    }

    void startLoadingDialog(String message) {
        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_loading);

        TextView tvLoadingMessage = dialog.findViewById(R.id.tvLoadingMessage);
        if (message != null && !message.isEmpty()) {
            tvLoadingMessage.setText(message);
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setCancelable(false);
        dialog.show();
    }

    void startLoadingDialog() {
        startLoadingDialog("Đang tải...");
    }

    void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
