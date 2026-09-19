# TikDown

A lightweight native Android app for downloading TikTok videos and photo
posts, replacing the earlier Termux script. Share a TikTok link to it and
it downloads in the background — no need to keep the app open.

## Features

- **Share-sheet integration** — share a TikTok link straight to TikDown; download starts immediately in the background via a foreground service, so it keeps going even after you're back in the TikTok app.
- **Album image picker** — for slideshow posts, pick exactly which images you want (all selected by default) before downloading.
- **Configurable storage location** — defaults to `Downloads/TikDown`, or pick any folder via the system folder picker.
- **Parallel downloads** — album images download concurrently instead of one at a time, and the whole app runs on OkHttp with HTTP/2 and connection pooling.
- **Neumorphic UI** — soft, embossed card style throughout, including the launcher icon.

## How it's built

| Choice | Value | Why |
|---|---|---|
| Language | Kotlin, plain Android Views (no Compose) | Matches AmpereFlow's stack; keeps the APK small |
| Networking | OkHttp 4.12.0 | Small, fast, connection pooling, HTTP/2 |
| Data source | tikwm.com public API | Same one the working Termux script used |
| JSON parsing | Built-in `org.json` | No extra dependency |
| Min SDK | 29 (Android 10) | Lets the whole app use scoped-storage `MediaStore` APIs with no legacy storage-permission branch — less code, less risk |
| Target/Compile SDK | 34 | Current stable baseline as of writing |
| AGP / Gradle / Kotlin | 8.7.3 / 8.9 / 2.0.21 | Deliberately a well-established, mature combination rather than the newest AGP 9.0 line (which shipped in January 2026 with DSL changes recent enough that I couldn't verify every detail with full confidence) |
| Build type shipped by CI | `debug` | Installable immediately, no keystore/signing secrets needed. A proper release signing config is a natural next step if you want to add it later |

## What I could and couldn't verify

I built and reviewed every file carefully, validated all XML for
well-formedness, and checked all Kotlin files for structural (brace/paren)
correctness — but **I don't have an Android SDK, Gradle, or a Kotlin
compiler in my own environment**, so I could not actually compile this
project myself. The real first compile happens in your GitHub Actions run,
exactly like it did for AmpereFlow.

I also could not fetch your actual AmpereFlow workflow file (it doesn't
appear to be publicly indexed, so it's likely a private repo I have no way
to reach) — the workflow here is reconstructed from the general JDK +
Gradle + APK-artifact recipe your project description implies, verified
against current (September 2026) guidance rather than pulled from your
exact file. If you still have your AmpereFlow `.github/workflows/*.yml`
handy, it will almost certainly drop into this repo unchanged, since
Android build workflows are largely project-agnostic — feel free to use
that instead of the one included here.

One area to sanity-check once it's running on your device: the neumorphic
shadow sizing in `NeumorphicCard.kt` (margin/corner-radius/blur constants)
is a reasonable starting point, but I can't render or preview UI in my
environment, so exact spacing may want a small tweak once you see it on
your actual screen. It's a few numbers at the top of one file.

## Setting it up

1. Create a new repo named `TikDown` under your GitHub account (or push
   this folder's contents into one).
2. Push all of this folder's contents to the `main` branch.
3. The included workflow (`.github/workflows/build.yml`) runs
   automatically on push, or you can trigger it manually from the Actions
   tab (`workflow_dispatch`).
4. Once it finishes, download the `TikDown-debug-apk` artifact from the
   run's summary page, and install it on your phone (you'll need to allow
   installs from your browser/file manager if you haven't already, same
   as with AmpereFlow).

## Using it

- **Share sheet:** In TikTok, tap Share → TikDown. Video posts download
  immediately; photo posts show a checklist so you can pick which images
  to keep.
- **Direct paste:** Open TikDown, paste a link into the box, tap
  Download.
- **Storage folder:** Tap "Change Storage Folder" in the app to pick a
  custom location, or "Reset to default" to go back to
  `Downloads/TikDown`.

## Possible next steps (not included in this version)

- Download history / log screen
- Thumbnail previews in the album picker (currently a plain numbered
  checklist, by design — it's simpler and has fewer moving parts than
  fetching and rendering thumbnails in a share-sheet flow)
- Release signing config + GitHub Secrets, if you want a properly signed
  release build instead of the debug build CI currently produces
