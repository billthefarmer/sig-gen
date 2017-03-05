////////////////////////////////////////////////////////////////////////////////
//
//  Signal generator - An Android Signal generator written in Java.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//  Bill Farmer	 william j farmer [at] yahoo [dot] co [dot] uk.
//
///////////////////////////////////////////////////////////////////////////////

package org.billthefarmer.siggen;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class Main extends Activity
    implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
               View.OnClickListener
{
    public static final String EXACT = "exact";

    private static final int DELAY = 10;
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 1000;

    private static final String TAG = "SigGen";

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String WAVE = "wave";
    private static final String MUTE = "mute";
    private static final String FINE = "fine";
    private static final String LEVEL = "level";
    private static final String SLEEP = "sleep";

    private static final String PREF_BUTTONS = "pref_buttons";

    private Audio audio;

    private Knob knob;
    private Scale scale;
    private Display display;

    private SeekBar fine;
    private SeekBar level;

    private PowerManager.WakeLock wakeLock;

    private boolean sleep;
    private boolean buttons = true;

    // On create
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get views
        display = (Display) findViewById(R.id.display);
        scale = (Scale) findViewById(R.id.scale);
        knob = (Knob) findViewById(R.id.knob);

        fine = (SeekBar) findViewById(R.id.fine);
        level = (SeekBar) findViewById(R.id.level);

        // Get wake lock
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        // Audio
        audio = new Audio();

        if (audio != null)
            audio.start();

        // Setup widgets
        setupWidgets();

        // Restore state
        if (savedInstanceState != null)
            restoreState(savedInstanceState);
    }

    // Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it
        // is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem sleepItem = menu.findItem(R.id.sleep);

        if (sleep)
            sleepItem.setIcon(R.drawable.ic_action_brightness_high);

        return true;
    }

    // On Resume
    @Override
    protected void onResume()
    {
        super.onResume();

        // Get preferences
        getPreferences();

        // Check overlap
        if (buttons)
            checkOverlap();
    }

    // Restore state
    private void restoreState(Bundle savedInstanceState)
    {
        // Get saved state bundle
        Bundle bundle = savedInstanceState.getBundle(STATE);

        // Knob
        if (knob != null)
            knob.setValue(bundle.getFloat(KNOB, 400));

        // Waveform
        int waveform = bundle.getInt(WAVE, Audio.SINE);

        // Waveform buttons
        View v = null;
        switch(waveform)
        {
        case Audio.SINE:
            v = findViewById(R.id.sine);
            break;

        case Audio.SQUARE:
            v = findViewById(R.id.square);
            break;

        case Audio.SAWTOOTH:
            v = findViewById(R.id.sawtooth);
            break;
        }

        onClick(v);

        // Mute
        boolean mute = bundle.getBoolean(MUTE, false);

        if (mute)
        {
            v = findViewById(R.id.mute);
            onClick(v);
        }

        // Fine frequency and level
        fine.setProgress(bundle.getInt(FINE, MAX_FINE / 2));
        level.setProgress(bundle.getInt(LEVEL, MAX_LEVEL / 10));

        // Sleep
        sleep = bundle.getBoolean(SLEEP, false);

        if (sleep)
            wakeLock.acquire();
    }

    // Save state
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        // State bundle
        Bundle bundle = new Bundle();

        // Knob
        bundle.putFloat(KNOB, knob.getValue());

        // Waveform
        bundle.putInt(WAVE, audio.waveform);

        // Mute
        bundle.putBoolean(MUTE, audio.mute);

        // Fine
        bundle.putInt(FINE, fine.getProgress());

        // Level
        bundle.putInt(LEVEL, level.getProgress());

        // Sleep
        bundle.putBoolean(SLEEP, sleep);

        // Save bundle
        outState.putBundle(STATE, bundle);
    }

    // On destroy
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (sleep)
            wakeLock.release();

        if (audio != null)
            audio.stop();
    }

    // On options item
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        // Settings
        case R.id.settings:
            return onSettingsClick(item);

        // Sleep
        case R.id.sleep:
            return onSleepClick(item);

        // Exact
        case R.id.exact:
            return onExactClick();

        default:
            return false;
        }
    }

    // On settings click
    private boolean onSettingsClick(MenuItem item)
    {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        return true;
    }

    // On sleep click
    private boolean onSleepClick(MenuItem item)
    {
        sleep = !sleep;

        if (sleep)
        {
            wakeLock.acquire();
            item.setIcon(R.drawable.ic_action_brightness_high);
        }

        else
        {
            wakeLock.release();
            item.setIcon(R.drawable.ic_action_brightness_low);
        }

        return true;
    }

    // On exact click
    private boolean onExactClick()
    {
        Intent intent = new Intent(this, Input.class);
        startActivityForResult(intent, 0);

        return true;
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {
        // Check result code
       if (resultCode == RESULT_OK)
        {
            // Get the result
            String result = data.getStringExtra(EXACT);
            float exact = Float.parseFloat(result);

            // Ignore if out of range
            if (exact < 10 || exact > 25000)
                return;

            // Calculate knob value
            float value = (float)Math.log10(exact / 10.0) * 200;

            // Set knob value
            if (knob != null)
                knob.setValue(value);
        }
    }

    // On knob change
    @Override
    public void onKnobChange(Knob knob, float value)
    {
        // Scale
        if (scale != null)
            scale.setValue((int)(-value * 2.5));

        // Frequency
        double frequency = Math.pow(10.0, value / 200.0) * 10.0;
        double adjust = ((fine.getProgress() - MAX_FINE / 2) /
                         (double)MAX_FINE) / 100.0;

        frequency += frequency * adjust;

        // Display
        if (display != null)
            display.setFrequency(frequency);

        if (audio != null)
            audio.frequency = frequency;
    }

    // On progress changed
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser)
    {
        int id = seekBar.getId();

        if (audio == null)
            return;

        // Check id
        switch (id)
        {
        // Fine
        case R.id.fine:
        {
            double frequency = Math.pow(10.0, knob.getValue() /
                                        200.0) * 10.0;
            double adjust = ((progress - MAX_FINE / 2) /
                             (double)MAX_FINE) / 50.0;

            frequency += frequency * adjust;

            if (display != null)
                display.setFrequency(frequency);

            if (audio != null)
                audio.frequency = frequency;
        }
        break;

        // Level
        case R.id.level:
            if (display != null)
            {
                double level = Math.log10(progress / (double)MAX_LEVEL) * 20.0;

                if (level < -80.0)
                    level = -80.0;

                display.setLevel(level);
            }

            if (audio != null)
                audio.level = progress / (double)MAX_LEVEL;
            break;
        }
    }

    // On click
    @Override
    public void onClick(View v)
    {
        // Check id
        int id = v.getId();
        switch(id)
        {
        // Sine
        case R.id.sine:
            if (audio != null)
                audio.waveform = Audio.SINE;
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.square);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.sawtooth);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Square
        case R.id.square:
            if (audio != null)
                audio.waveform = Audio.SQUARE;
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.sine);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.sawtooth);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Sawtooth
        case R.id.sawtooth:
            if (audio != null)
                audio.waveform = Audio.SAWTOOTH;
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.sine);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.square);
            ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Mute
        case R.id.mute:
            if (audio != null)
                audio.mute = !audio.mute;

            if (audio.mute)
                ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.checkbox_on_background, 0, 0, 0);

            else
                ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.checkbox_off_background, 0, 0, 0);
            break;

        // Lower
        case R.id.lower:
            if (fine != null)
            {
                int progress = fine.getProgress();
                fine.setProgress(--progress);
            }
            break;

        // Higher
        case R.id.higher:
            {
                int progress = fine.getProgress();
                fine.setProgress(++progress);
            }
            break;
        }
    }

    // checkOverlap
    private void checkOverlap()
    {
        // Get preferences
        final SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        // Check overlap after delay
        display.postDelayed(new Runnable()
            {
                public void run()
                {
                    View mute = findViewById(R.id.mute);
                    View lower = findViewById(R.id.lower);
                    View higher = findViewById(R.id.higher);

                    // Check for overlap
                    if (mute != null && lower != null && higher != null &&
                        (isViewOverlapping(mute, lower) ||
                         isViewOverlapping(mute, higher)))
                    {
                        // Remove buttons
                        lower.setVisibility(View.GONE);
                        higher.setVisibility(View.GONE);

                        // Set preference
                        SharedPreferences.Editor edit = preferences.edit();
                        edit.putBoolean(PREF_BUTTONS, false);
                        edit.apply();
                    }
                }
            }, DELAY);
    }

    // isViewOverlapping
    private boolean isViewOverlapping(View firstView, View secondView)
    {
        int firstPos[] = new int[2];
        int secondPos[] = new int[2];

        firstView.getLocationOnScreen(firstPos);
        secondView.getLocationOnScreen(secondPos);

        // Rect constructor parameters: left, top, right, bottom
        Rect rectFirstView = new Rect(firstPos[0], firstPos[1],
                                      firstPos[0] + firstView.getWidth(),
                                      firstPos[1] + firstView.getHeight());
        Rect rectSecondView = new Rect(secondPos[0], secondPos[1],
                                       secondPos[0] + secondView.getWidth(),
                                       secondPos[1] + secondView.getHeight());
        return rectFirstView.intersect(rectSecondView);
    }

    // Get preferences
    private void getPreferences()
    {
        // Load preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        buttons = preferences.getBoolean(PREF_BUTTONS, true);

        View v;

        if (buttons)
        {
            v = findViewById(R.id.lower);
            if (v != null)
                v.setVisibility(View.VISIBLE);

            v = findViewById(R.id.higher);
            if (v != null)
                v.setVisibility(View.VISIBLE);
        }

        else
        {
            v = findViewById(R.id.lower);
            if (v != null)
                v.setVisibility(View.GONE);

            v = findViewById(R.id.higher);
            if (v != null)
                v.setVisibility(View.GONE);
        }
    }

    // Set up widgets
    private void setupWidgets()
    {
        View v;

        if (knob != null)
        {
            knob.setOnKnobChangeListener(this);
            knob.setValue(400);

            v = findViewById(R.id.previous);
            if (v != null)
                v.setOnClickListener(knob);

            v = findViewById(R.id.next);
            if (v != null)
                v.setOnClickListener(knob);
        }

        v = findViewById(R.id.lower);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.higher);
        if (v != null)
            v.setOnClickListener(this);

        if (fine != null)
        {
            fine.setOnSeekBarChangeListener(this);

            fine.setMax(MAX_FINE);
            fine.setProgress(MAX_FINE / 2);
        }

        if (level != null)
        {
            level.setOnSeekBarChangeListener(this);

            level.setMax(MAX_LEVEL);
            level.setProgress(MAX_LEVEL / 10);
        }

        v = findViewById(R.id.sine);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.square);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.sawtooth);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.mute);
        if (v != null)
            v.setOnClickListener(this);
    }

    // A collection of unused unwanted unloved listener callback methods
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    // Audio
    protected class Audio implements Runnable
    {
        protected static final int SINE = 0;
        protected static final int SQUARE = 1;
        protected static final int SAWTOOTH = 2;

        protected int waveform;
        protected boolean mute;

        protected double frequency;
        protected double level;

        protected Thread thread;

        private AudioTrack audioTrack;

        protected Audio()
        {
            frequency = 440.0;
            level = 16384;
        }

        // Start
        protected void start()
        {
            thread = new Thread(this, "Audio");
            thread.start();
        }

        // Stop
        protected void stop()
        {
            Thread t = thread;
            thread = null;

            // Wait for the thread to exit
            while (t != null && t.isAlive())
                Thread.yield();
        }

        public void run()
        {
            processAudio();
        }

        // Process audio
        protected void processAudio()
        {
            short buffer[];

            int rate =
                AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            int minSize =
                AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO,
                                            AudioFormat.ENCODING_PCM_16BIT);

            // Find a suitable buffer size
            int sizes[] = {1024, 2048, 4096, 8192, 16384, 32768};
            int size = 0;

            for (int s : sizes)
            {
                if (s > minSize)
                {
                    size = s;
                    break;
                }
            }

            final double K = 2.0 * Math.PI / rate;

            // Create the audio track
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
                                        AudioFormat.CHANNEL_OUT_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        size, AudioTrack.MODE_STREAM);
            // Check audiotrack
            if (audioTrack == null)
                return;

            // Check state
            int state = audioTrack.getState();

            if (state != AudioTrack.STATE_INITIALIZED)
            {
                audioTrack.release();
                return;
            }

            audioTrack.play();

            // Create the buffer
            buffer = new short[size];

            // Initialise the generator variables
            double f = frequency;
            double l = 0.0;
            double q = 0.0;

            while (thread != null)
            {
                // Fill the current buffer
                for (int i = 0; i < buffer.length; i++)
                {
                    f += (frequency - f) / 4096.0;
                    l += ((mute ? 0.0 : level) * 16384.0 - l) / 4096.0;
                    q += (q < Math.PI) ? f * K : (f * K) - (2.0 * Math.PI);

                    switch (waveform)
                    {
                    case SINE:
                        buffer[i] = (short) Math.round(Math.sin(q) * l);
                        break;

                    case SQUARE:
                        buffer[i] = (short) ((q > 0.0) ? l : -l);
                        break;

                    case SAWTOOTH:
                        buffer[i] = (short) Math.round((q / Math.PI) * l);
                        break;
                    }
                }

                audioTrack.write(buffer, 0, buffer.length);
            }

            audioTrack.stop();
            audioTrack.release();
        }
    }
}
