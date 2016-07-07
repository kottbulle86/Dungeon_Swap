package se.andreasmikaelsson.dungeonswap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class PlayerMenuActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_menu);

        Button buttonStartCombat = (Button) findViewById(R.id.player_button_start_combat);
        buttonStartCombat.setOnClickListener(this);
        Button buttonStartTimeChallenge = (Button) findViewById(R.id.player_menu_button_start_time_challenge);
        buttonStartTimeChallenge.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_button_start_combat:
                Intent intentPlayer1 = new Intent(PlayerMenuActivity.this, PlayerCombatActivity.class);
                startActivity(intentPlayer1);
                break;
            case R.id.player_menu_button_start_time_challenge:
                Intent intentPlayer2 = new Intent(PlayerMenuActivity.this, PlayerTimeChallengeActivity.class);
                startActivity(intentPlayer2);
                break;
        }
    }
}