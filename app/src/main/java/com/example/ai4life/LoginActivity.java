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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;
    private LottieAnimationView progressBar;
    private FirebaseAuth mAuth;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null && currentUser.isEmailVerified()){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        progressBar = findViewById(R.id.progressBarLogin);

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String email, password;
            email = etEmail.getText().toString();
            password = etPassword.getText().toString();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                progressBar.setVisibility(View.GONE);
                showErrorDialog("Lỗi", "Vui lòng nhập đầy đủ email và mật khẩu.");
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                if (user.isEmailVerified()) {
                                    showSuccessLoginDialog();
                                }
                                else {
                                    mAuth.signOut();
                                    showVerifcationDialog(user);
                                }
                            }
                        } else {
                            showErrorDialog("Đăng nhập thất bại", "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.");
                        }
                    });
        });
    }

    private void showSuccessLoginDialog() {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.SUCCESS);
        dialog.setTitle("Thành công!");
        dialog.setMessage("Đăng nhập thành công. Chào mừng bạn quay trở lại.");
        dialog.setConfirmButton("Tiếp tục", v -> {
            dialog.dismiss();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
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

    private void showVerifcationDialog(FirebaseUser user) {
        CustomStatusDialog verificationDialog = new CustomStatusDialog(this);
        verificationDialog.setDialogType(CustomStatusDialog.DialogType.WARNING);
        verificationDialog.setTitle("Chưa xác thực tài khoản");
        verificationDialog.setMessage("Vui lòng kiểm tra email của bạn để xác thực tài khoản.");

        verificationDialog.setConfirmButton("Gửi lại email", v -> {
            verificationDialog.dismiss();

            progressBar.setVisibility(View.VISIBLE);

            user.sendEmailVerification().addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    showInfoDialog("Gửi thành công", "Vui lòng kiểm tra email của bạn để xác thực tài khoản.");
                }
                else {
                    showErrorDialog("Gửi thất bại", "Vui lòng thử lại xong");
                }
            });
        });

        verificationDialog.setCancelButton("OK", v -> {
            verificationDialog.dismiss();
        });
        verificationDialog.show();
    }

    private void showInfoDialog(String title, String message) {
        CustomStatusDialog dialog = new CustomStatusDialog(this);
        dialog.setDialogType(CustomStatusDialog.DialogType.SUCCESS);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setConfirmButton("Ok", v -> dialog.dismiss());
        dialog.show();
    }
}