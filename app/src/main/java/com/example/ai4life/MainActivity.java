package com.example.ai4life;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;


public class MainActivity extends AppCompatActivity {
    CardView createImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createImg = findViewById(R.id.createImg);
        createImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CreateImgActivity.class);
                startActivity(intent);
            }
        });
        CardView removeBgCard = findViewById(R.id.card_remove_background);

        removeBgCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RemoveBackgroundActivity.class);
                startActivity(intent);
            }
        });
    }
}
