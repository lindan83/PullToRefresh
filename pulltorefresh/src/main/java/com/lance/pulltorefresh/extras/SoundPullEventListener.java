/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.lance.pulltorefresh.extras;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;

import com.lance.pulltorefresh.PullToRefreshBase;

import java.util.HashMap;


public class SoundPullEventListener<V extends View> implements PullToRefreshBase.OnPullEventListener<V> {

    private final Context context;
    private final HashMap<PullToRefreshBase.State, Integer> soundMap;

    private MediaPlayer currentMediaPlayer;

    /**
     * Constructor
     *
     * @param context - Context
     */
    public SoundPullEventListener(Context context) {
        this.context = context;
        soundMap = new HashMap<>();
    }

    @Override
    public final void onPullEvent(PullToRefreshBase<V> refreshView, PullToRefreshBase.State event, PullToRefreshBase.Mode direction) {
        Integer soundResIdObj = soundMap.get(event);
        if (null != soundResIdObj) {
            playSound(soundResIdObj);
        }
    }

    /**
     * Set the Sounds to be played when a Pull Event happens. You specify which
     * sound plays for which events by calling this method multiple times for
     * each event.
     * If you've already set a sound for a certain event, and add another sound
     * for that event, only the new sound will be played.
     *
     * @param event - The event for which the sound will be played.
     * @param resId - Resource Id of the sound file to be played (e.g.
     *              <var>R.raw.pull_sound</var>)
     */
    public void addSoundEvent(PullToRefreshBase.State event, int resId) {
        soundMap.put(event, resId);
    }

    /**
     * Clears all of the previously set sounds and events.
     */
    public void clearSounds() {
        soundMap.clear();
    }

    /**
     * Gets the current (or last) MediaPlayer instance.
     *
     * @return MediaPlayer
     */
    public MediaPlayer getCurrentMediaPlayer() {
        return currentMediaPlayer;
    }

    private void playSound(int resId) {
        // Stop current player, if there's one playing
        if (null != currentMediaPlayer) {
            currentMediaPlayer.stop();
            currentMediaPlayer.release();
        }

        currentMediaPlayer = MediaPlayer.create(context, resId);
        if (null != currentMediaPlayer) {
            currentMediaPlayer.start();
        }
    }
}