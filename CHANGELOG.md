# Changelog

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
