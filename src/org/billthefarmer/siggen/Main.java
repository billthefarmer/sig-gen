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

import android.app.ActionBar;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class Main extends Activity
    implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
	       View.OnClickListener
{
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 100;

    private static final String TAG = "SigGen";

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String WAVE = "wave";
    private static final String MUTE = "mute";
    private static final String FINE = "fine";
    private static final String LEVEL = "level";
    private static final String SLEEP = "sleep";

    private Audio audio;

    private Knob knob;
    private Scale scale;
    private Display display;

    private ImageView imageView;
    private SeekBar fine;
    private SeekBar level;

    private PowerManager.WakeLock wakeLock;

    private boolean sleep;

    // On create

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);

	display = (Display) findViewById(R.id.display);
	scale = (Scale) findViewById(R.id.scale);
	knob = (Knob) findViewById(R.id.knob);

	fine = (SeekBar) findViewById(R.id.fine);
	level = (SeekBar) findViewById(R.id.level);

	// Add custom view to action bar

	ActionBar actionBar = getActionBar();
	actionBar.setCustomView(R.layout.custom);
	actionBar.setDisplayShowCustomEnabled(true);
	imageView = (ImageView)actionBar.getCustomView();

	if (imageView != null)
	    imageView.setOnClickListener(this);

	// Get wake lock

	PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
	wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
 
	audio = new Audio();

	if (audio != null)
	    audio.start();

	setupWidgets();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it
	// is present.
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
	super.onRestoreInstanceState(savedInstanceState);

	Bundle bundle = savedInstanceState.getBundle(STATE);

	Log.d(TAG, "Restore: " + bundle.toString());

	if (knob != null)
	{
	    knob.value = bundle.getFloat(KNOB, 400);
	    knob.invalidate();
	}

	if (scale != null)
	{
	    scale.value = (int) (-knob.value * 2.5);
	    scale.invalidate();
	}

	if (audio != null)
	{
	    audio.waveform = bundle.getInt(WAVE, Audio.SINE);
	    audio.mute = bundle.getBoolean(MUTE, false);
	}

	fine.setProgress(bundle.getInt(FINE, MAX_FINE / 2));
	level.setProgress(bundle.getInt(LEVEL, MAX_LEVEL / 10));

	sleep = bundle.getBoolean(SLEEP, false);


	double frequency = Math.pow(10.0, knob.value / 200.0) * 10.0;
	double adjust = ((fine.getProgress() - MAX_FINE / 2) /
			 (double)MAX_FINE) / 100.0;

	frequency += frequency * adjust;

	if (display != null)
	{
	    display.frequency = frequency;
	    display.invalidate();
	}

	if (audio != null)
	    audio.frequency = frequency;

	View v = null;
	switch(audio.waveform)
	{
	case Audio.SINE:
	    v = findViewById(R.id.sine);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_on_background, 0, 0, 0);

	    v = findViewById(R.id.square);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    v = findViewById(R.id.sawtooth);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    break;

	case Audio.SQUARE:
	    v = findViewById(R.id.square);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_on_background, 0, 0, 0);

	    v = findViewById(R.id.sine);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    v = findViewById(R.id.sawtooth);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    break;

	case Audio.SAWTOOTH:
	    v = findViewById(R.id.sawtooth);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_on_background, 0, 0, 0);

	    v = findViewById(R.id.sine);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    v = findViewById(R.id.square);
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
		      android.R.drawable.radiobutton_off_background, 0, 0, 0);
	    break;
	}

	v = findViewById(R.id.mute);
	if (audio.mute)
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
			android.R.drawable.checkbox_on_background, 0, 0, 0);

	else
	    ((Button)v).setCompoundDrawablesWithIntrinsicBounds(
			android.R.drawable.checkbox_off_background, 0, 0, 0);

	if (sleep)
	{
	    wakeLock.acquire();
	    imageView.setImageResource(R.drawable.ic_sleep_on);
	}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
	super.onSaveInstanceState(outState);

	Bundle bundle = new Bundle();

	// Knob

	bundle.putFloat(KNOB, knob.value);

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

    // On knob change

    @Override
    public void onKnobChange(Knob knob, float value)
    {
	if (scale != null)
	{
	    scale.value = (int) (-value * 2.5);
	    scale.invalidate();
	}

	double frequency = Math.pow(10.0, value / 200.0) * 10.0;
	double adjust = ((fine.getProgress() - MAX_FINE / 2) /
			 (double)MAX_FINE) / 100.0;

	frequency += frequency * adjust;

	if (display != null)
	{
	    display.frequency = frequency;
	    display.invalidate();
	}

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

	switch (id)
	{
	case R.id.fine:
	    {
		double frequency = Math.pow(10.0, knob.value / 200.0) * 10.0;
		double adjust = ((progress - MAX_FINE / 2) /
				 (double)MAX_FINE) / 50.0;

		frequency += frequency * adjust;

		if (display != null)
		{
		    display.frequency = frequency;
		    display.invalidate();
		}

		if (audio != null)
		    audio.frequency = frequency;
	    }
	    break;

	case R.id.level:
	    if (display != null)
	    {
		display.level = Math.log10(progress / (double)MAX_LEVEL) * 20.0;

		if (display.level < -80.0)
		    display.level = -80.0;

		display.invalidate();
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
	int id = v.getId();
	switch(id)
	{
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

	case R.id.sleep:
	    sleep = !sleep;

	    if (sleep)
	    {
		wakeLock.acquire();
		imageView.setImageResource(R.drawable.ic_sleep_on);
	    }

	    else
	    {
		wakeLock.release();
		imageView.setImageResource(R.drawable.ic_sleep_off);
	    }
	    break;
	}
    }

    // Set up widgets

    private void setupWidgets()
    {
	View v;

	if (knob != null)
	{
	    knob.setOnKnobChangeListener(this);
	    knob.value = 400;

	    v = findViewById(R.id.previous);
	    v.setOnClickListener(knob);

	    v = findViewById(R.id.next);
	    v.setOnClickListener(knob);
	}

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
	v.setOnClickListener(this);

	v = findViewById(R.id.square);
	v.setOnClickListener(this);

	v = findViewById(R.id.sawtooth);
	v.setOnClickListener(this);

	v = findViewById(R.id.mute);
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

	    for (int s: sizes)
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
		    l += ((mute? 0.0 : level) * 16384.0 - l) / 4096.0;
		    q += (q < Math.PI)? f * K: (f * K) - (2.0 * Math.PI);

		    switch (waveform)
		    {
		    case SINE:
			buffer[i] = (short) Math.round(Math.sin(q) * l);
			break;

		    case SQUARE:
			buffer[i] = (short) ((q > 0.0)? l: -l);
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
