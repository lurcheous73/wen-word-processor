# Changelog

## 0.1.4

- Added Brimstone company branding and a dedicated About panel using the Brimstone brand mark.
- Hardened document parsing with version, size, font and formatting validation.
- Hardened interrupted-save recovery with previous-target preservation and rollback.
- Invalid recovery files are retained instead of silently discarded.
- Restricted legacy migration to Wen document files.
- Explicitly marked the editor activity non-exported.
- Added local Vosk model integrity checks and self-repair.
- Added R8-minified/resource-shrunk release builds with native Vosk/JNA keep rules.
- Re-captured the screenshot set from the hardened 0.1.4 build.

## 0.1.3

- Polished the tablet document shelf and editor layout.
- Renamed remaining SimpleType UI text to Wen Word Processor.
- Added a visible document title bar in the editor.
- Split navigation/save from the formatting and speech toolbar.
- Added calmer spacing, colours, document cards, and an empty-library message.
- Added TalkBack-friendly descriptions to key editing and speech controls.
- Added project screenshots for the shelf, editor, font selector, and voice calibration.

## 0.1.2

- Renamed the visible app to Wen Word Processor.
- Added visible Undo and Redo controls.
- Added atomic document replacement where the filesystem supports it.
- Prevented stale queued autosaves from superseding newer manual saves.
- Added interrupted-save recovery for orphaned temporary document files.
- Moved the single document directory to `Documents/Wen` with automatic migration from `Documents/SimpleType`.
- Retained fully offline speech recognition, local en-GB dictionary, OpenDyslexic font set, and no INTERNET permission.
- Runtime-tested create/edit/save/reopen, formatting, clipboard, sorting, long-document page tracking, model extraction, silent-mic rejection, undo/redo, and interrupted-save recovery.

## 0.1.1

- Runtime hardening of the initial alpha.
- Fixed silent microphone calibration incorrectly succeeding at 0–0 Hz.
- Merged adjacent formatting spans to avoid metadata bloat.
- Improved page estimation and deterministic date-sort tie handling.

## 0.1.0

- Initial offline Android word processor alpha.
