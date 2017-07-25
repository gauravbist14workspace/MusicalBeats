package com.uzumaki.naruto.musicalbeat.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.uzumaki.naruto.musicalbeat.Model.SongInfo;
import com.uzumaki.naruto.musicalbeat.R;
import com.uzumaki.naruto.musicalbeat.Service.MusicService;

import java.util.ArrayList;

import static com.uzumaki.naruto.musicalbeat.Service.MusicService.BROADCAST_STATE;
import static com.uzumaki.naruto.musicalbeat.Service.MusicService.SEEKBAR_STATE;

/**
 * Created by Gaurav Bist on 16-07-2017.
 */

public class MusicFull extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MusicFull";
    private Context mContext;

    private ArrayList<SongInfo> songInfos;
    private int songPos = 0;

    // music app state
    private final static int STATE_PAUSED = 1;
    private final static int STATE_PLAYING = 2;
    private int mState = 0;

    Intent playIntent;
    MyReceiver myReceiver;
    private static final int BROADCAST_PREV_EVENT = 103;
    private static final int BROADCAST_NEXT_EVENT = 104;

    ImageView song_img;
    TextView song_name, song_artist_name, song_current_time, song_duration;
    SeekBar seekbar;
    ImageButton btn_previous, btn_pause, btn_next;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_full);

        bindViews();
        getIntentValues();
        init();
    }

    private void bindViews() {
        song_img = (ImageView) findViewById(R.id.song_img);
        song_name = (TextView) findViewById(R.id.song_name);
        song_name.setSelected(true);
        song_artist_name = (TextView) findViewById(R.id.song_artist_name);

        song_current_time = (TextView) findViewById(R.id.song_current_time);
        seekbar = (SeekBar) findViewById(R.id.seekbar);
        song_duration = (TextView) findViewById(R.id.song_duration);

        btn_previous = (ImageButton) findViewById(R.id.btn_previous);
        btn_pause = (ImageButton) findViewById(R.id.btn_pause);
        btn_next = (ImageButton) findViewById(R.id.btn_next);
    }

    private void getIntentValues() {
        mState = getIntent().getIntExtra("mState", 0);
        songPos = getIntent().getIntExtra("songPos", 0);

        changePlayPauseButtonIcon();
    }
	
	private void changePlayPauseButtonIcon() {
        if (mState == STATE_PAUSED) {
            btn_pause.setBackgroundResource(R.drawable.ic_play);
        } else if (mState == STATE_PLAYING) {
            btn_pause.setBackgroundResource(R.drawable.ic_pause);
        } else if (mState == 0) {
			// if already stopped state, change the icon
            // mState = STATE_PAUSED;
            btn_pause.setBackgroundResource(R.drawable.ic_play);
        }
    }

    private void init() {
        mContext = this;
        playIntent = new Intent(this, MusicService.class);

        // retrieving songs list from MainActivity
        songInfos = MainActivity.songInfos;

        song_name.setText(songInfos.get(songPos).getSongName());
        song_artist_name.setText(songInfos.get(songPos).getSongName());
        song_duration.setText(songInfos.get(songPos).getDuration());

        btn_previous.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_next.setOnClickListener(this);

        // Register our activity to the service events
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_STATE);
        intentFilter.addAction(SEEKBAR_STATE);
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_previous:
                playIntent.setAction("MusicFull.PREVIOUS");
                startService(playIntent);

                break;
            case R.id.btn_pause:
                playIntent.setAction("MusicFull.PAUSE");
                startService(playIntent);

                break;
            case R.id.btn_next:
                playIntent.setAction("MusicFull.NEXT");
                startService(playIntent);

                break;
        }
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(BROADCAST_STATE)) {
                //Log.d(TAG, "MusicFull: Broadcast received: " + intent.getIntExtra("event", 0));

                int receivedEvent = intent.getIntExtra("event", 0);
                if (receivedEvent == STATE_PAUSED) {
                    mState = receivedEvent;
                    btn_pause.setBackgroundResource(R.drawable.ic_play);

                } else if (receivedEvent == STATE_PLAYING) {
                    mState = receivedEvent;
                    btn_pause.setBackgroundResource(R.drawable.ic_pause);

                } else if (receivedEvent == BROADCAST_PREV_EVENT) {
                    if (songPos == 0)
                        songPos = songInfos.size() - 1;
                    else
                        songPos--;
                } else if (receivedEvent == BROADCAST_NEXT_EVENT) {
                    if (songPos == songInfos.size() - 1)
                        songPos = 0;
                    else
                        songPos++;
                }
                changeTrackDisplayed();
            } else if (intent.getAction().equals(SEEKBAR_STATE)) {
                //Log.d(TAG, "MusicFull: Seekbar values received: " + intent.getIntExtra("current_position", 0));

                int current = intent.getIntExtra("current_position", 0);
                int maxDuration = intent.getIntExtra("maximumDuration", 0);

//                Double percentage = (double) 0;
//
//                long currentSeconds = (int) (current / 1000);
//                long totalSeconds = (int) (maxDuration / 1000);
//
//                // calculating percentage
//                percentage =(((double)currentSeconds)/totalSeconds)*100;


                song_current_time.setText(getTimeString(current));

                seekbar.setMax(maxDuration);
                seekbar.setProgress(current);
            }
        }
    }

	private void changeTrackDisplayed() {
        Log.d(TAG, "MusicFull_onReceive: Next song at " + songPos + " is " + songInfos.get(songPos).getSongName());

        song_name.setText(songInfos.get(songPos).getSongName());
        song_artist_name.setText(songInfos.get(songPos).getArtistName());
        song_duration.setText(songInfos.get(songPos).getDuration());
    }
	
    private String getTimeString(int milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        startService(playIntent.setAction("com.uzumaki.naruto.musicalbeat.THREAD_STOP"));
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }
}
