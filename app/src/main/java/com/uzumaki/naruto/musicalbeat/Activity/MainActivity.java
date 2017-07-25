package com.uzumaki.naruto.musicalbeat.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.uzumaki.naruto.musicalbeat.Adapter.CustomAdapter;
import com.uzumaki.naruto.musicalbeat.Model.SongInfo;
import com.uzumaki.naruto.musicalbeat.R;
import com.uzumaki.naruto.musicalbeat.Service.MusicService;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.uzumaki.naruto.musicalbeat.Service.MusicService.NOTIFICATION_ID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks {

    private static final String TAG = "MainActivity";
    private Context mContext;

    // activity middle section
    CustomAdapter customAdapter;
    AlertDialog.Builder builder;
    ListView activity_main_list;
    public static ArrayList<SongInfo> songInfos;

    // bottom media controller
    ImageView song_img;
    TextView song_name, song_artist_name;
    ImageButton btn_prev, btn_next, btn_pause;

    private int songPos = 0;
    // music app state
    private final static int STATE_PAUSED = 1;
    private final static int STATE_PLAYING = 2;
    private int mState = 0;

    MusicService musicService;
    Intent playIntent;
    MyReceiver myReceiver;
    private static final int BROADCAST_PREV_EVENT = 103;
    private static final int BROADCAST_NEXT_EVENT = 104;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        init();
    }

    private void bindViews() {
        activity_main_list = (ListView) findViewById(R.id.activity_main_list);

        song_img = (ImageView) findViewById(R.id.song_img);

        song_name = (TextView) findViewById(R.id.song_name);
        song_name.setSelected(true);
        song_artist_name = (TextView) findViewById(R.id.song_artist_name);

        btn_prev = (ImageButton) findViewById(R.id.btn_previous);
        btn_pause = (ImageButton) findViewById(R.id.btn_pause);
        btn_next = (ImageButton) findViewById(R.id.btn_next);
    }

    private void init() {
        mContext = this;

        songInfos = new ArrayList<>();
        getSupportLoaderManager().initLoader(1, null, this);

        setupDialog();

        customAdapter = new CustomAdapter(mContext, songInfos);
        activity_main_list.setAdapter(customAdapter);
        activity_main_list.setOnItemClickListener(this);

        song_img.setOnClickListener(this);
        btn_prev.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_next.setOnClickListener(this);

        checkPermission();
        registerBroadcastReceiver();
    }

    private void setupDialog() {
        builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Choose an option");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_previous:
                playIntent.setAction("MainActivity.PREVIOUS");
                startService(playIntent);

                break;
            case R.id.btn_pause:
                playIntent.setAction("MainActivity.PAUSE");
                startService(playIntent);

                break;
            case R.id.btn_next:
                playIntent.setAction("MainActivity.NEXT");
                startService(playIntent);

                break;

            case R.id.song_img:
                Pair[] pair = new Pair[1];
                pair[0] = new Pair<View, String>(song_img, "song_album_art");
//                pair[1] = new Pair<View, String>(song_name, "song_title");
//                pair[2] = new Pair<View, String>(song_artist_name, "song_artist_name");

                Intent intent = new Intent(mContext, MusicFull.class);
                intent.putExtra("mState", mState);
                intent.putExtra("songPos", songPos);

                startService(playIntent.setAction("com.uzumaki.naruto.musicalbeat.THREAD_START"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation((Activity) mContext, pair);
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
                break;
        }
//        changeTrackDisplayed();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
        Log.d(TAG, "onItemClick: Song selected: " + songInfos.get(pos).getSongName());
        songPos = pos;

        musicService.setSelectedSong(songPos, NOTIFICATION_ID);
        changeTrackDisplayed();
    }
	
	private void changeTrackDisplayed() {
        song_name.setText(songInfos.get(songPos).getSongName());
        song_artist_name.setText(songInfos.get(songPos).getArtistName());
    }

	/*
	* Will add options for each item such as sharing, deleting or renaming the song
	*/
    AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int song_num, long l) {
            builder.setItems(getResources().getStringArray(R.array.options), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int option_num) {
                    switch (option_num){
                        case 0:
                            // delete the song
                            break;
                        case 1:
                            // rename the song
                            File file = new File(songInfos.get(song_num).getSongPath());
                            break;
                        case 2:
                            // share the song
                            break;
                    }
                }
            });
            return false;
        }
    };

    private void changePlayPauseButtonIcon() {
        Log.d(TAG, "changePlayPauseButtonIcon: Current state is " + mState);
        if (mState == STATE_PAUSED) {
            mState = STATE_PLAYING;
            Log.d(TAG, "changePlayPauseButtonIcon: changing to " + mState);
            btn_pause.setBackgroundResource(R.drawable.ic_pause);
        } else if (mState == STATE_PLAYING) {
            mState = STATE_PAUSED;
            Log.d(TAG, "changePlayPauseButtonIcon: changing to " + mState);
            btn_pause.setBackgroundResource(R.drawable.ic_play);
        } else {
            mState = STATE_PLAYING;
            Log.d(TAG, "changePlayPauseButtonIcon: Since new icon is clicked so state changed to " + mState);
            btn_pause.setBackgroundResource(R.drawable.ic_pause);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 123);
                return;
            } else
                getSupportLoaderManager().initLoader(1, null, this);
        } else
            getSupportLoaderManager().initLoader(1, null, this);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        //Uri CONTACT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        String sort_order = MediaStore.Audio.AudioColumns.ARTIST + " DESC, "
                + MediaStore.Audio.AudioColumns.TRACK + " DESC";

        CursorLoader cursorLoader = new CursorLoader(mContext, uri, null,
                selection, null, sort_order);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        Cursor cursor = ((Cursor) data);

        if (cursor == null) {
            // query failed
        } else if (!cursor.moveToFirst()) {
            // no media found
        } else {
            cursor.moveToFirst();
            do {
                SongInfo songInfo = new SongInfo();

                String[] res = cursor.getString(
                        cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME))
                        .split("\\.mp3");

                songInfo.setSongName(res[0]);       // Removal of .mp3 part from song name ....
                songInfo.setArtistName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                songInfo.setUri(ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID))
                ));
                songInfo.setSongPath(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                //songInfo.setDuration(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
                songInfo.setDuration(milliSecondsToTimer(Integer.parseInt(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)))));

                songInfos.add(songInfo);
            } while (cursor.moveToNext());
        }
    }

    // Method no. 1 to get formatted time
    @NonNull
    private String getDuration(long millisec) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millisec);
        millisec -= TimeUnit.MINUTES.toMillis(minutes);
        long sec = TimeUnit.SECONDS.toMinutes(millisec);

        StringBuilder sb = new StringBuilder(5);
        sb.append(minutes < 10 ? "0" + minutes : minutes);
        sb.append(":");
        sb.append((sec < 10 ? "0" + sec : sec).toString().substring(0, 2));

        return sb.toString();
    }

    // Method no. 2 to get formatted time
    public String milliSecondsToTimer(long milliseconds) {
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
    public void onLoaderReset(Loader loader) {

    }

    private void registerBroadcastReceiver() {
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.BROADCAST_STATE);
        registerReceiver(myReceiver, intentFilter);
    }

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.PlayerBinder binder = (MusicService.PlayerBinder) iBinder;
            musicService = binder.getService();    
            musicService.setSongList(songInfos);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            musicService = null;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 123:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getSupportLoaderManager().initLoader(1, null, this);
                } else {
                    Toast.makeText(this, "Permission to access media denied !", Toast.LENGTH_SHORT).show();
                    checkPermission();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, BIND_AUTO_CREATE);
        } else {
            Log.d(TAG, "onStart: PlayIntent is not null. Directing to service connection.");
        }
    }

    @Override
    protected void onDestroy() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);

        startService(playIntent.setAction("MAINACTIVITY.STOP"));

        unregisterReceiver(myReceiver);
        stopService(playIntent);
        unbindService(musicConnection);

        super.onDestroy();
    }

    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "onReceive: Broadcast received: " + intent.getIntExtra("event", 0));
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
        }
    }
}
