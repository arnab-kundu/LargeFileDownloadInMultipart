package com.example.largefiledownloadinmultipart;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

public class ProgressIndicatorActivity extends AppCompatActivity {


    float max = 1;
    float update = 0;
    boolean threadRunning = false;
    ProgressIndicator mProgressIndicator1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_indicator);

        mProgressIndicator1 = (ProgressIndicator) findViewById(R.id.determinate_progress_indicator1);
        mProgressIndicator1.setForegroundColor(Color.parseColor("#AAAAAA"));
        mProgressIndicator1.setBackgroundColor(Color.WHITE);
        mProgressIndicator1.setPieStyle(true);
        startThread();
    }



    private void startThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                threadRunning = true;
                update = 0;
                while (update <= max) {
                    update += 0.005;
                    updateProgressIndicatorValue();
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {

                    }
                }
                threadRunning = false;
            }
        }).start();
    }

    private void updateProgressIndicatorValue() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressIndicator1.setValue(update);
            }
        });
    }

}
