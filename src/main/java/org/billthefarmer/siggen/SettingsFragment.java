////////////////////////////////////////////////////////////////////////////////
//
//  Signal Generator - An Android Signal Generator written in Java.
//
//  Copyright (C) 2013	Bill Farmer
//
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 3 of the License, or
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

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;

// SettingsFragment
@SuppressWarnings("deprecation")
public class SettingsFragment extends android.preference.PreferenceFragment
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String KEY_PREF_ABOUT = "pref_about";

    private static final int VERSION_M = 23;

    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        SharedPreferences preferences =
            PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Get about summary
        Preference about = findPreference(KEY_PREF_ABOUT);

        // Set version in text view
        if (about != null)
        {
            String sum = about.getSummary().toString();
            String s = String.format(sum, BuildConfig.VERSION_NAME);
            about.setSummary(s);
        }
    }

    // on Resume
    @Override
    public void onResume()
    {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
    }

    // on Pause
    @Override
    public void onPause()
    {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
    }

    // On shared preference changed
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key)
    {
        if (key.equals(Main.PREF_DARK_THEME))
        {
            if (Build.VERSION.SDK_INT != VERSION_M)
                getActivity().recreate();
        }
    }
}
