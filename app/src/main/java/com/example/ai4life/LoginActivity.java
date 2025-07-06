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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvSignUp;
    private FirebaseAuth mAuth;
    private LoadingDialog loadingDialog;

    @Override
    protected void onStart() {
        super.onStart();
        //Check dang nhap chua, neu roi thi khoi dang nhap nua, chua thi dang nhap lai
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loadingDialog = new LoadingDialog(this);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> {
            loadingDialog.startLoadingDialog();
            String email, password;
            email = etEmail.getText().toString().trim();
            password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ email và mật khẩu.", Toast.LENGTH_SHORT).show();
                loadingDialog.dismissDialog();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        loadingDialog.dismissDialog();
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // Trường hợp thành công: Email đã xác thực
                                Toast.makeText(getApplicationContext(), "Đăng nhập thành công.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // Trường hợp thất bại: Email CHƯA xác thực
                                // Đăng xuất người dùng ra khỏi trạng thái đăng nhập tạm thời
                                mAuth.signOut();
                                // Hiển thị hộp thoại cho phép gửi lại email
                                showVerificationDialog(user);
                            }
                        } else {
                            // Trường hợp thất bại: Sai email/mật khẩu
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại. Vui lòng kiểm tra lại email hoặc mật khẩu.", Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void showVerificationDialog(FirebaseUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác thực email");
        builder.setMessage("Tài khoản của bạn chưa được xác thực. Vui lòng kiểm tra hòm thư hoặc yêu cầu" +
                "gửi lại link");

        builder.setPositiveButton("Gửi lại email", ((dialog, which) -> {
            if (user != null) {
                user.sendEmailVerification().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đã gửi lại email. Vui lòng kiểm tra", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(LoginActivity.this, "Gửi lại email thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }));

        builder.setNegativeButton("Để sau", ((dialog, which) -> {
            dialog.dismiss();
        }));

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
