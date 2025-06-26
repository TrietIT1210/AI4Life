package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


public class MainActivity extends AppCompatActivity {
    CardView cardCreateImage;
    CardView cardRemoveBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardCreateImage = findViewById(R.id.cardCreateImage);
        cardCreateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateImgActivity.class);
                startActivity(intent);
            }
        });
        cardRemoveBackground = findViewById(R.id.cardRemoveBackground);
        cardRemoveBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoveBackgroundActivity.class);
                startActivity(intent);
            }
        });
    }
}
