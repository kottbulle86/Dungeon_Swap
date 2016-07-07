package se.andreasmikaelsson.dungeonswap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class DMMenuActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dm_menu);

        Button buttonStartCombat = (Button) findViewById(R.id.dm_button_start_combat);
        buttonStartCombat.setOnClickListener(this);
        Button buttonStartTimeChallenge = (Button) findViewById(R.id.dm_button_create_time_challenge);
        buttonStartTimeChallenge.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.dm_button_start_combat:
                Intent intentDM1 = new Intent(DMMenuActivity.this, DMCombatActivity.class);
                startActivity(intentDM1);
                break;
            case R.id.dm_button_create_time_challenge:
                Intent intentDM2 = new Intent(DMMenuActivity.this, DMTimeChallengeActivity.class);
                startActivity(intentDM2);
                break;
        }
    }
}