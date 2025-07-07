package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvSignIn;
    private LottieAnimationView progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);
        progressBar = findViewById(R.id.progressBarRegister);

        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email, password, confirmPassword;
            email = etEmail.getText().toString();
            password = etPassword.getText().toString();
            confirmPassword = etConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                progressBar.setVisibility(View.GONE);
                showErrorDialog("Lỗi", "Vui lòng điền đầy đủ thông tin.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                progressBar.setVisibility(View.GONE);
                showErrorDialog("Lỗi", "Mật khẩu xác nhận không khớp.");
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                    mAuth.signOut();
                                    showRegistrationSuccessDialog();
                                });
                            }
                        } else {
                            showErrorDialog("Đăng ký thất bại", task.getException().getMessage());
                        }
                    });
        });
    }

    private void showRegistrationSuccessDialog() {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.SUCCESS);
        dialog.setTitle("Hoàn tất!");
        dialog.setMessage("Tài khoản của bạn đã được tạo. Vui lòng kiểm tra email để xác thực nhé.");
        dialog.setConfirmButton("Về Đăng nhập", v -> {
            dialog.dismiss();
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });
        dialog.setCancelable(false);
        dialog.show();
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