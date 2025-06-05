package com.project.reactiontest;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout gameLayout;
    private TextView tvMessage, tvScore, tvRecords;
    private int targetColor;
    private int currentColor;
    private int score = 0;
    private long reactionStartTime;
    private boolean isGameActive;
    private boolean isWaitingForReaction;
    private final Random random = new Random();
    private final Handler handler = new Handler();

    //Цвета
    private final int[] colors = {Color.RED, Color.GREEN, Color.BLUE};
    private final String[] colorNames = {"КРАСНЫЙ", "ЗЕЛЁНЫЙ", "СИНИЙ"};

    //Рекорды
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "ReactionPrefs";
    private static final String SCORES_KEY = "top_scores";
    private static final String TIMES_KEY = "top_times";
    private int[] topScores = new int[3];
    private long[] topReactionTimes = new long[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameLayout = findViewById(R.id.gameLayout);
        tvMessage = findViewById(R.id.tvMessage);
        tvScore = findViewById(R.id.tvScore);
        tvRecords = findViewById(R.id.tvRecords);

        //Загрузка рекордов
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadRecords();
        updateRecordsDisplay();

        //Запуск игры
        startGame();

        //Нажатия
        gameLayout.setOnClickListener(v -> {
            if (!isGameActive) {
                startGame();
            } else {
                if (currentColor == targetColor && isWaitingForReaction) {
                    long reactionTime = System.currentTimeMillis() - reactionStartTime;
                    score += calculateScore(reactionTime);
                    tvScore.setText(String.format("Очки: %d | Реакция: %d мс", score, reactionTime));
                    checkRecords(score, reactionTime);
                    setNewTargetColor();
                } else if (currentColor != targetColor) {
                    score = Math.max(0, score - 1);
                    tvScore.setText(String.format("Очки: %d | Ошибка!", score));
                }
            }
        });
    }

    //Ещё загрузка рекордов
    private void loadRecords() {
        String scores = prefs.getString(SCORES_KEY, "0,0,0");
        String times = prefs.getString(TIMES_KEY, "9999,9999,9999");

        String[] scoreParts = scores.split(",");
        String[] timeParts = times.split(",");

        for (int i = 0; i < 3; i++) {
            topScores[i] = Integer.parseInt(scoreParts[i]);
            topReactionTimes[i] = Long.parseLong(timeParts[i]);
        }
    }

    //Их сохранение
    private void saveRecords() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SCORES_KEY,
                topScores[0] + "," + topScores[1] + "," + topScores[2]);
        editor.putString(TIMES_KEY,
                topReactionTimes[0] + "," + topReactionTimes[1] + "," + topReactionTimes[2]);
        editor.apply();
    }

    //ещё что-то
    private void checkRecords(int newScore, long newTime) {
        boolean updated = false;

        //дополнительно
        for (int i = 0; i < 3; i++) {
            if (newScore > topScores[i]) {
                //ну как сказать это приоритет рекордов над другими
                for (int j = 2; j > i; j--) {
                    topScores[j] = topScores[j-1];
                }
                topScores[i] = newScore;
                updated = true;
                break;
            }
        }

        //а тут по времени
        for (int i = 0; i < 3; i++) {
            if (newTime < topReactionTimes[i]) {
                for (int j = 2; j > i; j--) {
                    topReactionTimes[j] = topReactionTimes[j-1];
                }
                topReactionTimes[i] = newTime;
                updated = true;
                break;
            }
        }

        if (updated) {
            saveRecords();
            updateRecordsDisplay();
        }
    }

    //изменение отображения рекордов
    private void updateRecordsDisplay() {
        String recordsText = "Рекорды:\n";
        recordsText += "Очки: " + topScores[0] + ", " + topScores[1] + ", " + topScores[2] + "\n";
        recordsText += "Время: " + topReactionTimes[0] + "мс, " +
                topReactionTimes[1] + "мс, " +
                topReactionTimes[2] + "мс";
        tvRecords.setText(recordsText);
    }

    //расчёт реакции
    private int calculateScore(long reactionTime) {
        return Math.max(1, 6 - (int)(reactionTime / 200));
    }

    //игра заново
    private void startGame() {
        isGameActive = true;
        score = 0;
        tvScore.setText("Очки: 0 | Начните игру");
        setNewTargetColor();
        startColorRotation();
    }

    //установка нового цвета
    private void setNewTargetColor() {
        targetColor = colors[random.nextInt(colors.length)];
        tvMessage.setText("ЖМИ НА " + colorNames[getColorIndex(targetColor)]);
    }

    //смена цветов через n-ное время
    private void startColorRotation() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGameActive) return;

                isWaitingForReaction = false;
                currentColor = colors[random.nextInt(colors.length)];
                gameLayout.setBackgroundColor(currentColor);

                if (currentColor == targetColor) {
                    isWaitingForReaction = true;
                    reactionStartTime = System.currentTimeMillis();
                }

                handler.postDelayed(this, random.nextInt(2000) + 1000);
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
        isGameActive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGameActive) {
            startGame();
        }
    }

    //получение цвета
    private int getColorIndex(int color) {
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) return i;
        }
        return 0;
    }
}