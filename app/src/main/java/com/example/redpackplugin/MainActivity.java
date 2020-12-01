package com.example.redpackplugin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private String TAG = "RedPacketService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity::onStart: ");
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch switch1 = (Switch)findViewById(R.id.switch1);
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this,RedPackageService.class);
                if (isChecked){
                    startService(intent);
                    Log.d(TAG, "onCheckedChanged: 开始Service~~~");
                }
                else {
                    stopService(intent);
                    Log.d(TAG, "onCheckedChanged: 结束Service~~~");
                }
            }
        });
    }
}