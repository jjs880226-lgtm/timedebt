package com.example.myapplication1;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GameData {

    static final String PREFS_NAME = "time_debt_prefs";
    static final int BANKRUPTCY_LIMIT_MINUTES = 480;
    static final String DEFAULT_AI_MESSAGE =
            "AI: 집중 중 궁금한 것을 물어보세요. 예) 뭐부터 할까?, 쉬어도 돼?, 남은 시간 어때?";

    enum DebtRiskLevel {
        SAFE, CAUTION, WARNING, CRITICAL, BANKRUPTCY
    }

    private final SharedPreferences prefs;

    final List<DebtTask> tasks = new ArrayList<>();
    int currentTaskIndex = -1;
    int debtMinutes = 0;
    int todayDueMinutes = 0;
    int successCount = 0;
    int failCount = 0;
    int leaveWarningCount = 0;
    int points = 0;
    boolean isRunning = false;
    String aiChatHistory = DEFAULT_AI_MESSAGE;
    int currentStreak = 0;
    int bestStreak = 0;
    String unlockedAchievements = "";

    GameData(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    void load() {
        tasks.clear();
        tasks.addAll(parseTasks(prefs.getString("tasks", "")));
        currentTaskIndex = prefs.getInt("currentTaskIndex", -1);
        debtMinutes = prefs.getInt("debtMinutes", calculateTaskDebt());
        todayDueMinutes = prefs.getInt("todayDueMinutes", calculateTaskDebt());
        successCount = prefs.getInt("successCount", 0);
        failCount = prefs.getInt("failCount", 0);
        leaveWarningCount = prefs.getInt("leaveWarningCount", 0);
        points = prefs.getInt("points", 0);
        isRunning = prefs.getBoolean("isRunning", false);
        aiChatHistory = prefs.getString("aiChatHistory", DEFAULT_AI_MESSAGE);
        currentStreak = prefs.getInt("currentStreak", 0);
        bestStreak = prefs.getInt("bestStreak", 0);
        unlockedAchievements = prefs.getString("unlockedAchievements", "");

        if (currentTaskIndex >= tasks.size()) {
            currentTaskIndex = -1;
        }

        refreshStreakOnLoad();
    }

    void save() {
        prefs.edit()
                .putString("tasks", serializeTasks())
                .putInt("currentTaskIndex", currentTaskIndex)
                .putInt("debtMinutes", debtMinutes)
                .putInt("todayDueMinutes", todayDueMinutes)
                .putInt("successCount", successCount)
                .putInt("failCount", failCount)
                .putInt("leaveWarningCount", leaveWarningCount)
                .putInt("points", points)
                .putBoolean("isRunning", isRunning)
                .putString("aiChatHistory", aiChatHistory)
                .putInt("currentStreak", currentStreak)
                .putInt("bestStreak", bestStreak)
                .putString("unlockedAchievements", unlockedAchievements)
                .putString("lastStreakDate", prefs.getString("lastStreakDate", ""))
                .apply();
    }

    DebtTask getCurrentTask() {
        if (currentTaskIndex < 0 || currentTaskIndex >= tasks.size()) {
            return null;
        }
        return tasks.get(currentTaskIndex);
    }

    int calculateTaskDebt() {
        int total = 0;
        for (DebtTask task : tasks) {
            total += task.minutes;
        }
        return total;
    }

    DebtRiskLevel getDebtRiskLevel() {
        if (debtMinutes >= BANKRUPTCY_LIMIT_MINUTES) {
            return DebtRiskLevel.BANKRUPTCY;
        }
        int percent = (debtMinutes * 100) / BANKRUPTCY_LIMIT_MINUTES;
        if (percent >= 75) {
            return DebtRiskLevel.CRITICAL;
        }
        if (percent >= 50) {
            return DebtRiskLevel.WARNING;
        }
        if (percent >= 25) {
            return DebtRiskLevel.CAUTION;
        }
        return DebtRiskLevel.SAFE;
    }

    int getDebtRiskPercent() {
        return Math.min(100, (debtMinutes * 100) / BANKRUPTCY_LIMIT_MINUTES);
    }

    int getMinutesUntilBankruptcy() {
        return Math.max(0, BANKRUPTCY_LIMIT_MINUTES - debtMinutes);
    }

    String getDebtRiskLabel() {
        switch (getDebtRiskLevel()) {
            case CAUTION:
                return "주의";
            case WARNING:
                return "위험";
            case CRITICAL:
                return "파산 임박";
            case BANKRUPTCY:
                return "파산";
            case SAFE:
            default:
                return "안전";
        }
    }

    void recordSuccessfulDay() {
        String today = todayString();
        String lastDate = prefs.getString("lastStreakDate", "");

        if (today.equals(lastDate)) {
            return;
        }

        if (isYesterday(lastDate)) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }

        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }

        prefs.edit().putString("lastStreakDate", today).apply();
    }

    private void refreshStreakOnLoad() {
        String lastDate = prefs.getString("lastStreakDate", "");
        if (TextUtils.isEmpty(lastDate)) {
            return;
        }

        String today = todayString();
        if (today.equals(lastDate) || isYesterday(lastDate)) {
            return;
        }

        currentStreak = 0;
    }

    boolean hasAchievement(String id) {
        if (TextUtils.isEmpty(unlockedAchievements)) {
            return false;
        }
        String[] ids = unlockedAchievements.split(",");
        for (String existing : ids) {
            if (id.equals(existing.trim())) {
                return true;
            }
        }
        return false;
    }

    boolean unlockAchievement(String id) {
        if (hasAchievement(id)) {
            return false;
        }
        if (TextUtils.isEmpty(unlockedAchievements)) {
            unlockedAchievements = id;
        } else {
            unlockedAchievements = unlockedAchievements + "," + id;
        }
        return true;
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
    }

    private boolean isYesterday(String date) {
        if (TextUtils.isEmpty(date)) {
            return false;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            Date parsed = format.parse(date);
            if (parsed == null) {
                return false;
            }
            long diff = format.parse(todayString()).getTime() - parsed.getTime();
            return diff >= 86_400_000L && diff < 172_800_000L;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String serializeTasks() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tasks.size(); i++) {
            DebtTask task = tasks.get(i);
            builder.append(Uri.encode(task.name))
                    .append(",")
                    .append(task.minutes)
                    .append(",")
                    .append(task.penaltyPercent)
                    .append(",")
                    .append(task.remainingMillis);
            if (i < tasks.size() - 1) {
                builder.append(";");
            }
        }
        return builder.toString();
    }

    private List<DebtTask> parseTasks(String rawTasks) {
        List<DebtTask> parsedTasks = new ArrayList<>();
        if (TextUtils.isEmpty(rawTasks)) {
            return parsedTasks;
        }

        String[] rows = rawTasks.split(";");
        for (String row : rows) {
            String[] parts = row.split(",");
            if (parts.length != 4) {
                continue;
            }

            try {
                String name = Uri.decode(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int penaltyPercent = Integer.parseInt(parts[2]);
                long remainingMillis = Long.parseLong(parts[3]);
                parsedTasks.add(new DebtTask(name, minutes, penaltyPercent, remainingMillis));
            } catch (NumberFormatException ignored) {
                // Broken saved rows are skipped so the app can still open.
            }
        }
        return parsedTasks;
    }
}
