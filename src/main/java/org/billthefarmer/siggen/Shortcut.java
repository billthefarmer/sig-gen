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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Locale;

@SuppressWarnings("deprecation")
public class Shortcut extends Activity
{
    public final static String TAG = "Shortcut";

    // onCreate
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        int theme = Integer.parseInt(preferences.getString(Main.PREF_THEME,
                                                           "0"));
        // Get day/night mode
        Configuration config = getResources().getConfiguration();
        int night = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        // Set theme
        switch (theme)
        {
        case Main.LIGHT:
            setTheme(R.style.DialogTheme);
            break;

        case Main.DARK:
            setTheme(R.style.DialogDarkTheme);
            break;

        case Main.SYSTEM:
            switch (night)
            {
            case Configuration.UI_MODE_NIGHT_NO:
                setTheme(R.style.DialogTheme);
                break;

            case Configuration.UI_MODE_NIGHT_YES:
                setTheme(R.style.DialogDarkTheme);
                break;
            }
            break;
        }

        // Set content
        setContentView(R.layout.shortcut);

        TextView nameView = findViewById(R.id.name);
        TextView freqView = findViewById(R.id.freq);
        TextView levelView = findViewById(R.id.level);
        RadioButton sine = findViewById(R.id.sine);
        RadioButton square = findViewById(R.id.square);
        RadioButton sawtooth = findViewById(R.id.sawtooth);
        CheckBox muteBox = findViewById(R.id.mute);
        Button cancel = findViewById(R.id.cancel);
        Button clear = findViewById(R.id.clear);
        Button create = findViewById(R.id.create);

        cancel.setOnClickListener((v) ->
        {
            setResult(RESULT_CANCELED, null);
            finish();
        });

        clear.setOnClickListener((v) ->
        {
            nameView.setText("");
            freqView.setText("");
            levelView.setText("");
            sine.setChecked(false);
            square.setChecked(false);
            sawtooth.setChecked(false);
            muteBox.setChecked(false);
        });

        create.setOnClickListener((v) ->
        {
            String value = freqView.getText().toString();
            float freq = Float.NaN;

            // Ignore empty string
            if (!value.isEmpty())
                freq = Float.parseFloat(value);

            // Ignore if out of range
            if (freq < 0.1 || freq > 25000)
                freq = Float.NaN;

            value = levelView.getText().toString();
            float level = Float.NaN;

            // Ignore empty string
            if (!value.isEmpty())
                level = Float.parseFloat(value);

            // Ignore if out of range
            if (level < -80 || level > 0)
                level = Float.NaN;

            int waveform = -1;
            if (sine.isChecked())
                waveform = Main.Audio.SINE;

            else if (square.isChecked())
                waveform = Main.Audio.SQUARE;

            else if (sawtooth.isChecked())
                waveform = Main.Audio.SAWTOOTH;

            boolean mute = muteBox.isChecked();

            // Create the shortcut intent
            Intent shortcut = new Intent(this, Main.class);
            shortcut.setAction(Intent.ACTION_MAIN);
            shortcut.addCategory(Intent.CATEGORY_DEFAULT);
            if (!Float.isNaN(freq))
                shortcut.putExtra(Main.SET_FREQ, freq);
            if (!Float.isNaN(level))
                shortcut.putExtra(Main.SET_LEVEL, level);
            if (waveform != -1)
                shortcut.putExtra(Main.SET_WAVE, waveform);
            shortcut.putExtra(Main.SET_MUTE, mute);

            StringBuilder name = new StringBuilder();
            value = nameView.getText().toString();
            if (value.isEmpty())
            {
                if (!Float.isNaN(freq))
                    name.append(String.format(Locale.getDefault(),
                                              "%1.2fHz ", freq));
                if (!Float.isNaN(level))
                    name.append(String.format(Locale.getDefault(),
                                              "%1.2fdB ", level));
                if (waveform != -1)
                {
                    switch (waveform)
                    {
                    case Main.Audio.SINE:
                        name.append(getText(R.string.sine));
                        break;

                    case Main.Audio.SQUARE:
                        name.append(getText(R.string.square));
                        break;

                    case Main.Audio.SAWTOOTH:
                        name.append(getText(R.string.sawtooth));
                        break;
                    }
                    name.append(" ");
                }

                if (mute)
                    name.append(getText(R.string.mute));

                if (name.length() == 0)
                    name.append(getText(R.string.app_name));
            }

            else
                name.append(value);

            // Create the shortcut
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name.toString());
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                            Intent.ShortcutIconResource.fromContext
                            (this, R.drawable.ic_launcher));

            setResult(RESULT_OK, intent);
            finish();
        });
    }
}
