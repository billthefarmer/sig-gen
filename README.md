# ![Logo](src/main/res/drawable-hdpi/ic_launcher.png) Signal Generator [![.github/workflows/main.yml](https://github.com/billthefarmer/sig-gen/workflows/.github/workflows/main.yml/badge.svg)](https://github.com/billthefarmer/sig-gen/actions) [![Release](https://img.shields.io/github/release/billthefarmer/sig-gen.svg?logo=github)](https://github.com/billthefarmer/sig-gen/releases) [<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.svg" alt="Get it on F-Droid" width="128">](https://f-droid.org/packages/org.billthefarmer.siggen)

A signal generator for Android. The app can be downloaded from [F-Droid](https://f-droid.org/packages/org.billthefarmer.siggen)
and [here](https://github.com/billthefarmer/sig-gen/releases).

![](https://github.com/billthefarmer/billthefarmer.github.io/raw/master/images/SigGen.png)

 *  Frequency range 0.1Hz - 25KHz
 *  Level range 0dB - -80dB
 *  Set exact frequency
 *  Frequency bookmarks
 *  Square wave duty cycle

### Using
The frequency knob responds to finger twirling. The fine frequency and
level sliders allow for fine adjustments of frequency and output
level. The frequency knob is also adjustable using the left and right
arrow buttons above. The two bookmark buttons below the frequency knob
show if there are bookmarks, and will go to the next lower or higher
bookmark if it exists.

The toolbar items are, from left to right:
 * **Exact** - Prompt for an exact frequency
 * **Bookmark** - Save the current frequency as a bookmark
 * **Sleep** - Prevent the device from sleeping
 * **Settings** - Show the settings.
 
To remove a bookmark, go to it and touch the toolbar bookmark button.

### External Control
The app may be started, the frequency, level, waveform and mute
externally by sending a suitable
[Intent](https://developer.android.com/reference/android/content/Intent). The
parameters should be:

| Parameter | Value | Type | Value |
| --------- | ----- | ---- | ----- |
| Action | android.intent.action.MAIN |
| | android.intent.action.DEFAULT |
| Category | android.intent.category.LAUNCHER |
| | android.intent.category.DEFAULT |
| Extras | org.billthefarmer.siggen.SET_FREQ | int, float |
| | org.billthefarmer.siggen.SET_LEVEL | int, float |
| | org.billthefarmer.siggen.SET_WAVE | int | 0 = Sine |
| | | | 1 = Square |
| | | | 2 = Sawtooth |
| | org.billthefarmer.siggen.SET_MUTE | boolean |

Any combination of extras or none may be sent. Subsequent intents sent
will update the parameters from the included extras.
