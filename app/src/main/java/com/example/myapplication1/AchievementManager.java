package com.example.myapplication1;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class AchievementManager {

    static final class Achievement {
        final String id;
        final String title;
        final String description;

        Achievement(String id, String title, String description) {
            this.id = id;
            this.title = title;
            this.description = description;
        }
    }

    private static final Achievement[] ALL = {
            new Achievement("first_success", "첫 빚 갚기", "첫 집중 세션을 성공했습니다"),
            new Achievement("success_10", "집중 마스터", "집중 성공 10회 달성"),
            new Achievement("streak_3", "3일 연속", "3일 연속 집중 성공"),
            new Achievement("streak_7", "일주일 챌린저", "7일 연속 집중 성공"),
            new Achievement("debt_free", "빚 청산", "모든 시간 빚을 갚았습니다"),
            new Achievement("marathon", "마라톤 집중", "60분 이상 할 일을 완료했습니다"),
            new Achievement("survivor", "파산 생존", "파산 직전(75%+)에서 빚을 줄였습니다")
    };

    private AchievementManager() {
    }

    static List<Achievement> getAll() {
        List<Achievement> list = new ArrayList<>();
        for (Achievement achievement : ALL) {
            list.add(achievement);
        }
        return list;
    }

    static void checkAfterSuccess(Context context, GameData data, DebtTask completedTask, int debtBefore) {
        if (data.unlockAchievement("first_success")) {
            showUnlockToast(context, "첫 빚 갚기");
        }
        if (data.successCount >= 10 && data.unlockAchievement("success_10")) {
            showUnlockToast(context, "집중 마스터");
        }
        if (data.currentStreak >= 3 && data.unlockAchievement("streak_3")) {
            showUnlockToast(context, "3일 연속");
        }
        if (data.currentStreak >= 7 && data.unlockAchievement("streak_7")) {
            showUnlockToast(context, "일주일 챌린저");
        }
        if (data.debtMinutes == 0 && data.tasks.isEmpty() && data.unlockAchievement("debt_free")) {
            showUnlockToast(context, "빚 청산");
        }
        if (completedTask != null && completedTask.minutes >= 60 && data.unlockAchievement("marathon")) {
            showUnlockToast(context, "마라톤 집중");
        }
        int percentBefore = (debtBefore * 100) / GameData.BANKRUPTCY_LIMIT_MINUTES;
        if (percentBefore >= 75 && data.debtMinutes < debtBefore && data.unlockAchievement("survivor")) {
            showUnlockToast(context, "파산 생존");
        }
    }

    static String buildAchievementSummary(GameData data) {
        StringBuilder builder = new StringBuilder();
        for (Achievement achievement : ALL) {
            boolean unlocked = data.hasAchievement(achievement.id);
            builder.append(unlocked ? "✅ " : "🔒 ")
                    .append(achievement.title)
                    .append(" · ")
                    .append(achievement.description);
            builder.append("\n");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private static void showUnlockToast(Context context, String title) {
        Toast.makeText(context, "🏆 업적 해금: " + title, Toast.LENGTH_LONG).show();
    }
}
