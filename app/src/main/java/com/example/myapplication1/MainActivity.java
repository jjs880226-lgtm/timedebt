package com.example.myapplication1;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int[] PENALTY_PERCENT_VALUES = {5, 10, 15, 20, 25, 30};

    private TextView debtText;
    private TextView todayDueText;
    private TextView pointText;
    private TextView statusText;
    private TextView taskListText;
    private TextView currentTaskText;
    private TextView warningText;
    private TextView aiChatText;
    private TextView timerText;
    private TextView statsText;
    private TextView debtRiskBadge;
    private TextView debtRiskDetailText;
    private TextView streakText;
    private TextView achievementText;
    private EditText taskNameInput;
    private EditText taskMinutesInput;
    private EditText aiQuestionInput;
    private Spinner penaltySpinner;
    private Spinner taskSelectSpinner;
    private ProgressBar debtProgress;
    private ProgressBar focusProgress;
    private Button addTaskButton;
    private Button startFocusButton;
    private Button stopFocusButton;
    private Button bankruptcyButton;
    private Button buyMealButton;
    private Button buyAirButton;
    private Button buySleepButton;
    private Button buyRestButton;
    private Button shopToggleButton;
    private Button sendAiQuestionButton;
    private LinearLayout shopItemsPanel;
    private LinearLayout debtCardContainer;

    private GameData data;
    private ArrayAdapter<String> taskSelectAdapter;
    private boolean shopOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        data = new GameData(this);
        bindViews();
        setupPenaltySpinner();
        setupTaskSelectSpinner();
        connectActions();
        reloadAndRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadAndRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        data.save();
    }

    private void reloadAndRefresh() {
        data.load();
        refreshTaskSpinner();
        updateScreen();
    }

    private void bindViews() {
        debtText = findViewById(R.id.debtText);
        todayDueText = findViewById(R.id.todayDueText);
        pointText = findViewById(R.id.pointText);
        statusText = findViewById(R.id.statusText);
        taskListText = findViewById(R.id.taskListText);
        currentTaskText = findViewById(R.id.currentTaskText);
        warningText = findViewById(R.id.warningText);
        aiChatText = findViewById(R.id.aiChatText);
        timerText = findViewById(R.id.timerText);
        statsText = findViewById(R.id.statsText);
        debtRiskBadge = findViewById(R.id.debtRiskBadge);
        debtRiskDetailText = findViewById(R.id.debtRiskDetailText);
        streakText = findViewById(R.id.streakText);
        achievementText = findViewById(R.id.achievementText);
        taskNameInput = findViewById(R.id.taskNameInput);
        taskMinutesInput = findViewById(R.id.taskMinutesInput);
        aiQuestionInput = findViewById(R.id.aiQuestionInput);
        penaltySpinner = findViewById(R.id.penaltySpinner);
        taskSelectSpinner = findViewById(R.id.taskSelectSpinner);
        debtProgress = findViewById(R.id.debtProgress);
        focusProgress = findViewById(R.id.focusProgress);
        addTaskButton = findViewById(R.id.addTaskButton);
        startFocusButton = findViewById(R.id.startFocusButton);
        stopFocusButton = findViewById(R.id.stopFocusButton);
        bankruptcyButton = findViewById(R.id.bankruptcyButton);
        buyMealButton = findViewById(R.id.buyMealButton);
        buyAirButton = findViewById(R.id.buyAirButton);
        buySleepButton = findViewById(R.id.buySleepButton);
        buyRestButton = findViewById(R.id.buyRestButton);
        shopToggleButton = findViewById(R.id.shopToggleButton);
        shopItemsPanel = findViewById(R.id.shopItemsPanel);
        sendAiQuestionButton = findViewById(R.id.sendAiQuestionButton);
        debtCardContainer = findViewById(R.id.debtCardContainer);
    }

    private void setupPenaltySpinner() {
        String[] labels = new String[PENALTY_PERCENT_VALUES.length];
        for (int i = 0; i < PENALTY_PERCENT_VALUES.length; i++) {
            labels[i] = "+" + PENALTY_PERCENT_VALUES[i] + "%";
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        penaltySpinner.setAdapter(adapter);
        penaltySpinner.setSelection(1);
    }

    private void setupTaskSelectSpinner() {
        taskSelectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        taskSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskSelectSpinner.setAdapter(taskSelectAdapter);
        taskSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < data.tasks.size()) {
                    data.currentTaskIndex = position;
                    data.leaveWarningCount = 0;
                    updateScreen();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                data.currentTaskIndex = -1;
                updateScreen();
            }
        });
    }

    private void connectActions() {
        addTaskButton.setOnClickListener(view -> addTask());
        startFocusButton.setOnClickListener(view -> startFocus());
        stopFocusButton.setOnClickListener(view -> cancelPausedFocus());
        bankruptcyButton.setOnClickListener(view -> resetByBankruptcy());
        buyMealButton.setOnClickListener(view -> buyShopItem("밥먹기", 10));
        buyAirButton.setOnClickListener(view -> buyShopItem("잠깐 바람쐬러 나가기", 5));
        buySleepButton.setOnClickListener(view -> buyShopItem("잠자기", 25));
        buyRestButton.setOnClickListener(view -> buyShopItem("1시간 쉬기", 10));
        shopToggleButton.setOnClickListener(view -> toggleShop());
        sendAiQuestionButton.setOnClickListener(view -> sendAiQuestion());
        aiQuestionInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendAiQuestion();
                return true;
            }
            return false;
        });
    }

    private void addTask() {
        String taskName = taskNameInput.getText().toString().trim();
        String minutesText = taskMinutesInput.getText().toString().trim();

        if (TextUtils.isEmpty(taskName)) {
            taskNameInput.setError("할 일 이름을 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(minutesText)) {
            taskMinutesInput.setError("기본 시간을 입력하세요.");
            return;
        }

        int minutes;
        try {
            minutes = Integer.parseInt(minutesText);
        } catch (NumberFormatException e) {
            taskMinutesInput.setError("숫자로 입력하세요.");
            return;
        }

        if (minutes <= 0) {
            taskMinutesInput.setError("1분 이상 입력하세요.");
            return;
        }

        int penaltyPercent = PENALTY_PERCENT_VALUES[penaltySpinner.getSelectedItemPosition()];
        data.tasks.add(new DebtTask(taskName, minutes, penaltyPercent, minutes * 60_000L));
        data.debtMinutes += minutes;
        data.todayDueMinutes += minutes;

        taskNameInput.setText("");
        taskMinutesInput.setText("");
        statusText.setText("새 시간 빚이 생성되었습니다. 집중 화면에서 원하는 할 일을 골라 갚을 수 있습니다.");
        refreshTaskSpinner();
        data.save();
        updateScreen();
    }

    private void startFocus() {
        if (data.tasks.isEmpty()) {
            Toast.makeText(this, "먼저 할 일을 추가하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedIndex = taskSelectSpinner.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= data.tasks.size()) {
            Toast.makeText(this, "갚을 할 일을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        data.currentTaskIndex = selectedIndex;
        DebtTask task = data.getCurrentTask();
        if (task == null) {
            return;
        }

        if (task.remainingMillis <= 0L || task.remainingMillis > task.totalMillis()) {
            task.remainingMillis = task.totalMillis();
        }

        data.leaveWarningCount = 0;
        data.save();

        startActivity(new Intent(this, FocusActivity.class));
    }

    private void cancelPausedFocus() {
        data.isRunning = false;
        data.leaveWarningCount = 0;
        DebtTask task = data.getCurrentTask();
        if (task != null) {
            task.remainingMillis = task.totalMillis();
        }
        statusText.setText("집중을 취소했습니다. 할 일은 처음부터 다시 시작합니다.");
        data.save();
        updateScreen();
    }

    private void sendAiQuestion() {
        String question = aiQuestionInput.getText().toString().trim();
        if (TextUtils.isEmpty(question)) {
            aiQuestionInput.setError("질문을 입력하세요.");
            return;
        }

        DebtTask task = getVisibleTask();
        String answer = AiHelper.makeAnswer(data, question, task, data.isRunning);
        data.aiChatHistory = AiHelper.trimChatHistory(
                data.aiChatHistory + "\n\n나: " + question + "\nAI: " + answer);
        aiChatText.setText(data.aiChatHistory);
        aiQuestionInput.setText("");
        data.save();
    }

    private void toggleShop() {
        shopOpen = !shopOpen;
        shopItemsPanel.setVisibility(shopOpen ? View.VISIBLE : View.GONE);
        shopToggleButton.setText(shopOpen ? "상점 닫기" : "상점 열기");
    }

    private void buyShopItem(String itemName, int cost) {
        if (data.points < cost) {
            Toast.makeText(this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
            statusText.setText(itemName + " 구매 실패. " + (cost - data.points) + "P가 더 필요합니다.");
            return;
        }

        data.points -= cost;
        statusText.setText(itemName + " 구매 완료! 남은 포인트: " + data.points + "P");
        data.save();
        updateScreen();
    }

    private void resetByBankruptcy() {
        data.tasks.clear();
        data.debtMinutes = 0;
        data.todayDueMinutes = 0;
        data.currentTaskIndex = -1;
        data.leaveWarningCount = 0;
        data.isRunning = false;
        statusText.setText("파산 처리 완료. 시간 빚과 할 일 목록을 초기화했습니다.");
        refreshTaskSpinner();
        data.save();
        updateScreen();
    }

    private void refreshTaskSpinner() {
        taskSelectAdapter.clear();
        for (DebtTask task : data.tasks) {
            taskSelectAdapter.add(task.name + " · 남은 " + formatMinutes(task.remainingMinutes()));
        }
        taskSelectAdapter.notifyDataSetChanged();
        if (!data.tasks.isEmpty()) {
            taskSelectSpinner.setSelection(Math.max(0, Math.min(data.currentTaskIndex, data.tasks.size() - 1)));
        }
    }

    private void updateScreen() {
        debtText.setText(formatMinutes(data.debtMinutes));
        todayDueText.setText("오늘 갚아야 할 시간: " + data.todayDueMinutes + "분");
        pointText.setText("보유 포인트: " + data.points + "P");
        debtProgress.setProgress(Math.min(GameData.BANKRUPTCY_LIMIT_MINUTES, data.debtMinutes));
        taskListText.setText(buildTaskListText());
        aiChatText.setText(data.aiChatHistory);
        updateDebtRiskVisualization();
        updateStreakAndAchievements();

        DebtTask activeTask = getVisibleTask();
        if (activeTask == null) {
            currentTaskText.setText("대기 중인 할 일이 없습니다.");
            timerText.setText("00:00");
            warningText.setText("앱 이탈 경고: 0/3");
            focusProgress.setProgress(0);
            startFocusButton.setText("집중 화면으로 시작");
            stopFocusButton.setEnabled(false);
        } else {
            currentTaskText.setText(activeTask.name + " · " + activeTask.minutes + "분 · 패널티 +" + activeTask.penaltyPercent + "%");
            timerText.setText(formatTimer(activeTask.remainingMillis));
            warningText.setText("앱 이탈 경고: " + Math.min(data.leaveWarningCount, 3) + "/3");
            boolean isPaused = activeTask.remainingMillis > 0L
                    && activeTask.remainingMillis < activeTask.totalMillis();
            startFocusButton.setText(isPaused ? "집중 화면에서 재개" : "집중 화면으로 시작");
            stopFocusButton.setEnabled(isPaused);
            updateFocusProgress(activeTask);
        }

        int totalAttempts = data.successCount + data.failCount;
        int successRate = totalAttempts == 0 ? 0 : (data.successCount * 100) / totalAttempts;
        statsText.setText("성공 " + data.successCount + "회 · 실패 " + data.failCount + "회 · 성공률 " + successRate + "%");
        taskSelectSpinner.setEnabled(true);
    }

    private void updateDebtRiskVisualization() {
        GameData.DebtRiskLevel level = data.getDebtRiskLevel();
        int percent = data.getDebtRiskPercent();
        int remaining = data.getMinutesUntilBankruptcy();

        debtRiskBadge.setText(data.getDebtRiskLabel());
        debtRiskDetailText.setText(
                "빚 위험도 " + percent + "% · 파산까지 " + formatMinutes(remaining));

        int cardBackground;
        int progressColor;
        switch (level) {
            case CAUTION:
                cardBackground = R.drawable.bg_debt_card_caution;
                progressColor = 0xFFFBBF24;
                break;
            case WARNING:
                cardBackground = R.drawable.bg_debt_card_warning;
                progressColor = 0xFFF97316;
                break;
            case CRITICAL:
            case BANKRUPTCY:
                cardBackground = R.drawable.bg_debt_card_critical;
                progressColor = 0xFFEF4444;
                break;
            case SAFE:
            default:
                cardBackground = R.drawable.bg_debt_card_safe;
                progressColor = 0xFF86EFAC;
                break;
        }

        debtCardContainer.setBackgroundResource(cardBackground);
        debtProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        if (level == GameData.DebtRiskLevel.CRITICAL || level == GameData.DebtRiskLevel.BANKRUPTCY) {
            debtRiskDetailText.setText(
                    "⚠️ " + debtRiskDetailText.getText() + " · 지금 바로 짧은 할 일부터 갚으세요!");
        }
    }

    private void updateStreakAndAchievements() {
        streakText.setText("🔥 연속 " + data.currentStreak + "일 · 최고 " + data.bestStreak + "일");
        achievementText.setText(AchievementManager.buildAchievementSummary(data));
    }

    private DebtTask getVisibleTask() {
        DebtTask runningTask = data.getCurrentTask();
        if (runningTask != null) {
            return runningTask;
        }

        int selectedIndex = taskSelectSpinner.getSelectedItemPosition();
        if (selectedIndex >= 0 && selectedIndex < data.tasks.size()) {
            return data.tasks.get(selectedIndex);
        }
        return null;
    }

    private String buildTaskListText() {
        if (data.tasks.isEmpty()) {
            return "등록된 할 일이 없습니다.";
        }

        StringBuilder builder = new StringBuilder("등록된 할 일\n");
        for (int i = 0; i < data.tasks.size(); i++) {
            DebtTask task = data.tasks.get(i);
            builder.append(i + 1)
                    .append(". ")
                    .append(task.name)
                    .append(" · ")
                    .append(task.minutes)
                    .append("분 · 남은 ")
                    .append(formatMinutes(task.remainingMinutes()))
                    .append(" · +")
                    .append(task.penaltyPercent)
                    .append("%");
            if (i < data.tasks.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private void updateFocusProgress(DebtTask task) {
        if (task.minutes <= 0) {
            focusProgress.setProgress(0);
            return;
        }

        long elapsedMillis = task.totalMillis() - Math.max(0L, task.remainingMillis);
        int progress = (int) ((elapsedMillis * 100L) / task.totalMillis());
        focusProgress.setProgress(Math.max(0, Math.min(100, progress)));
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int minutePart = minutes % 60;
        return hours + "시간 " + minutePart + "분";
    }

    private String formatTimer(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.KOREA, "%02d:%02d", minutes, seconds);
    }
}
