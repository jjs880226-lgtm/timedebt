package com.example.myapplication1;

public class DebtTask {
    final String name;
    final int minutes;
    final int penaltyPercent;
    long remainingMillis;

    DebtTask(String name, int minutes, int penaltyPercent, long remainingMillis) {
        this.name = name;
        this.minutes = minutes;
        this.penaltyPercent = penaltyPercent;
        this.remainingMillis = remainingMillis;
    }

    long totalMillis() {
        return minutes * 60_000L;
    }

    int remainingMinutes() {
        return (int) Math.ceil(remainingMillis / 60_000.0);
    }
}
