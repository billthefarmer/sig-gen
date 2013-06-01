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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.app.Activity;
import android.content.res.Resources;
import android.view.Menu;
import android.widget.Button;
import android.widget.SeekBar;
import android.view.View;

public class Main extends Activity
    implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
	       View.OnClickListener
{
    private static final int MAX_LEVEL = 100;
    private static final int MAX_FINE = 100;

    private Audio audio;

    private Knob knob;
    private Scale scale;
    private Display display;

    private SeekBar fine;
    private SeekBar level;

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

	audio = new Audio();

	if (audio != null)
	    audio.start();

	if (knob != null)
	{
	    knob.setOnKnobChangeListener(this);
	    knob.value = 400;
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

	View v = findViewById(R.id.sine);
	v.setOnClickListener(this);

	v = findViewById(R.id.square);
	v.setOnClickListener(this);

	v = findViewById(R.id.sawtooth);
	v.setOnClickListener(this);

	v = findViewById(R.id.mute);
	v.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
	// Inflate the menu; this adds items to the action bar if it is present.
	return true;
    }

    @Override
    protected void onDestroy()
    {
	super.onDestroy();

	if (audio != null)
	    audio.stop();
    }
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

    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {		
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {		
    }

    @Override
    public void onClick(View v)
    {
	Resources resources = getResources();
	Bitmap off =
	    BitmapFactory.decodeResource(resources,
					 android.R.drawable.radiobutton_off_background);
	Bitmap on =
	    BitmapFactory.decodeResource(resources,
					 android.R.drawable.radiobutton_on_background);
	BitmapDrawable drawOff = new BitmapDrawable(resources, off);
	BitmapDrawable drawOn = new BitmapDrawable(resources, on);
	drawOff.setBounds(0, 0, off.getWidth(), off.getHeight());
	drawOn.setBounds(0, 0, on.getWidth(), on.getHeight());

	int id = v.getId();
	switch(id)
	{
	case R.id.sine:
	    if (audio != null)
		audio.waveform = Audio.SINE;
	    ((Button)v).setCompoundDrawables(drawOn, null, null, null);

	    v = findViewById(R.id.square);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    v = findViewById(R.id.sawtooth);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    break;

	case R.id.square:
	    if (audio != null)
		audio.waveform = Audio.SQUARE;
	    ((Button)v).setCompoundDrawables(drawOn, null, null, null);

	    v = findViewById(R.id.sine);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    v = findViewById(R.id.sawtooth);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    break;

	case R.id.sawtooth:
	    if (audio != null)
		audio.waveform = Audio.SAWTOOTH;
	    ((Button)v).setCompoundDrawables(drawOn, null, null, null);

	    v = findViewById(R.id.sine);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    v = findViewById(R.id.square);
	    ((Button)v).setCompoundDrawables(drawOff, null, null, null);
	    break;

	case R.id.mute:
	    if (audio != null)
		audio.mute = !audio.mute;

	    if (audio.mute)
	    {
		Bitmap box =
		    BitmapFactory.decodeResource(resources,
						 android.R.drawable.checkbox_on_background);
		BitmapDrawable drawable = new BitmapDrawable(resources, box);
		drawable.setBounds(0, 0, box.getWidth(), box.getHeight());
		((Button)v).setCompoundDrawables(drawable, null, null, null);
	    }

	    else
	    {
		Bitmap box =
		    BitmapFactory.decodeResource(resources,
						 android.R.drawable.checkbox_off_background);
		BitmapDrawable drawable = new BitmapDrawable(resources, box);
		drawable.setBounds(0, 0, box.getWidth(), box.getHeight());
		((Button)v).setCompoundDrawables(drawable, null, null, null);
	    }
	    break;
	}
    }

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


	protected void processAudio()
	{
	    short buffer[];

	    int rate =
		AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
	    int minSize =
		AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO,
					    AudioFormat.ENCODING_PCM_16BIT);

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

	    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, rate,
					AudioFormat.CHANNEL_OUT_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					size, AudioTrack.MODE_STREAM);
	    audioTrack.play();

	    buffer = new short[size];

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

		    switch (audio.waveform)
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
