package com.example.banga;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        Button btnStartGame = findViewById(R.id.btnStartGame);
        Button btnQuit = findViewById(R.id.btnQuit);
        Button btnHighScores = findViewById(R.id.btnHighScores);

        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Khởi động GameActivity
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });

        // Add High Scores button listener
        if (btnHighScores != null) {
            btnHighScores.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, HighScoreActivity.class);
                    startActivity(intent);
                }
            });
        }

        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Thoát ứng dụng
                finish();
            }
        });
    }
}
