package com.example.mjbimsdk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by zhoulj on 2017/9/28.
 */
public class LuncherActivity extends Activity implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashReport.initCrashReport(getApplicationContext());

        setContentView(R.layout.luncher_layout);
        TextView tv1 = (TextView)findViewById(R.id.entry);
        tv1.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id)
        {
            case R.id.entry:
            {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
                break;

        }
    }
}
