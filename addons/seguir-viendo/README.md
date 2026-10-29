# Seguir viendo

What you left half-watched, on Home. Seguir viendo reads your [Trakt](https://trakt.tv) account, where Stremio on
this TV and Kodi on any other device scrobble what you watch, and publishes each paused episode or film as a row in
Android TV's Continue watching (Watch Next) table. Any home screen that reads that table shows them; in this fork the
launcher's `continue_watching` row does (`FORK.md`). OK on a row opens Stremio.

![Seguir viendo on a TV](screenshot.jpg)

- **Your own rows only.** It writes rows under its own package and replaces them on each sync, so a show you finish
  on another device leaves the row at the next sync. Other apps' rows are never touched.
- **Opens the app, not itself.** Each row opens a small trampoline in this app, which starts Stremio (or shows that
  Stremio is not installed). The launcher only follows a row's link when it points at the app that published it, so
  the trampoline is what lets a row from here open Stremio.
- **Next episodes too.** For each show you watched in the last 60 days and have nothing paused in, the next unwatched
  episode gets an "up next" row.
- **Artwork from TMDB, served locally.** Trakt has no images, so with a TMDB key the app fetches each show's or film's
  wide backdrop once, keeps it in its cache and hands the launcher a `content://` address: the launcher, which has no
  network access, reads the picture from this app. Without a key the launcher shows this app's banner.
- **Straight to the episode.** In Stremio a row opens that episode's or film's page, by its IMDb id; when Trakt has no
  IMDb id, or the app is set to Kodi, it opens the app.

## Setting it up

1. Create an application at <https://trakt.tv/oauth/applications/new>: any name, no permissions ticked. A
   Development application only accepts a loopback redirect URI, so give it `http://127.0.0.1`. The sign-in on the
   TV never opens it; Trakt only checks it matches when the app renews its session.
2. Put its keys in `addons/seguir-viendo/local.properties` (git ignores the file, so they never reach a commit):

   ```properties
   sdk.dir=/path/to/Android/sdk
   trakt.clientId=<Client ID>
   trakt.redirectUri=http://127.0.0.1
   ```

   For artwork, add `tmdb.apiKey=<key>`: a TMDB account's API key or API read access token, both on
   <https://www.themoviedb.org/settings/api>. Without it the rows have no artwork.

   Trakt no longer issues a client secret for new applications ("Client Secret: Not issued", PKCE): the app signs
   in with a PKCE challenge instead. Add `trakt.clientSecret=<Client Secret>` only for an older application that has
   one, and leave `trakt.redirectUri` out only if it was registered with `urn:ietf:wg:oauth:2.0:oob`.

3. Build and install (below), open **Seguir viendo**, choose **Connect Trakt** and enter the code it shows at
   <https://trakt.tv/activate> from your phone. No password is typed on the TV.
4. Choose **Sync now**. Without the keys the app says so and the Trakt part stays off.
5. **Open in** switches the app the rows open between Stremio and Kodi, whichever is installed. **Disconnect** signs
   out: it deletes the Trakt tokens, this app's rows and the stored artwork, and stops the hourly sync.

## Build and install

From the repository root:

```bash
cd addons/seguir-viendo && ./gradlew assembleRelease && cd ../..
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py --push addons/seguir-viendo/app/build/outputs/apk/release/app-release.apk /data/local/tmp/addon.apk
TV_HOST=<tv ip> tools/venv/bin/python -I tools/tv.py 'pm install -r /data/local/tmp/addon.apk; rm -f /data/local/tmp/addon.apk'
```

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | To reach `api.trakt.tv` for the sign-in and the paused items; nothing else goes online |
| `com.android.providers.tv.permission.WRITE_EPG_DATA` | To write its own rows to the system's Watch Next table |
| `ACCESS_NETWORK_STATE` | So the hourly sync waits until the TV has a network |
| `RECEIVE_BOOT_COMPLETED` | To sync once when the TV starts and keep the hourly sync across restarts |

It also has a read-only content provider, `io.github.eltorres.seguirviendo.posters`, that any app can read: it answers
only to opaque image ids and returns only the artwork, so the launcher can draw it without network access.

## Background work

While Trakt is connected, Android's JobScheduler runs a sync about once an hour, only when there is a network, and
once after the TV starts. Nothing runs in the foreground and there is no persistent service. It also syncs when you
press **Sync now**, right after you connect and after you change **Open in**. No telemetry.

## What it stores on the TV

The Trakt tokens, encrypted with a key held in Android Keystore, in the app's private storage, the app chosen in
**Open in**, the time of the last sync, and the artwork of the rows on screen in its cache (older pictures are
deleted at each sync). Tokens never appear in logcat. Removing the app removes them; revoking the app at
<https://trakt.tv/oauth/authorized_applications> ends the session from the Trakt side.

## Credits

This product uses the TMDB API but is not endorsed or certified by TMDB. Viewing data comes from Trakt.

## Tested on

TCL 55C725 (Android TV 11) with this fork's launcher and Stremio 1.12.0: Trakt connected with the device code (PKCE
application, no client secret), paused episodes and next episodes in the Continue watching row with TMDB artwork and
their progress, OK on a card opening that episode in Stremio, a newly paused episode arriving with the scheduled sync,
and Disconnect emptying the row and stopping the sync.
