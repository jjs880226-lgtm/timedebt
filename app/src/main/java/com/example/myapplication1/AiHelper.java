package com.example.myapplication1;

import java.util.Locale;

final class AiHelper {

    private AiHelper() {
    }

    static String makeAnswer(GameData data, String question, DebtTask task, boolean isRunning) {
        String lowerQuestion = question.toLowerCase(Locale.KOREA);
        String taskName = task == null ? "선택한 할 일" : task.name;
        int remainingMinutes = task == null ? 0 : task.remainingMinutes();

        if (containsAny(lowerQuestion, "뭐부터", "먼저", "순서", "우선")) {
            if (task == null) {
                return "지금은 선택된 할 일이 없어요. 집중 모드에서 가장 짧은 할 일부터 선택하면 시작 장벽이 낮아집니다.";
            }
            return taskName + "부터 끝내는 게 좋아요. 남은 " + remainingMinutes + "분만 처리하면 빚과 목록이 바로 줄어듭니다.";
        }

        if (containsAny(lowerQuestion, "쉬", "휴식", "피곤", "졸려")) {
            if (isRunning && remainingMinutes <= 10) {
                return "남은 시간이 짧아요. 지금 쉬기보다 " + remainingMinutes + "분만 마무리하고 상점 보상으로 쉬는 쪽이 이득입니다.";
            }
            return "너무 피곤하면 일시정지 후 3분만 쉬세요. 단, 앱 이탈 경고가 쌓이지 않게 바로 돌아오는 게 좋아요.";
        }

        if (containsAny(lowerQuestion, "남은", "시간", "얼마")) {
            if (task == null) {
                return "아직 선택한 할 일이 없어서 남은 시간을 계산할 수 없어요. 갚을 할 일을 먼저 선택하세요.";
            }
            return taskName + "은 " + remainingMinutes + "분 남았습니다. 현재 전체 빚은 " + formatMinutes(data.debtMinutes) + "입니다.";
        }

        if (containsAny(lowerQuestion, "포인트", "보상", "상점")) {
            if (task == null) {
                return "현재 보유 포인트는 " + data.points + "P입니다. 60분 집중 성공마다 5P를 받을 수 있어요.";
            }
            return taskName + "을 성공하면 " + ((task.minutes * 5) / 60) + "P를 받고, 현재 보유 포인트는 " + data.points + "P입니다.";
        }

        if (containsAny(lowerQuestion, "빚", "위험", "파산")) {
            if (data.debtMinutes >= 300) {
                return "지금 빚이 큰 편이에요. 새 할 일을 만들기보다 짧은 항목부터 하나씩 갚는 전략이 안전합니다.";
            }
            return "현재 빚은 " + formatMinutes(data.debtMinutes) + "이라 아직 관리 가능합니다. 하나만 끝내도 흐름이 좋아져요.";
        }

        if (containsAny(lowerQuestion, "집중", "딴짓", "핸드폰", "유튜브")) {
            return "지금은 질문을 짧게 끝내고 타이머 화면만 보세요. 다음 행동은 하나입니다: " + taskName + "의 첫 줄부터 시작하기.";
        }

        if (containsAny(lowerQuestion, "계획", "방법", "어떻게")) {
            if (task == null) {
                return "계획은 짧게 잡는 게 좋아요. 할 일을 선택하고 10분만 진행한 뒤 계속할지 판단하세요.";
            }
            return taskName + "을 3단계로 쪼개세요. 준비, 핵심 작업, 제출/정리. 지금은 준비 단계만 시작하면 됩니다.";
        }

        return "좋은 질문이에요. 집중 모드에서는 완벽한 답보다 다음 행동이 중요합니다. " + taskName + "을 5분만 진행해보세요.";
    }

    static String trimChatHistory(String history) {
        int maxLength = 900;
        if (history.length() <= maxLength) {
            return history;
        }
        return "...\n" + history.substring(history.length() - maxLength);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int minutePart = minutes % 60;
        return hours + "시간 " + minutePart + "분";
    }
}
