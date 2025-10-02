package com.example.banga;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HighScoreManager {
    private static final String PREF_NAME = "BangaHighScores";
    private static final String KEY_HIGH_SCORES = "high_scores";
    private static final int MAX_SCORES = 8; // Keep top 8 scores
    
    private SharedPreferences prefs;
    
    public HighScoreManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveScore(int score) {
        List<Integer> scores = getHighScores();
        scores.add(score);
        Collections.sort(scores, Collections.reverseOrder()); // Sort descending
        
        // Keep only top MAX_SCORES
        if (scores.size() > MAX_SCORES) {
            scores = scores.subList(0, MAX_SCORES);
        }
        
        // Save to SharedPreferences
        Set<String> scoreStrings = new HashSet<>();
        for (int s : scores) {
            scoreStrings.add(String.valueOf(s));
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_HIGH_SCORES, scoreStrings);
        editor.apply();
    }
    
    public List<Integer> getHighScores() {
        Set<String> scoreStrings = prefs.getStringSet(KEY_HIGH_SCORES, new HashSet<>());
        List<Integer> scores = new ArrayList<>();
        
        for (String scoreString : scoreStrings) {
            try {
                scores.add(Integer.parseInt(scoreString));
            } catch (NumberFormatException e) {
                // Skip invalid scores
            }
        }
        
        Collections.sort(scores, Collections.reverseOrder());
        return scores;
    }
    
    public int getHighestScore() {
        List<Integer> scores = getHighScores();
        return scores.isEmpty() ? 0 : scores.get(0);
    }
    
    public boolean isNewHighScore(int score) {
        return score > getHighestScore();
    }
}