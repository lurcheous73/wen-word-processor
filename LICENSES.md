# Third-party components

## OpenDyslexic

OpenDyslexic, OpenDyslexic 3, and OpenDyslexic Mono are bundled for offline use.
The current OpenDyslexic family and OpenDyslexic 3 are distributed under the SIL Open Font License; a copy of the upstream OFL text is included with the app assets.

## Caveat

Caveat is bundled for offline use.
Caveat is distributed under the SIL Open Font License 1.1. The upstream OFL text is included at `app/src/main/assets/licenses/caveat_OFL.txt`.

## Times New Roman

Times New Roman is not bundled in the APK. Microsoft documents that Windows font redistribution rights do not permit embedding the Windows font files in applications without separate redistribution licensing. Wen asks Android for the system family named "Times New Roman" when available and falls back to the platform serif family when it is not installed.

## Vosk

The Android Vosk speech-recognition library and the bundled small English model are Apache-2.0 licensed.
The application uses `com.alphacephei:vosk-android:0.3.75`.

## en-GB dictionary

The bundled en-GB spelling word list is derived from the wooorm/dictionaries en-GB package.
Its upstream licence text is included at `app/src/main/assets/licenses/en_gb_dictionary_license.txt`.
