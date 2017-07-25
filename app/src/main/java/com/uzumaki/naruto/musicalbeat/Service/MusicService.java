package com.uzumaki.naruto.musicalbeat.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.uzumaki.naruto.musicalbeat.Model.SongInfo;
import com.uzumaki.naruto.musicalbeat.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Gaurav Bist on 15-07-2017.
 */

public class MusicService extends Service implements MediaPlayer.OnPreparedListener
        , MediaPlayer.OnErrorListener
        , MediaPlayer.OnCompletionListener {

    private static final String TAG = "MusicService";
    public static final String BROADCAST_STATE = "BROADCAST_ACTION";
    public static final String SEEKBAR_STATE = "BROADCAST_SEEK";
    private Thread myThread;
    private static boolean isThreadRunning = false;

    private Intent seekIntent;

    private MediaPlayer mediaPlayer;
    private Uri mSongUri;

    private ArrayList<SongInfo> mListSongs;
    private int SONG_POS = 0;

    // binder gets initialised whenever service is loaded into memory
    private final IBinder musicBind = new PlayerBinder();

    // intent-actions defined for the service
    private final String MAIN_STOP = "MainActivity.STOP";
    private final String MAIN_NEXT = "MainActivity.NEXT";
    private final String MAIN_PREVIOUS = "MainActivity.PREVIOUS";
    private final String MAIN_PAUSE = "MainActivity.PAUSE";

    private final String MUSIC_STOP = "MusicFull.STOP";
    private final String MUSIC_NEXT = "MusicFull.NEXT";
    private final String MUSIC_PREVIOUS = "MusicFull.PREVIOUS";
    private final String MUSIC_PAUSE = "MusicFull.PAUSE";

    private final String ACTION_THREAD_START = "com.uzumaki.naruto.musicalbeat.THREAD_START";
    private final String ACTION_THREAD_STOP = "com.uzumaki.naruto.musicalbeat.NOTIFY_STOP";

    // music app state
    private final static int STATE_PAUSED = 1;
    private final static int STATE_PLAYING = 2;
    private int mState = 0;

    // broadcast sending values
    private static final int BROADCAST_PREV_EVENT = 103;
    private static final int BROADCAST_NEXT_EVENT = 104;

    // unique pending-intent values for different actions
    private final static int REQUEST_CODE_PAUSE = 101;
    private final static int REQUEST_CODE_PREVIOUS = 102;
    private final static int REQUEST_CODE_NEXT = 103;
    private final static int REQUEST_CODE_STOP = 104;


    public static int NOTIFICATION_ID = 11;
    private Notification.Builder notificationBuilder;
    private Notification mNotification;

    /**
     * This binder class is required to keep on passing the current service state to our activity for further functionality
     */
    public class PlayerBinder extends Binder {

        public MusicService getService() {
            Log.d(TAG, "getService: ");
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: called");
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();

        mediaPlayer.release();

        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    /**
     * Whenever the service is created for the first time
     * This method is called once, so it is used to instantiate things
     * which are to be instantiated for one time only.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: MusicService is created.");

        // get media player object
        mediaPlayer = new MediaPlayer();
        initPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);

        notificationBuilder = new Notification.Builder(getApplicationContext());

        seekIntent = new Intent();
        seekIntent.setAction(SEEKBAR_STATE);
    }

    private void initPlayer() {
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);   // dont close service when phone locked
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);  // we choose this to play song
    }

    /**
     * After creation, this method is called multiple times, so do things here which
     * are needed everytime the client request something from service.
     * This method is called everytime a client starts service using startService(intent).
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: This method is called via PendingIntent.");

        if (intent != null) {
            Log.d(TAG, "onStartCommand: The current action is : " + intent.getAction());
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {

                if (action.equals(MAIN_PAUSE)) {
                    playPauseSong();
                } else if (action.equals(MUSIC_PAUSE)) {
                    if (mState == STATE_PLAYING) {
                        isThreadRunning = false;
                        try {
                            myThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        myThread = null;
                    } else {
                        isThreadRunning = true;
                        myThread = null;
                    }
                    playPauseSong();
                } else if (action.equals(MAIN_PREVIOUS)) {
                    previousSong();
                } else if (action.equals(MUSIC_PREVIOUS)) {
                    if (isThreadRunning) {
                        isThreadRunning = false;
                        try {
                            myThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        myThread = null;
                        isThreadRunning = true;
                    }
                    previousSong();
                } else if (action.equals(MAIN_NEXT)) {
                    nextSong();
                } else if (action.equals(MUSIC_NEXT)) {
                    if (isThreadRunning) {
                        isThreadRunning = false;
                        try {
                            myThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        myThread = null;
                        isThreadRunning = true;
                    }
                    nextSong();
                } else if (action.equals(MAIN_STOP)) {
                    isThreadRunning = false;
                    stopSong();
                    stopSelf();
                } else if (action.equals(ACTION_THREAD_START)) {
                    if (mState == STATE_PLAYING) {
                        isThreadRunning = true;
                        myThread = new MyThread();
                        myThread.start();
                    } else {
                        isThreadRunning = false;
                    }
                } else if (action.equals(ACTION_THREAD_STOP)) {
                    isThreadRunning = false;
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void playPauseSong() {
        if (mState == STATE_PAUSED) {
            mState = STATE_PLAYING;
            mediaPlayer.start();

            if(isThreadRunning) {
                myThread = new MyThread();
                myThread.start();
            }
        } else if (mState == STATE_PLAYING) {
            mState = STATE_PAUSED;
            mediaPlayer.pause();
        } else {
			// if initially stopped then change to playing
            mState = STATE_PLAYING;
            startSong(mListSongs.get(SONG_POS).getUri(), mListSongs.get(SONG_POS).getSongName());
        }

        // notify the activities
        Intent intent = new Intent();
        intent.setAction(BROADCAST_STATE);
        intent.putExtra("event", mState);
        sendBroadcast(intent);
    }

    private void previousSong() {
        if (SONG_POS == 0) {
            SONG_POS = mListSongs.size() - 1;
        } else
            SONG_POS -= 1;
        startSong(mListSongs.get(SONG_POS).getUri(), mListSongs.get(SONG_POS).getSongName());

        // notify the activities
        Intent intent = new Intent();
        intent.setAction(BROADCAST_STATE);
        intent.putExtra("event", BROADCAST_PREV_EVENT);
        sendBroadcast(intent);
    }

    private void nextSong() {
        if (SONG_POS == mListSongs.size() - 1) {
            SONG_POS = 0;
        } else
            SONG_POS += 1;
        startSong(mListSongs.get(SONG_POS).getUri(), mListSongs.get(SONG_POS).getSongName());

        // notify the activities
        Intent intent = new Intent();
        intent.setAction(BROADCAST_STATE);
        intent.putExtra("event", BROADCAST_NEXT_EVENT);
        sendBroadcast(intent);
    }

    private void stopSong() {
        mediaPlayer.stop();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ID);

        // notify the activities
        Intent intent = new Intent();
        intent.setAction(BROADCAST_STATE);
        intent.putExtra("event", 0);
        sendBroadcast(intent);
    }

    public void setSongList(ArrayList<SongInfo> songInfos) {
        mListSongs = songInfos;
    }

    private void setSongURI(Uri url) {
        this.mSongUri = url;
    }

    // this function is directly called whenever a song is selected
    public void setSelectedSong(int pos, int notificationId) {
        SONG_POS = pos;
        NOTIFICATION_ID = notificationId;
        setSongURI(mListSongs.get(SONG_POS).getUri());
        showNotification();
        startSong(mListSongs.get(SONG_POS).getUri(), mListSongs.get(SONG_POS).getSongName());
    }

    /**
     * This thread is created to update the seekbar in MusicFull activity
     */
    private class MyThread extends Thread {
        @Override
        public void run() {
            while (isThreadRunning) {
                try {
                    if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration())
                        isThreadRunning = false;

                    Log.d(TAG, "MyThread_run: isThreadRunning = " + isThreadRunning);

                    Thread.sleep(1000);

                    seekIntent.putExtra("current_position", mediaPlayer.getCurrentPosition());
                    seekIntent.putExtra("maximumDuration", mediaPlayer.getDuration());
                    sendBroadcast(seekIntent);
                } catch (Exception e) {
                    Log.d(TAG, "run: There was some issue in Thread running.\n" + e.getMessage());
                    isThreadRunning = false;
                }
            }
        }
    }

    public void startSong(Uri songUri, String songName) {
        mediaPlayer.reset();

        mSongUri = songUri;
        try {
            mediaPlayer.setDataSource(getApplicationContext(), mSongUri);
        } catch (IOException e) {
            Log.d(TAG, "MUSIC SERVICE: Error setting music resource " + e.getMessage());
        }

        mediaPlayer.prepareAsync();
        updateNotification(songName);
    }

    private void showNotification() {
        PendingIntent pendingIntent;
        Intent intent;

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.notification_media_controller);
        notificationView.setTextViewText(R.id.notification_song_title, mListSongs.get(SONG_POS).getSongName());

        Log.d(TAG, "showNotification: Setting Pending Intent ACTION_STOP");
        intent = new Intent(MAIN_STOP);
        pendingIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notify_btn_stop, pendingIntent);

        Log.d(TAG, "showNotification: Setting Pending Intent ACTION_PAUSE");
        intent = new Intent(MAIN_PAUSE);
        pendingIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE_PAUSE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.btn_pause, pendingIntent);

        Log.d(TAG, "showNotification: Setting Pending Intent ACTION_PREVIOUS");
        intent = new Intent(MAIN_PREVIOUS);
        pendingIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE_PREVIOUS, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notify_btn_previous, pendingIntent);

        Log.d(TAG, "showNotification: Setting Pending Intent ACTION_NEXT");
        intent = new Intent(MAIN_NEXT);
        pendingIntent = PendingIntent.getService(getApplicationContext(), REQUEST_CODE_NEXT, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notify_btn_next, pendingIntent);

        mNotification = notificationBuilder
                .setSmallIcon(R.drawable.ic_headset).setOngoing(true)   // setOngoing() is set so swiping won't finish our notification
                .setWhen(System.currentTimeMillis())    				// when to fire up the notification
                .setContent(notificationView)           				// set the custom view
                .setDefaults(Notification.FLAG_NO_CLEAR)
                .build();

        notificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    private void updateNotification(String songName) {
        Log.d(TAG, "updateNotification: Updating the notification bar.");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            mNotification.contentView.setTextViewText(R.id.notification_song_title, songName);
        } catch (NullPointerException e) {
            showNotification();
        }
        notificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "startSong: Mediaplayer synced and ready");
        if (mState == STATE_PAUSED) {

        } else {
            Log.d(TAG, "onPrepared: Song played bcoz state is " + mState);
            mState = STATE_PLAYING;
            mediaPlayer.start();

            if (mediaPlayer.isPlaying()) {
                if (isThreadRunning) {
                    myThread = new MyThread();
                    myThread.start();
                }
            }

            // notify the activies
            Intent intent = new Intent();
            intent.setAction(BROADCAST_STATE);
            intent.putExtra("event", mState);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.reset();
        try {
            if (SONG_POS != mListSongs.size() - 1) {
                SONG_POS++;
            } else {
                SONG_POS = 0;
            }
            mediaPlayer.setDataSource(getApplicationContext(), mListSongs.get(SONG_POS).getUri());

            // notify the activity
            Intent intent = new Intent();
            intent.setAction(BROADCAST_STATE);
            intent.putExtra("event", BROADCAST_NEXT_EVENT);
            sendBroadcast(intent);

            // update notification bar for text change
            updateNotification(mListSongs.get(SONG_POS).getSongName());
        } catch (Exception e) {

        }
        mediaPlayer.prepareAsync();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        Log.d(TAG, "onError: There was some error with " + mediaPlayer);
        return false;
    }
}
