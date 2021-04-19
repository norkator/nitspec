package com.nitramite.nitspec;

import android.content.Context;
import android.media.MediaPlayer;

@SuppressWarnings("WeakerAccess")
class AudioPlayer {

    // Variables
    private MediaPlayer mMediaPlayer;
    private Boolean ENABLE_SOUNDS = true;

    @SuppressWarnings("SameParameterValue")
    private void play(Context c, int rid, float volume) {
        //stop();
        mMediaPlayer = MediaPlayer.create(c, rid);
        mMediaPlayer.setOnCompletionListener(mediaPlayer -> stop());
        mMediaPlayer.setVolume(volume, volume);
        mMediaPlayer.start();
    }

    // Play sound method
    void playSound(Context context, Integer soundResourceId) {
        if (ENABLE_SOUNDS) {
            play(context, soundResourceId, 100);
        }
    }

    // Stop method
    void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


} // End of class