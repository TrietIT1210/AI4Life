package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountFragment extends Fragment {

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private TextView tvUserEmail;
    private CardView cardUpgrade;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        btnLogout = view.findViewById(R.id.btnLogout);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        cardUpgrade = view.findViewById(R.id.cardUpgrade);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            tvUserEmail.setText(currentUser.getEmail());
        }

        cardUpgrade.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ComingSoonActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showLogoutConfirmationDialog() {
        CustomStatusDialog dialog = new CustomStatusDialog(requireContext());
        dialog.setDialogType(CustomStatusDialog.DialogType.DELETE);
        dialog.setTitle("Đăng xuất");
        dialog.setMessage("Bạn có chắc chắn muốn đăng xuất?");
        dialog.setConfirmButton("Xác nhận", v -> {
            dialog.dismiss();
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
        dialog.setCancelButton("Hủy", v -> dialog.dismiss());
        dialog.show();
    }
}