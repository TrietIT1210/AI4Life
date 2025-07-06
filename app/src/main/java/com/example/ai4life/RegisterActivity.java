package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvSignIn;
    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        loadingDialog = new LoadingDialog(this);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);

        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            loadingDialog.startLoadingDialog();
            String email, password, confirmPassword;
            email = etEmail.getText().toString().trim();
            password = etPassword.getText().toString().trim();
            confirmPassword = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterActivity.this, "Enter email", Toast.LENGTH_SHORT).show();
                loadingDialog.dismissDialog();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterActivity.this, "Enter password", Toast.LENGTH_SHORT).show();
                loadingDialog.dismissDialog();
                return;
            }
            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Password not match", Toast.LENGTH_SHORT).show();
                loadingDialog.dismissDialog();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        loadingDialog.dismissDialog();
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user !=  null) {
                                user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                                    if (emailTask.isSuccessful()) {
                                        Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng kiểm tra email để xác thực.", Toast.LENGTH_LONG).show();
                                    }
                                    else {
                                        Toast.makeText(RegisterActivity.this, "Gửi email xác thực thất bại.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }

                            // Đăng xuất người dùng ra ngay sau khi đăng ký để buộc họ phải đăng nhập lại
                            mAuth.signOut();

                            // Chuyển về trang đăng nhập
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }
                        else {
                            // Hiển thị lỗi rõ ràng hơn
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

    }
}
