package com.easytimelog.timekeeper.controllers;

import android.media.MediaPlayer;
import android.os.Handler;

import java.io.IOException;
import java.util.HashSet;

public class AudioController implements MediaPlayer.OnCompletionListener {
    private static AudioController _AudioController;
    public static AudioController getController() {
        return (_AudioController != null) ? _AudioController : (_AudioController = new AudioController());
    }

    private static final String AVAILABLE = "";
    private String mCurrentlyPlaying;
    private MediaPlayer mMediaPlayer;
    private HashSet<UpdateListener> mListeners;
    private ProgressUpdateTask mProgressUpdateTask = new ProgressUpdateTask();

    public AudioController() {
        mCurrentlyPlaying = AVAILABLE;
        mListeners = new HashSet<UpdateListener>();
        mMediaPlayer = new MediaPlayer();
    }

    public boolean isPlaying(String file) {
        return file != null && file.equals(mCurrentlyPlaying) && mMediaPlayer.isPlaying();
    }

    public void play(String file) {
        if(file == null) { return; }
        if(file.equals(mCurrentlyPlaying)) {
            if(!mMediaPlayer.isPlaying()) { play(); }
        }else {
            String oldFile = mCurrentlyPlaying;
            if(!AVAILABLE.equals(mCurrentlyPlaying)) { stop(mCurrentlyPlaying); }
            setupMediaPlayer(file);
            play();
            updateListeners(MEDIA_CHANGED, oldFile);
        }
    }
    private void play() {
        mMediaPlayer.start();
        mProgressUpdateTask.start();
    }

    public void pause(String file) {
        if(isPlaying(file)) { pause(); }
    }
    private void pause() {
        mMediaPlayer.pause();
        mProgressUpdateTask.stop();
    }

    public void stop(String file) {
        if(file != null && file.equals(mCurrentlyPlaying)) { stop(); }
    }
    private void stop() {
        mProgressUpdateTask.stop();
        mMediaPlayer.stop();
        // prepare media player to play another file
        mCurrentlyPlaying = AVAILABLE;
        mMediaPlayer.reset();
    }

    private void setupMediaPlayer(String file) {
        mCurrentlyPlaying = file;
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(file);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.prepare();
        }catch(IllegalArgumentException e) { e.printStackTrace();
        }catch(IllegalStateException e) { e.printStackTrace();
        }catch(IOException e) { e.printStackTrace(); }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stop();
        updateListeners(MEDIA_COMPLETED);
    }

    private static final int MEDIA_PROGRESS = 0;
    private static final int MEDIA_CHANGED = 1;
    private static final int MEDIA_COMPLETED = 2;
    private void updateListeners(int eventType) { updateListeners(eventType, null); }
    private void updateListeners(int eventType, Object data) {
        // todo - implement
        switch(eventType) {
            case MEDIA_PROGRESS:
                int current = mMediaPlayer.getCurrentPosition()
                  , duration = mMediaPlayer.getDuration();
                for(UpdateListener listener:mListeners) { listener.onProgress(current, duration); }
                break;
            case MEDIA_CHANGED:
                for(UpdateListener listener:mListeners) { listener.onChanged((String)data, mCurrentlyPlaying); }
                break;
            case MEDIA_COMPLETED:
                for(UpdateListener listener:mListeners) { listener.onComplete(); }
                break;
        }
    }

    public void addUpdateListener   (UpdateListener listener) { mListeners.add(listener); }
    public void removeUpdateListener(UpdateListener listener) { mListeners.remove(listener); }
    public interface UpdateListener {
        public void onProgress(int currentTime, int duration);
        public void onChanged(String previouslyPlaying, String nowPlaying);
        public void onComplete();
    }

    private class ProgressUpdateTask implements Runnable {
        private static final long UPDATE_PERIOD = 100;
        private Handler mHandler = new Handler();
        private boolean running = false, _running = false;
        public void start() {
            if(!running)  { running = true; }
            synchronized (this) {
                if (!_running) {
                    _running = true;
                    mHandler.postDelayed(this, UPDATE_PERIOD);
                }
            }
        }
        public void stop() {
            running = false;
        }
        @Override
        public void run() {
            synchronized (this) {
                if (running) {
                    updateListeners(MEDIA_PROGRESS);
                    mHandler.postDelayed(this, UPDATE_PERIOD);
                } else {
                    _running = false;
                }
            }
        }
    }
}
