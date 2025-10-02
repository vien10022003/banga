package com.example.banga;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import android.content.Intent;



public class HighScoreActivity extends Activity {
    
    private HighScoreManager highScoreManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen setup
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_high_score);
        
        highScoreManager = new HighScoreManager(this);
        
        setupUI();
        displayHighScores();
    }
    
    private void setupUI() {
        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Quay về MainActivity và clear tất cả activity khác
                    Intent intent = new Intent(HighScoreActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }
    
    private void displayHighScores() {
        LinearLayout scoresContainer = findViewById(R.id.scoresContainer);
        List<Integer> scores = highScoreManager.getHighScores();
        
        if (scoresContainer == null) return;
        
        scoresContainer.removeAllViews();
        
        if (scores.isEmpty()) {
            TextView noScoresText = new TextView(this);
            noScoresText.setText("No high scores yet!");
            noScoresText.setTextColor(0xFFCCCCCC);
            noScoresText.setTextSize(24);
            noScoresText.setGravity(android.view.Gravity.CENTER);
            noScoresText.setPadding(0, 50, 0, 50);
            scoresContainer.addView(noScoresText);
        } else {
            for (int i = 0; i < scores.size(); i++) {
                TextView scoreText = new TextView(this);
                scoreText.setText(String.format("%d. %d points", i + 1, scores.get(i)));
                scoreText.setTextColor(i == 0 ? 0xFFFFD700 : 0xFFFFFFFF); // Gold for #1
                scoreText.setTextSize(28);
                scoreText.setPadding(0, 15, 0, 15);
                scoreText.setGravity(android.view.Gravity.CENTER);
                
                // Add special styling for top 3
                if (i < 3) {
                    scoreText.setTextSize(32);
                    switch (i) {
                        case 0:
                            scoreText.setTextColor(0xFFFFD700); // Gold
                            break;
                        case 1:
                            scoreText.setTextColor(0xFFC0C0C0); // Silver
                            break;
                        case 2:
                            scoreText.setTextColor(0xFFCD7F32); // Bronze
                            break;
                    }
                }
                
                scoresContainer.addView(scoreText);
            }
        }
    }
}