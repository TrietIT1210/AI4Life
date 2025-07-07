package com.example.ai4life;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class CustomStatusDialog extends Dialog {

    public enum DialogType {
        SUCCESS,
        WARNING,
        DELETE
    }

    private DialogType dialogType;
    private String title, message, confirmText, cancelText;
    private View.OnClickListener confirmClickListener, cancelClickListener;

    private ImageView dialogIcon;
    private TextView dialogTitle, dialogMessage;
    private Button btnConfirm, btnCancel;


    public CustomStatusDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_custom_status);

        // Làm cho nền của Dialog trong suốt để thấy được bo góc của CardView
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialogIcon = findViewById(R.id.dialog_icon);
        dialogTitle = findViewById(R.id.dialog_title);
        dialogMessage = findViewById(R.id.dialog_message);
        btnConfirm = findViewById(R.id.dialog_btn_confirm);
        btnCancel = findViewById(R.id.dialog_btn_cancel);

        configureDialog();
    }

    private void configureDialog() {
        switch (dialogType) {
            case SUCCESS:
                dialogIcon.setImageResource(R.drawable.ic_success);
                btnConfirm.setBackgroundResource(R.drawable.dialog_button_success);
                btnCancel.setVisibility(View.GONE);
                break;
            case WARNING:
                dialogIcon.setImageResource(R.drawable.ic_warning);
                btnConfirm.setBackgroundResource(R.drawable.dialog_button_warning);
                btnCancel.setVisibility(View.VISIBLE);
                break;
            case DELETE:
                dialogIcon.setImageResource(R.drawable.ic_delete);
                btnConfirm.setBackgroundResource(R.drawable.dialog_button_delete);
                btnCancel.setVisibility(View.VISIBLE);
                break;
        }

        dialogTitle.setText(title);
        dialogMessage.setText(message);

        if (confirmText != null) btnConfirm.setText(confirmText);
        if (cancelText != null) btnCancel.setText(cancelText);

        if (confirmClickListener != null) {
            btnConfirm.setOnClickListener(confirmClickListener);
        } else {
            // Mặc định là đóng dialog
            btnConfirm.setOnClickListener(v -> dismiss());
        }

        if (cancelClickListener != null) {
            btnCancel.setOnClickListener(cancelClickListener);
        } else {
            btnCancel.setOnClickListener(v -> dismiss());
        }
    }

    // Các phương thức public để cấu hình dialog từ bên ngoài
    public void setDialogType(DialogType dialogType) {
        this.dialogType = dialogType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setConfirmButton(String text, View.OnClickListener listener) {
        this.confirmText = text;
        this.confirmClickListener = listener;
    }

    public void setCancelButton(String text, View.OnClickListener listener) {
        this.cancelText = text;
        this.cancelClickListener = listener;
    }
}