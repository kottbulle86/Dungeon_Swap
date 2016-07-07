package se.andreasmikaelsson.dungeonswap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton buttonMainDM = (ImageButton) findViewById(R.id.main_button_dm);
        buttonMainDM.setOnClickListener(this);
        ImageButton buttonMainPlayer = (ImageButton) findViewById(R.id.main_button_player);
        buttonMainPlayer.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_button_dm:
                Intent intentDM = new Intent(MainActivity.this, DMMenuActivity.class);
                startActivity(intentDM);
                break;
            case R.id.main_button_player:
                Intent intentPlayer = new Intent(MainActivity.this, PlayerMenuActivity.class);
                startActivity(intentPlayer);
                break;
        }
    }
}
