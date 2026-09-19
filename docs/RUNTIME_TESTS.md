# Runtime test notes

Tested on 19 September 2026 using an Android 16 / API 36 ARM64 Pixel Tablet emulator.

## Passed

- Clean install and launch.
- Create a document and type with the normal Android IME.
- Live word count and autosave.
- Hard force-stop followed by restart and document recovery.
- Three font selector values persist to disk.
- Bold, italic, and underline persist as formatting spans.
- Adjacent typed formatting is merged into one span instead of one span per character.
- Toolbar Copy, Cut, and Paste work on selected text.
- Cut updates the live word count and autosaves.
- Paste restores the text and formatting.
- Bundled en-GB dictionary loads locally.
- Bundled Vosk model extracts locally and starts offline recognition.
- APK requests RECORD_AUDIO only; no INTERNET permission.
- Microphone permission prompt works.
- Silent calibration samples are rejected and are not saved as a valid profile.
- Document shelf combined word count is correct.
- A-Z ordering is correct.
- 3,600-word document opens with 3,600 words counted.
- A4-ish page estimate reported 8 pages and updated while scrolling from page 8 back to page 1.
## Hardware still required

The emulator cannot validate recognition accuracy for the intended speaker.
The next hardware test must cover her normal, high, low, and naturally changing voice registers using the real tablet microphone.

The four-octave requirement remains an acceptance criterion; the calibration front-end accepts a broad pitch range and adjusts microphone gain, but recognition quality must be measured with her actual voice rather than inferred from an emulator.

## 0.1.2 hardening

- Visible Undo and Redo buttons tested in the editor.
- Undo reduced a two-word edit to one word; Redo restored the second word.
- Document saves now use atomic replacement where supported.
- Save sequencing prevents an older queued autosave from overwriting a newer manual save.
- Orphaned `.stype.tmp` files are recovered automatically on restart.
- Crash-recovery fixture restored as a normal document and appeared in the shelf word count.
- Legacy `Documents/SimpleType` data is migrated into `Documents/Wen`.

## 0.1.4 hardening

- Corrupt JSON and oversized document fixtures left Wen running; unreadable documents are isolated from normal parsing.
- Direct external launch of the non-exported EditorActivity is denied by Android.
- Invalid temp files are retained as `.recovery-failed` files.
- Valid temp recovery replaces the interrupted target only after preserving the old target as `.pre-recovery`.
- Removing `vosk-model-en/am/final.mdl` while leaving `.ready` causes the bundled model to self-repair on the next dictation start.
- Shrunk/obfuscated release APK was test-signed locally and launched on the emulator; the native speech path produced no class/link/runtime errors.
- Permission audit still reports `RECORD_AUDIO` only.
- Brimstone About panel screenshot captured from the hardened build.

## 0.1.3 visual polish

- Tablet document shelf visually checked with three neutral demo documents.
- Editor title bar, formatting row, voice status, and word/page footer checked on a 2560×1600 tablet emulator.
- OpenDyslexic font selector displays all three bundled font choices.
- Voice calibration dialog opens directly from the main editor toolbar.
- Screenshots captured from the real emulator build with no personal data.
