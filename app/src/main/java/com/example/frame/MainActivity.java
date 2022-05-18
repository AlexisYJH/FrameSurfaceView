package com.example.frame;

import static com.example.frame.FrameSurfaceView.INFINITE;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FrameSurfaceView surfaceView = findViewById(R.id.surfaceView);
        ArrayList<Integer> list = new ArrayList<>();
        list.add(R.drawable.frame0);
        list.add(R.drawable.frame1);
        list.add(R.drawable.frame2);
        list.add(R.drawable.frame3);
        list.add(R.drawable.frame4);
        list.add(R.drawable.frame5);
        list.add(R.drawable.frame6);
        list.add(R.drawable.frame7);
        list.add(R.drawable.frame8);
        list.add(R.drawable.frame9);
        list.add(R.drawable.frame10);
        list.add(R.drawable.frame11);
        list.add(R.drawable.frame12);
        list.add(R.drawable.frame13);
        list.add(R.drawable.frame14);
        list.add(R.drawable.frame15);
        list.add(R.drawable.frame16);
        list.add(R.drawable.frame17);
        list.add(R.drawable.frame18);
        list.add(R.drawable.frame19);
        surfaceView.setDrawableIds(list);
        surfaceView.start();
    }
}