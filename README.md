# No Repeat Songs — Stop YouTube Autoplay Repeating the Same Songs (Free Android App)

[![Download APK](https://img.shields.io/github/v/release/ABDURREHMAN629/no-repeat-songs-youtube-autoplay?label=Download%20APK&style=for-the-badge)](https://github.com/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/total?style=for-the-badge)](https://github.com/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/releases)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](#install-the-apk)
[![Kotlin](https://img.shields.io/badge/Kotlin-Material%203-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](#building-from-source)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

**No Repeat Songs is a free, open-source Android app that makes YouTube playlists with only
songs you have never heard before.** Tell it the singers and genres you like (Punjabi, Hindi,
Urdu, English, lofi, sad songs, anything), tap one button, and it builds a playlist of up to
**50 new songs** that opens straight in your YouTube app, where it keeps playing with the screen off.

It **remembers every song it has given you and never gives it again** — even when the same
song is uploaded under a different title ("Official Video", "Lyrics", "Audio", another channel).
Your listening never goes back to the same 20 songs.

**No Google login. No API key. No ads. No account.** Everything it remembers stays on your phone.

## Download the APK

**[⬇ Download the latest No Repeat Songs APK](https://github.com/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/releases/latest)**
— free, no sign-up, built from the source in this repository.

### Install the APK

1. Download `No-Repeat-Songs-<version>.apk` from the [latest release](https://github.com/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/releases/latest).
2. Open it on your phone and allow installing from this source when Android asks.
3. Open **No Repeat Songs**, type a few singers, tap **Make new playlist**, then **Play playlist**.

## The problem: YouTube autoplay keeps repeating the same songs

You play one new song, put the phone down, and YouTube autoplay (or YouTube Music radio)
drifts back to the songs you already know — the same handful, again and again. YouTube has
no setting for *"don't play songs I've already heard"*, and liking, disliking or clearing
history only helps a little.

No Repeat Songs fixes that from the outside: it picks the songs, filters out everything you
have heard, and hands YouTube a ready-made playlist of genuinely new music.

## How it works

1. **You choose what you like** — singers / artists (e.g. *Karan Aujla, Sidhu Moose Wala,
   Diljit Dosanjh, AP Dhillon, R Nait*) and genres or moods (e.g. *new punjabi songs,
   punjabi romantic songs, hindi sad songs, lofi*). One per line or separated by commas.
2. **It searches YouTube** for each one, page by page, and takes turns between them so the
   playlist is mixed rather than 50 songs from one singer.
3. **It removes songs you have already heard** — by video ID *and* by a fuzzy title match,
   so *"Softly (Official Video) | Karan Aujla"* and *"Karan Aujla – Softly [Lyrics]"* count
   as the same song.
4. **It skips junk** — YouTube Shorts, hour-long jukeboxes and non-stop mixes, live streams,
   and *slowed + reverb / 8D / nightcore / status* re-uploads (unless you ask for them).
5. **It makes the playlist** (25 or 50 songs, shuffled) and **Play playlist** opens it
   directly in your YouTube app — no "open with Chrome?" question.
6. **Every song in it is remembered**, so the next playlist is all new again.

Next time it continues searching from where it stopped, while still checking page 1 so
brand-new releases are never missed.

## Features

- **No-repeat playlists** of 25 or 50 songs you have never heard, made in seconds.
- **Any language, any singer, any genre** — built with Punjabi music in mind, works for
  Hindi, Urdu, English, Bollywood, Haryanvi, lofi, ghazal, qawwali or anything on YouTube.
- **Smart duplicate detection** across different uploads and titles of the same song.
- **Junk filter** for Shorts, jukeboxes, live streams and slowed/reverb re-uploads.
- **Opens in your YouTube app** (official YouTube or another YouTube client) so screen-off
  background play works the way it already does on your phone.
- **Import your YouTube history** from Google Takeout (`.zip`, `watch-history.json` or
  `watch-history.html`) so it also skips songs you heard *before* installing the app.
- **Forget all** button to start fresh any time.
- **No login, no API key, no Google Cloud setup, no daily limit.**
- **Private** — the list of songs you have heard is stored only on your phone.
- **Material 3** design with light and dark themes. Tiny APK (about 5 MB).

## Who it is for

Anyone who listens to music on YouTube with the phone in a pocket or on the table and is
tired of autoplay looping the same songs — especially fans of **Punjabi songs**, **new
Punjabi releases**, **Hindi / Bollywood** and **Urdu** music, where new uploads appear every
day but autoplay keeps returning to the same few hits.

## Import your YouTube watch history (optional)

So the app also knows what you heard before installing it:

1. Open [takeout.google.com](https://takeout.google.com/).
2. Tap **Deselect all**, then tick **YouTube and YouTube Music**.
3. Tap **All YouTube data included**, choose only **history**, then **OK**.
4. **Next step → Create export.** Google emails you a download link.
5. Download the `.zip` on your phone, open No Repeat Songs → **Import YouTube history** → pick the zip.

The app shows these steps too (**How to get the file**).

## Good to know (honest limits)

- **Unofficial.** The app talks to YouTube the same way the youtube.com website does. This is
  not an official API and is against YouTube's terms of service, so it may stop working if
  YouTube changes its website. The app already updates itself for the most common change (a new
  website version); for anything bigger, check for a new release.
- **The playlist is temporary.** It is made with YouTube's `watch_videos` link and is not saved
  in a Google account. Play it the same day and make a new one whenever you want more songs.
- **Screen-off playback is done by your YouTube app.** The official YouTube app needs
  YouTube Premium for background play.
- **Songs you play outside the app** (normal autoplay) are unknown to it until you import your
  history again.
- **It does not download music.** It only builds playlists that YouTube plays.

## Privacy

- **No account, no login, no analytics, no ads, no trackers.**
- The only permission is **Internet**, used to search YouTube and create the playlist.
- The list of songs you have heard (video ID + title) is saved in the app's private storage on
  your phone and is never uploaded anywhere. Uninstalling the app deletes it.

---

## Building from source

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.3.2 (built-in Kotlin) |
| Gradle | 9.5.0 (wrapper included) |
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 26 (Android 8.0) |
| JDK | 17+ (Android Studio's bundled JBR is fine) |
| UI | Android Views + Material Components 3 |

```bash
./gradlew testDebugUnitTest assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`. To install straight to a
connected phone:

```bash
./gradlew installDebug
```

> AGP 9 has built-in Kotlin, so the `org.jetbrains.kotlin.android` plugin must **not** be applied.
> Release builds are signed with your local debug key so they install as updates over builds
> from Android Studio on the same computer.

## Architecture

Single module, no DI framework, no network library — plain `HttpURLConnection` and `org.json`.

```
app/src/main/java/com/norepeat/songs/
  MainActivity.kt     Screen; opens the playlist directly in the installed YouTube app
  MainViewModel.kt    Runs work off the main thread; remembers the last playlist
  PlaylistMaker.kt    Searches each artist/genre, filters, mixes and builds the playlist
  YouTubeWeb.kt       youtube.com search + temporary playlist; response parser
  SongMemory.kt       "Is this the same song?" matching across different upload titles
  PlayedStore.kt      Songs already heard, saved on the phone
  HistoryImporter.kt  Reads Google Takeout watch history (.zip / .json / .html)
```

Unit tests cover the duplicate-song matching and the YouTube response parser.

## FAQ

### How do I stop YouTube autoplay from repeating the same songs?

YouTube has no built-in setting for it. No Repeat Songs works around it: it builds a playlist
of only songs you haven't heard and plays that instead of autoplay, and it remembers every song
so the next playlist is new too.

### Is No Repeat Songs free?

Yes — free and open source (MIT), with no ads, no in-app purchases and no account.

### Do I need to log in to Google or create an API key?

No. There is no sign-in and no Google Cloud setup.

### Does it work with the screen off?

Yes, if your YouTube app plays in the background. The playlist opens in your YouTube app, which
handles playback; the official app needs YouTube Premium for screen-off play.

### Does it work with YouTube Music?

The playlist opens in the YouTube app. Songs come from YouTube, which has the same music.

### Can it find new Punjabi songs?

Yes — that is what it was built for. Put `new punjabi songs` in genres and your favourite Punjabi
singers in artists; each tap gives you songs you have not heard yet.

### Why are slowed + reverb, 8D and jukebox videos missing?

They are filtered out so you get the real songs. Write the word in your search (for example
`punjabi lofi`) and they are allowed again.

### Which Android versions are supported?

Android 8.0 (API 26) through Android 17 (API 37).

### Is my data sent anywhere?

Only your search words go to YouTube, exactly like searching on youtube.com. Your list of heard
songs never leaves the phone.

---

## Contact

- GitHub: [@ABDURREHMAN629](https://github.com/ABDURREHMAN629)
- Questions, bugs and ideas: [open an issue](https://github.com/ABDURREHMAN629/no-repeat-songs-youtube-autoplay/issues)
- Email: gabdurrehman493@gmail.com

## License

[MIT](LICENSE) — free to use, change and share.

*No Repeat Songs is an independent project and is not affiliated with, endorsed by or sponsored
by YouTube or Google. YouTube is a trademark of Google LLC.*

---

**Keywords:** youtube autoplay repeating same songs fix, stop youtube repeating songs, youtube no
repeat playlist, new songs only playlist, youtube music autoplay same songs, never repeat songs
android app, punjabi songs playlist app, new punjabi songs 2026, hindi songs playlist maker,
youtube playlist generator android, music discovery app android, background music youtube
playlist, no login youtube playlist app, open source android music app, free APK download.
