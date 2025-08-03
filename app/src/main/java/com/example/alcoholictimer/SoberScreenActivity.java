// ...existing imports...
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.alcoholictimer.R; // R 클래스 import 추가
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class SoberScreenActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private int elapsedSeconds = 0;
    private boolean isTimerRunning = false;
    private TextView tvTimeDetail; // 변수명 변경

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_status);

        tvTimeDetail = findViewById(R.id.tvTimeDetail); // ID 변경

        // 타이머 시작 버튼 설정
        FloatingActionButton btnStartTimer = findViewById(R.id.btnStopSobriety);
        btnStartTimer.setOnClickListener(v -> {
            if (!isTimerRunning) {
                startTimer();
            }
        });
    }

    private void startTimer() {
        elapsedSeconds = 0; // 타이머 초기화
        isTimerRunning = true; // 타이머 상태 변경
        handler.post(timerRunnable); // Runnable 실행
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTimerRunning) {
                int minutes = elapsedSeconds / 60;
                int seconds = elapsedSeconds % 60;
                tvTimeDetail.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                elapsedSeconds++;
                handler.postDelayed(this, 1000); // 1초마다 업데이트
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable); // 핸들러 콜백 제거
        isTimerRunning = false; // 타이머 상태 초기화
    }
}
