package com.example.myapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(48, 48, 48, 48);

        final TextView text = new TextView(this);
        text.setText("স্বাগতম! আমার প্রথম অ্যাপ");
        text.setTextSize(24);
        text.setGravity(Gravity.CENTER);

        Button button = new Button(this);
        button.setText("ক্লিক করুন");
        button.setOnClickListener(v -> {
            count++;
            text.setText("ক্লিক সংখ্যা: " + count);
        });

        layout.addView(text);
        layout.addView(button);
        setContentView(layout);
    }
}
