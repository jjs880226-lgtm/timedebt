package com.example.myapplication1;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class FocusActivity extends Activity {

    private GameData data;
    private CountDownTimer timer;

    private TextView focusTaskNameText;
    private TextView focusTaskMetaText;
    private TextView focusTimerText;
    private TextView focusWarningText;
    private TextView focusEncourageText;
    private TextView focusAiChatText;
    private EditText focusAiQuestionInput;
    private ProgressBar focusSessionProgress;
    private Button focusPauseButton;
    private Button focusExitButton;
    private Button focusSendAiButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        data = new GameData(this);
        data.load();

        bindViews();
        connectActions();

        DebtTask task = data.getCurrentTask();
        if (task == null) {
            Toast.makeText(this, "집중할 할 일이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (task.remainingMillis <= 0L || task.remainingMillis > task.totalMillis()) {
            task.remainingMillis = task.totalMillis();
        }

        data.isRunning = true;
        data.save();
        updateUi();
        startTimer(task.remainingMillis);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!data.isRunning) {
            return;
        }

        data.leaveWarningCount++;
        if (data.leaveWarningCount <= 3) {
            String message = "집중 모드 이탈 경고 " + data.leaveWarningCount + "/3";
            focusWarningText.setText("앱 이탈 경고: " + data.leaveWarningCount + "/3");
            focusEncourageText.setText(message + " · 4번째 이탈부터 실패 처리됩니다.");
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            data.save();
            return;
        }

        failCurrentTask("집중 모드 중 앱을 4번 이탈하여 실패 처리되었습니다.");
    }

    @Override
    public void onBackPressed() {
        pauseFocus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        data.save();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }

    private void bindViews() {
        focusTaskNameText = findViewById(R.id.focusTaskNameText);
        focusTaskMetaText = findViewById(R.id.focusTaskMetaText);
        focusTimerText = findViewById(R.id.focusTimerText);
        focusWarningText = findViewById(R.id.focusWarningText);
        focusEncourageText = findViewById(R.id.focusEncourageText);
        focusAiChatText = findViewById(R.id.focusAiChatText);
        focusAiQuestionInput = findViewById(R.id.focusAiQuestionInput);
        focusSessionProgress = findViewById(R.id.focusSessionProgress);
        focusPauseButton = findViewById(R.id.focusPauseButton);
        focusExitButton = findViewById(R.id.focusExitButton);
        focusSendAiButton = findViewById(R.id.focusSendAiButton);
    }

    private void connectActions() {
        focusPauseButton.setOnClickListener(view -> pauseFocus());
        focusExitButton.setOnClickListener(view -> pauseFocus());
        focusSendAiButton.setOnClickListener(view -> sendAiQuestion());
        focusAiQuestionInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAiQuestion();
                return true;
            }
            return false;
        });
    }

    private void startTimer(long remainingMillis) {
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(remainingMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                DebtTask runningTask = data.getCurrentTask();
                if (runningTask == null) {
                    return;
                }
                runningTask.remainingMillis = millisUntilFinished;
                focusTimerText.setText(formatTimer(millisUntilFinished));
                updateSessionProgress();
            }

            @Override
            public void onFinish() {
                DebtTask finishedTask = data.getCurrentTask();
                if (finishedTask != null) {
                    finishedTask.remainingMillis = 0L;
                }
                completeCurrentTask();
            }
        };
        timer.start();
    }

    private void pauseFocus() {
        if (!data.isRunning) {
            finish();
            return;
        }

        if (timer != null) {
            timer.cancel();
        }

        data.isRunning = false;
        data.save();
        Toast.makeText(this, "일시정지되었습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void completeCurrentTask() {
        if (timer != null) {
            timer.cancel();
        }

        DebtTask task = data.getCurrentTask();
        if (task == null) {
            clearFocusState();
            data.save();
            finish();
            return;
        }

        int debtBefore = data.debtMinutes;
        int earnedPoints = (task.minutes * 5) / 60;
        data.points += earnedPoints;
        data.debtMinutes = Math.max(0, data.debtMinutes - task.minutes);
        data.todayDueMinutes = Math.max(0, data.todayDueMinutes - task.minutes);
        data.successCount++;
        data.recordSuccessfulDay();

        DebtTask completedTask = task;
        String taskName = task.name;
        data.tasks.remove(data.currentTaskIndex);
        clearFocusState();
        AchievementManager.checkAfterSuccess(this, data, completedTask, debtBefore);
        data.save();

        Toast.makeText(this, "성공! " + taskName + " 빚을 갚고 " + earnedPoints + "P 획득", Toast.LENGTH_LONG).show();
        finish();
    }

    private void failCurrentTask(String message) {
        if (timer != null) {
            timer.cancel();
        }

        DebtTask task = data.getCurrentTask();
        if (task == null) {
            finish();
            return;
        }

        int penalty = Math.max(1, (int) Math.ceil(task.minutes * (task.penaltyPercent / 100.0)));
        data.debtMinutes += penalty;
        data.todayDueMinutes += penalty;
        data.failCount++;
        data.isRunning = false;
        task.remainingMillis = task.totalMillis();
        data.leaveWarningCount = 0;

        if (data.debtMinutes >= GameData.BANKRUPTCY_LIMIT_MINUTES) {
            data.debtMinutes = 0;
            data.todayDueMinutes = 0;
            data.tasks.clear();
            data.currentTaskIndex = -1;
            Toast.makeText(this, "빚이 8시간을 넘었습니다. 파산 시스템으로 초기화되었습니다.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, message + " 패널티: +" + penalty + "분", Toast.LENGTH_LONG).show();
        }

        data.save();
        finish();
    }

    private void sendAiQuestion() {
        String question = focusAiQuestionInput.getText().toString().trim();
        if (TextUtils.isEmpty(question)) {
            focusAiQuestionInput.setError("질문을 입력하세요.");
            return;
        }

        DebtTask task = data.getCurrentTask();
        String answer = AiHelper.makeAnswer(data, question, task, data.isRunning);
        data.aiChatHistory = AiHelper.trimChatHistory(
                data.aiChatHistory + "\n\n나: " + question + "\nAI: " + answer);
        focusAiChatText.setText(data.aiChatHistory);
        focusAiQuestionInput.setText("");
        data.save();
    }

    private void clearFocusState() {
        data.currentTaskIndex = -1;
        data.leaveWarningCount = 0;
        data.isRunning = false;
    }

    private void updateUi() {
        DebtTask task = data.getCurrentTask();
        if (task == null) {
            return;
        }

        focusTaskNameText.setText(task.name);
        focusTaskMetaText.setText(task.minutes + "분 · 패널티 +" + task.penaltyPercent + "%");
        focusTimerText.setText(formatTimer(task.remainingMillis));
        focusWarningText.setText("앱 이탈 경고: " + Math.min(data.leaveWarningCount, 3) + "/3");
        focusAiChatText.setText(data.aiChatHistory);
        updateSessionProgress();
        updateEncourageText(task);
    }

    private void updateEncourageText(DebtTask task) {
        int remaining = task.remainingMinutes();
        if (remaining <= 5) {
            focusEncourageText.setText("거의 다 왔어요! " + remaining + "분만 더 집중하면 빚이 줄어듭니다.");
        } else if (remaining <= 15) {
            focusEncourageText.setText("좋아요. " + remaining + "분 남았습니다. 지금 이 화면만 보세요.");
        } else {
            focusEncourageText.setText("집중 모드 전용 화면입니다. 다른 앱으로 나가면 경고가 쌓입니다.");
        }
    }

    private void updateSessionProgress() {
        DebtTask task = data.getCurrentTask();
        if (task == null || task.minutes <= 0) {
            focusSessionProgress.setProgress(0);
            return;
        }

        long elapsedMillis = task.totalMillis() - Math.max(0L, task.remainingMillis);
        int progress = (int) ((elapsedMillis * 100L) / task.totalMillis());
        focusSessionProgress.setProgress(Math.max(0, Math.min(100, progress)));
    }

    private String formatTimer(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.KOREA, "%02d:%02d", minutes, seconds);
    }
}
