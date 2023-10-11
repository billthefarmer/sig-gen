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

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class Main extends Activity
    implements Knob.OnKnobChangeListener, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener, ValueAnimator.AnimatorUpdateListener,
    PopupMenu.OnMenuItemClickListener
{
    public static final String EXACT = "exact";

    private static final int DELAY = 250;
    private static final int MAX_LEVEL = 160;
    private static final int MAX_DUTY = 1000;
    private static final int MAX_FINE = 1000;

    public static final int LIGHT  = 0;
    public static final int DARK   = 1;
    public static final int SYSTEM = 2;

    public static final int VERSION_CODE_S_V2 = 32;

    private static final double MARGIN = 1.0;

    private static final String TAG = "SigGen";
    private static final String LOCK = "SigGen:lock";

    private static final String STATE = "state";

    private static final String KNOB = "knob";
    private static final String WAVE = "wave";
    private static final String MUTE = "mute";
    private static final String DUTY = "duty";
    private static final String FINE = "fine";
    private static final String LEVEL = "level";
    private static final String SLEEP = "sleep";

    public static final String PREF_BOOKMARKS = "pref_bookmarks";
    public static final String PREF_THEME = "pref_theme";
    public static final String PREF_DUTY = "pref_duty";
    public static final String PREF_MUTE = "pref_mute";
    public static final String PREF_FREQ = "pref_freq";
    public static final String PREF_WAVE = "pref_wave";
    public static final String PREF_LEVEL = "pref_level";

    public static final String SET_FREQ = "org.billthefarmer.siggen.SET_FREQ";
    public static final String SET_WAVE = "org.billthefarmer.siggen.SET_WAVE";
    public static final String SET_MUTE = "org.billthefarmer.siggen.SET_MUTE";
    public static final String SET_DUTY = "org.billthefarmer.siggen.SET_DUTY";
    public static final String SET_LEVEL = "org.billthefarmer.siggen.SET_LEVEL";

    private Audio audio;

    private Knob knob;
    private Scale scale;
    private Display display;

    private SeekBar duty;
    private SeekBar fine;
    private SeekBar level;
    private Toolbar toolbar;
    private TextView custom;

    private Toast toast;

    private PowerManager.WakeLock wakeLock;
    private PhoneStateListener phoneListener;
    private List<Double> bookmarks;

    private boolean sleep;
    private int theme;

    // On create
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        theme = Integer.parseInt(preferences.getString(PREF_THEME, "0"));

        Configuration config = getResources().getConfiguration();
        int night = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        switch (theme)
        {
        case LIGHT:
            setTheme(R.style.AppTheme);
            break;

        case DARK:
            setTheme(R.style.AppDarkTheme);
            break;

        case SYSTEM:
            switch (night)
            {
            case Configuration.UI_MODE_NIGHT_NO:
                setTheme(R.style.AppTheme);
                break;

            case Configuration.UI_MODE_NIGHT_YES:
                setTheme(R.style.AppDarkTheme);
                break;
            }
            break;
        }

        setContentView(R.layout.main);

        // Get views
        display = findViewById(R.id.display);
        scale = findViewById(R.id.scale);
        knob = findViewById(R.id.knob);

        duty = findViewById(R.id.duty);
        fine = findViewById(R.id.fine);
        level = findViewById(R.id.level);

        // Get wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK);

        // Set up custom view
        getActionBar().setCustomView(R.layout.custom);
        getActionBar().setDisplayShowCustomEnabled(true);
        custom = (TextView) getActionBar().getCustomView();

        // Find toolbar
        toolbar = findViewById(getResources().getIdentifier("action_bar",
                                                            "id", "android"));
        // Set up navigation
        toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
        toolbar.setNavigationOnClickListener((v) ->
        {
            PopupMenu popup = new PopupMenu(this, v);
            popup.inflate(R.menu.navigation);
            popup.setOnMenuItemClickListener(this);
            popup.show();
        });

        // Audio
        audio = new Audio();

        if (audio != null)
            audio.start();

        // Setup widgets
        setupWidgets();

        // Setup phone state listener
        setupPhoneStateListener();

        // Get preferences
        getPreferences();

        // Restore state
        if (savedInstanceState != null)
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
            switch (waveform)
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

            if (v != null)
                onClick(v);

            // Mute
            boolean mute = bundle.getBoolean(MUTE, false);
            if (mute != audio.mute)
                onClick(findViewById(R.id.mute));

            // Duty, fine frequency and level
            duty.setProgress(bundle.getInt(DUTY, MAX_DUTY / 2));
            fine.setProgress(bundle.getInt(FINE, MAX_FINE / 2));
            level.setProgress(bundle.getInt(LEVEL, MAX_LEVEL * 3 / 4));

            // Sleep
            sleep = bundle.getBoolean(SLEEP, false);

            if (sleep)
                wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }

        // Get intent
        setState(getIntent());
    }

    // onNewIntent
    @Override
    public void onNewIntent(Intent intent)
    {
        setState(intent);
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

        int last = theme;

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        theme = Integer.parseInt(preferences.getString(PREF_THEME, "0"));

        if (last != theme && Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();

        checkButtons();
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

        // Duty
        bundle.putInt(DUTY, duty.getProgress());

        // Fine
        bundle.putInt(FINE, fine.getProgress());

        // Level
        bundle.putInt(LEVEL, level.getProgress());

        // Sleep
        bundle.putBoolean(SLEEP, sleep);

        // Save bundle
        outState.putBundle(STATE, bundle);
    }

    // On pause
    @Override
    protected void onPause()
    {
        super.onPause();

        // Get preferences
        final SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = preferences.edit();

        if (bookmarks != null)
        {
            JSONArray json = new JSONArray(bookmarks);

            // Save preference
            edit.putString(PREF_BOOKMARKS, json.toString());
        }

        edit.putInt(PREF_WAVE, audio.waveform);
        edit.putBoolean(PREF_MUTE, audio.mute);
        edit.remove(PREF_DUTY);
        edit.putInt(PREF_DUTY, duty.getProgress());
        edit.putInt(PREF_LEVEL, level.getProgress());
        edit.putString(PREF_FREQ, Double.toString(audio.frequency));
        edit.apply();
    }

    // On destroy
    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        try
        {
            TelephonyManager manager = (TelephonyManager)
                                       getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
        }

        catch (Exception e) {}

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
        // Sleep
        case R.id.sleep:
            return onSleepClick(item);

        // Bookmark
        case R.id.bookmark:
            return onBookmarkClick();

        // Exact
        case R.id.exact:
            return onExactClick();

        default:
            return false;
        }
    }

    // onMenuItemClick
    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        // Get id
        int id = item.getItemId();
        switch (id)
        {
        // Help
        case R.id.help:
            return onHelpClick(item);

        // Settings
        case R.id.settings:
            return onSettingsClick(item);

        default:
            return false;
        }
    }

    // On help click
    private boolean onHelpClick(MenuItem item)
    {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);

        return true;
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
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            item.setIcon(R.drawable.ic_action_brightness_high);
        }
        else
        {
            wakeLock.release();
            item.setIcon(R.drawable.ic_action_brightness_low);
        }

        return true;
    }

    // On bookmark click
    private boolean onBookmarkClick()
    {
        if (bookmarks == null)
            bookmarks = new ArrayList<>();

        for (double bookmark : bookmarks)
        {
            if (Math.abs(audio.frequency - bookmark) < MARGIN)
            {
                bookmarks.remove(bookmark);
                showToast(R.string.bookmark_removed, bookmark);
                return true;
            }
        }

        bookmarks.add(audio.frequency);
        showToast(R.string.bookmark_added, audio.frequency);
        Collections.sort(bookmarks);
        checkBookmarks();

        return true;
    }

    // On exact click
    private boolean onExactClick()
    {
        // Open dialog
        exactDialog(R.string.frequency, R.string.enter,
                    (dialog, id) ->
        {
            switch (id)
            {
            case DialogInterface.BUTTON_POSITIVE:
                EditText text =
                ((Dialog) dialog).findViewById(R.id.text);
                String result = text.getText().toString();

                // Ignore empty string
                if (result.isEmpty())
                    return;

                float exact = Float.parseFloat(result);

                // Ignore if out of range
                if (exact < 0.1 || exact > 25000)
                    return;

                setFrequency(exact);
            }
        });

        return true;
    }

    // exactDialog
    private void exactDialog(int title, int hint,
                             DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Add the buttons
        builder.setPositiveButton(R.string.ok, listener);
        builder.setNegativeButton(R.string.cancel, listener);

        // Create edit text
        Context context = builder.getContext();
        EditText text = new EditText(context);
        text.setId(R.id.text);
        text.setHint(hint);
        text.setInputType(InputType.TYPE_CLASS_NUMBER |
                          InputType.TYPE_NUMBER_FLAG_DECIMAL);

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setView(text, 40, 0, 40, 0);
        dialog.show();
    }

    // setState
    private void setState(Intent intent)
    {
        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        // Frequency
        if (extras.containsKey(SET_FREQ))
        {
            float value = extras.getFloat(SET_FREQ);
            if (value != 0.0)
                setFrequency(value);

            else
                setFrequency(extras.getInt(SET_FREQ));
        }

        if (extras.containsKey(SET_WAVE))
        {
            // Waveform buttons
            View view = null;
            switch (extras.getInt(SET_WAVE))
            {
            default:
            case Audio.SINE:
                view = findViewById(R.id.sine);
                break;

            case Audio.SQUARE:
                view = findViewById(R.id.square);
                break;

            case Audio.SAWTOOTH:
                view = findViewById(R.id.sawtooth);
                break;
            }

            onClick(view);
        }

        // Mute
        if (extras.containsKey(SET_MUTE))
        {
            boolean mute = extras.getBoolean(SET_MUTE);
            if (mute != audio.mute)
                onClick(findViewById(R.id.mute));
        }

        // Duty
        if (extras.containsKey(SET_DUTY))
        {
            int value = extras.getInt(SET_DUTY);
            if (value != 0)
                duty.setProgress(value * MAX_DUTY / 100);

            else
                duty.setProgress((int) extras.getFloat(SET_DUTY) *
                                 MAX_DUTY / 100);
        }

        // Level
        if (extras.containsKey(SET_LEVEL))
        {
            float value = extras.getFloat(SET_LEVEL);
            if (value != 0.0)
                setLevel(value);

            else
                setLevel(extras.getInt(SET_LEVEL));
        }
    }

    // Set frequency
    private void setFrequency(double freq)
    {
        // Calculate knob value
        float value = (float) Math.log10(freq / 10.0) * 200;

        // Set knob value
        if (knob != null)
            knob.setValue(value);

        // Reset fine
        if (fine != null)
            fine.setProgress(MAX_FINE / 2);
    }

    // Set level
    private void setLevel(double value)
    {
        if (value < -80.0)
            value = -80.0;

        long progress = Math.round((value / 80.0) * MAX_LEVEL) + MAX_LEVEL;
        level.setProgress((int) progress);
        audio.level = Math.pow(10.0, value / 20.0);
        display.setLevel(value);
    }

    // On knob change
    @Override
    public void onKnobChange(Knob knob, float value)
    {
        // Scale
        if (scale != null)
            scale.setValue((int) (-value * 2.5));

        // Frequency
        double frequency = Math.pow(10.0, value / 200.0) * 10.0;
        double adjust = ((fine.getProgress() - MAX_FINE / 2) /
                         (double) MAX_FINE) / 100.0;

        frequency += frequency * adjust;

        // Display
        if (display != null)
            display.setFrequency(frequency);

        if (audio != null)
            audio.frequency = frequency;

        checkBookmarks();
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
        // Duty
        case R.id.duty:
            audio.duty = ((float) progress) / MAX_DUTY;
            String text = String.format(Locale.getDefault(), "%d%%",
                                        progress * 100 / MAX_DUTY);
            custom.setText(text);
            break;

        // Fine
        case R.id.fine:
        {
            double frequency = Math.pow(10.0, knob.getValue() /
                                        200.0) * 10.0;
            double adjust = ((progress - MAX_FINE / 2) /
                             (double) MAX_FINE) / 50.0;

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
                double level =
                    ((double) progress - MAX_LEVEL) / MAX_LEVEL * 80.0;
                display.setLevel(level);
            }

            if (audio != null)
            {
                double level =
                    ((double) progress - MAX_LEVEL) / MAX_LEVEL * 80.0;
                audio.level = Math.pow(10.0, level / 20.0);
            }
            break;
        }
    }

    // On click
    @Override
    public void onClick(View v)
    {
        // Check id
        int id = v.getId();
        switch (id)
        {
        // Sine
        case R.id.sine:
            if (audio != null)
                audio.waveform = Audio.SINE;
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.square);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.sawtooth);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Square
        case R.id.square:
            if (audio != null)
                audio.waveform = Audio.SQUARE;
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.sine);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.sawtooth);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Sawtooth
        case R.id.sawtooth:
            if (audio != null)
                audio.waveform = Audio.SAWTOOTH;
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_on_background, 0, 0, 0);

            v = findViewById(R.id.sine);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            v = findViewById(R.id.square);
            ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.radiobutton_off_background, 0, 0, 0);
            break;

        // Mute
        case R.id.mute:
            if (audio != null)
                audio.mute = !audio.mute;

            if (audio.mute)
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.checkbox_on_background, 0, 0, 0);

            else
                ((Button) v).setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.checkbox_off_background, 0, 0, 0);
            break;

        // Back
        case R.id.back:
            if (bookmarks != null)
            {
                try
                {
                    Collections.reverse(bookmarks);
                    for (double bookmark : bookmarks)
                    {
                        if (bookmark < audio.frequency)
                        {
                            animateBookmark(audio.frequency, bookmark);
                            break;
                        }
                    }
                }
                finally
                {
                    Collections.sort(bookmarks);
                }
            }
            break;

        // Forward
        case R.id.forward:
            if (bookmarks != null)
            {
                for (double bookmark : bookmarks)
                {
                    if (bookmark > audio.frequency)
                    {
                        animateBookmark(audio.frequency, bookmark);
                        break;
                    }
                }
            }
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
            if (fine != null)
            {
                int progress = fine.getProgress();
                fine.setProgress(++progress);
            }
            break;

        // Less
        case R.id.less:
            if (level != null)
            {
                int progress = level.getProgress();
                level.setProgress(--progress);
            }
            break;

        // More
        case R.id.more:
            if (level != null)
            {
                int progress = level.getProgress();
                level.setProgress(++progress);
            }
            break;
        }
    }

    // animateBookmark
    private void animateBookmark(double start, double finish)
    {
        // Calculate knob values
        float value = (float) Math.log10(start / 10.0) * 200;
        float target = (float) Math.log10(finish / 10.0) * 200;

        // Start the animation
        ValueAnimator animator = ValueAnimator.ofFloat(value, target);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(this);
        animator.start();

        // Reset fine
        if (fine != null)
            fine.setProgress(MAX_FINE / 2);
    }

    // onAnimationUpdate
    @Override
    public void onAnimationUpdate(ValueAnimator animation)
    {
        // Get value
        float value = (Float) animation.getAnimatedValue();

        // Set knob value
        if (knob != null)
            knob.setValue(value);
    }

    // Show toast
    void showToast(int key, Object... args)
    {
        String format = getString(key);
        String text = String.format(Locale.getDefault(), format, args);

        showToast(text);
    }

    // Show toast
    void showToast(String text)
    {
        // Cancel the last one
        if (toast != null)
            toast.cancel();

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        // Fix for android 13
        View view = toast.getView();
        if (view != null && Build.VERSION.SDK_INT > VERSION_CODE_S_V2)
            view.setBackgroundResource(R.drawable.toast_frame);
        toast.show();
    }

    // Check bookmarks
    private void checkBookmarks()
    {
        // run
        knob.postDelayed(() ->
        {
            View back = findViewById(R.id.back);
            View forward = findViewById(R.id.forward);

            back.setEnabled(false);
            forward.setEnabled(false);

            if (bookmarks != null)
            {
                for (double bookmark : bookmarks)
                {
                    if (bookmark < audio.frequency)
                        back.setEnabled(true);

                    if (bookmark > audio.frequency)
                        forward.setEnabled(true);
                }
            }
        }, DELAY);
    }

    // Get preferences
    private void getPreferences()
    {
        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        if (audio != null)
        {
            View v = null;

            double frequency =
                Double.parseDouble(preferences.getString(PREF_FREQ, "1000.0"));
            setFrequency(frequency);

            int wave = preferences.getInt(PREF_WAVE, Audio.SINE);
            switch (wave)
            {
            default:
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

            try
            {
                int progress = preferences.getInt(PREF_DUTY, MAX_DUTY / 2);
                duty.setProgress(progress);
            }

            catch (Exception e) {}

            int progress = preferences.getInt(PREF_LEVEL, MAX_LEVEL * 3 / 4);
            level.setProgress(progress);

            boolean mute = preferences.getBoolean(PREF_MUTE, false);
            if (mute != audio.mute)
                onClick(findViewById(R.id.mute));
        }

        theme = Integer.parseInt(preferences.getString(PREF_THEME, "0"));

        String string = preferences.getString(PREF_BOOKMARKS, "");

        try
        {
            JSONArray json = new JSONArray(string);
            bookmarks = new ArrayList<>();
            for (int i = 0; i < json.length(); i++)
                bookmarks.add(json.getDouble(i));

            checkBookmarks();
        }

        catch (Exception e) {}
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

        v = findViewById(R.id.back);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.forward);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.lower);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.higher);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.less);
        if (v != null)
            v.setOnClickListener(this);

        v = findViewById(R.id.more);
        if (v != null)
            v.setOnClickListener(this);

        if (duty != null)
        {
            duty.setOnSeekBarChangeListener(this);

            duty.setMax(MAX_DUTY);
            duty.setProgress(MAX_DUTY / 2);
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
            level.setProgress(MAX_LEVEL * 3 / 4);
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

    private void checkButtons()
    {
        final View v = findViewById(R.id.mute);
        if (v != null)
            v.postDelayed(() ->
        {
            int width = v.getWidth();
            View l = findViewById(R.id.less);
            if (l != null && width < l.getWidth())
            {
                l.setVisibility(View.GONE);
                View m = findViewById(R.id.more);
                if (m != null)
                    m.setVisibility(View.GONE);
            }
        }, DELAY);
    }

    // setupPhoneStateListener
    private void setupPhoneStateListener()
    {
        phoneListener = new PhoneStateListener()
        {
            public void onCallStateChanged(int state,
                                           String incomingNumber)
            {
                if (state != TelephonyManager.CALL_STATE_IDLE)
                {
                    if (!audio.mute)
                    {
                        View v = findViewById(R.id.mute);
                        onClick(v);
                    }
                }
            }

        };

        try
        {
            TelephonyManager manager = (TelephonyManager)
                                       getSystemService(TELEPHONY_SERVICE);
            manager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        }

        catch (Exception e) {}
    }

    // A collection of unused unwanted unloved listener callback methods
    @Override
    public void onStartTrackingTouch(SeekBar seekBar)
    {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar)
    {
    }

    // Audio
    protected class Audio implements Runnable
    {
        protected static final int SINE = 0;
        protected static final int SQUARE = 1;
        protected static final int SAWTOOTH = 2;

        protected int waveform;
        protected boolean mute = false;

        protected double frequency;
        protected double level;

        protected float duty;

        protected Thread thread;

        private AudioTrack audioTrack;

        protected Audio()
        {
            frequency = 440.0;
            level = 16384.0;
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

            try
            {
                // Wait for the thread to exit
                if (t != null && t.isAlive())
                    t.join();
            }

            catch (Exception e) {}
        }

        public void run()
        {
            processAudio();
        }

        // Process audio
        @SuppressWarnings("deprecation")
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

            // Check audioTrack state
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
                double t = (duty * 2.0 * Math.PI) - Math.PI;

                // Fill the current buffer
                for (int i = 0; i < buffer.length; i++)
                {
                    f += (frequency - f) / 4096.0;
                    l += ((mute ? 0.0 : level) * 16384.0 - l) / 4096.0;
                    q += ((q + (f * K)) < Math.PI) ? f * K :
                        (f * K) - (2.0 * Math.PI);

                    switch (waveform)
                    {
                    case SINE:
                        buffer[i] = (short) Math.round(Math.sin(q) * l);
                        break;

                    case SQUARE:
                        buffer[i] = (short) ((q > t) ? l : -l);
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
