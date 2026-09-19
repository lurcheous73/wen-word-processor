# SimpleType

SimpleType is a deliberately small, offline Android word processor intended for simple, distraction-free writing.

## Core behaviour

- Android 10+ (minSdk 29), target API 37.
- No INTERNET permission and Android cloud backup disabled.
- All documents live in one app-owned `Documents/SimpleType` folder.
- Document list sorts A–Z, Z–A, newest altered, or oldest altered.
- Each document shows its own word count; the shelf shows the total word count for all documents.
- Automatic save plus an explicit Save button.
- Three document fonts: OpenDyslexic, Sans, and Mono.
- Bold, italic, underline, cut, copy, and paste.
- Android IME integration keeps swipe typing and keyboard prediction available.
- Approximate page number and total pages are shown while writing.

## Offline language features

- Bundled en-GB dictionary for local spelling marks.
- Bundled Vosk speech-recognition engine and model.
- Speech commands include comma, full stop, question mark, exclamation mark, new line, and new paragraph.
- Voice calibration is on the editor toolbar.
- Calibration takes four comfortable samples, estimates the observed pitch range, and derives a safe microphone gain profile.
- Recognition audio is normalised locally using that profile before being passed to Vosk.

## Privacy

The manifest contains RECORD_AUDIO only. It intentionally does not request INTERNET permission.
Android cloud backup is disabled. Documents, dictionary work, calibration data and speech recognition remain on the device.

## Build

Open this directory in Android Studio, or run:

```sh
./gradlew clean assembleDebug lintDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## First hardware test

The important next step is testing dictation with the intended speaker across her normal, high, low, and changing registers.
Calibration measures a very wide pitch window, but recognition quality ultimately depends on the acoustic model and microphone.
