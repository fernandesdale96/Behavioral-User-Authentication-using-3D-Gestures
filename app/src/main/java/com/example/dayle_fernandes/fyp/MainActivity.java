package com.example.dayle_fernandes.fyp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button recgesture;
    private Button comparegesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recgesture = (Button) findViewById(R.id.record_button);
        comparegesture = (Button) findViewById(R.id.compare_button);

        recgesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecordGesture.class);
                startActivity(intent);
            }
        });

        comparegesture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GestureMatching.class);
                startActivity(intent);
            }
        });





    }
}
