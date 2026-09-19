package uk.brimstone.simpletype;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class EditorActivity extends Activity {
    public static final String EXTRA_FILE_NAME = "file_name";
    private static final int REQ_AUDIO = 41;
    private static final int AUDIO_DICTATE = 1;
    private static final int AUDIO_CALIBRATE = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final AtomicLong saveGeneration = new AtomicLong();

    private DocumentRepository repository;
    private OfflineDictationEngine dictation;
    private LocalDictionary localDictionary;
    private RichEditText editor;
    private TextView stats;
    private TextView voiceStatus;
    private TextView documentTitle;
    private Button boldButton;
    private Button italicButton;
    private Button underlineButton;
    private Button dictateButton;
    private Spinner fontSpinner;

    private String fileName;
    private String title = "Untitled";
    private String saveState = "Saved";
    private boolean loading;
    private int pendingAudioAction;

    private final Runnable saveRunnable = this::saveAsync;
    private final Runnable spellRunnable = this::checkSpelling;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        if (fileName == null) {
            finish();
            return;
        }
        repository = new DocumentRepository(this);
        dictation = new OfflineDictationEngine(this);
        localDictionary = new LocalDictionary(this);
        io.execute(() -> {
            try {
                localDictionary.load();
                runOnUiThread(this::checkSpelling);
            } catch (Exception ignored) {}
        });
        buildUi();
        loadDocument();
        updateVoiceStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(244, 242, 238));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), dp(7), dp(8), dp(4));

        Button docs = button("Documents");
        docs.setContentDescription("Back to documents");
        docs.setOnClickListener(v -> {
            saveSync();
            finish();
        });
        header.addView(docs);

        documentTitle = new TextView(this);
        documentTitle.setText("Untitled");
        documentTitle.setTextSize(22);
        documentTitle.setTextColor(Color.rgb(54, 50, 57));
        documentTitle.setTypeface(FontManager.get(this, FontManager.OPEN_DYSLEXIC));
        documentTitle.setSingleLine(true);
        documentTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        documentTitle.setPadding(dp(16), 0, dp(16), 0);
        header.addView(documentTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button save = button("Save");
        save.setContentDescription("Save document");
        save.setOnClickListener(v -> saveSync());
        header.addView(save);
        root.addView(header);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(2), dp(8), dp(6));

        fontSpinner = new Spinner(this);
        fontSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, FontManager.labels()));
        fontSpinner.setContentDescription("Document font");
        toolbar.addView(fontSpinner, new LinearLayout.LayoutParams(dp(220), dp(54)));

        boldButton = button("B");
        boldButton.setContentDescription("Bold");
        boldButton.setTypeface(null, android.graphics.Typeface.BOLD);
        boldButton.setOnClickListener(v -> {
            boolean on = editor.toggleBold();
            boldButton.setText(on ? "B ✓" : "B");
            scheduleSave();
        });
        toolbar.addView(boldButton);

        italicButton = button("I");
        italicButton.setContentDescription("Italic");
        italicButton.setTypeface(null, android.graphics.Typeface.ITALIC);
        italicButton.setOnClickListener(v -> {
            boolean on = editor.toggleItalic();
            italicButton.setText(on ? "I ✓" : "I");
            scheduleSave();
        });
        toolbar.addView(italicButton);

        underlineButton = button("U");
        underlineButton.setContentDescription("Underline");
        underlineButton.setOnClickListener(v -> {
            boolean on = editor.toggleUnderline();
            underlineButton.setText(on ? "U ✓" : "U");
            scheduleSave();
        });
        toolbar.addView(underlineButton);

        toolbar.addView(separator());

        Button cut = button("Cut");
        cut.setOnClickListener(v -> editor.onTextContextMenuItem(android.R.id.cut));
        toolbar.addView(cut);

        Button copy = button("Copy");
        copy.setOnClickListener(v -> editor.onTextContextMenuItem(android.R.id.copy));
        toolbar.addView(copy);

        Button paste = button("Paste");
        paste.setOnClickListener(v -> editor.onTextContextMenuItem(android.R.id.paste));
        toolbar.addView(paste);

        toolbar.addView(separator());

        Button undo = button("Undo");
        undo.setOnClickListener(v -> editor.onTextContextMenuItem(android.R.id.undo));
        toolbar.addView(undo);

        Button redo = button("Redo");
        redo.setOnClickListener(v -> editor.onTextContextMenuItem(android.R.id.redo));
        toolbar.addView(redo);

        toolbar.addView(separator());

        dictateButton = button("Dictate");
        dictateButton.setContentDescription("Start or stop offline dictation");
        dictateButton.setOnClickListener(v -> {
            if (dictation.isRunning()) stopDictation();
            else requireAudio(AUDIO_DICTATE);
        });
        toolbar.addView(dictateButton);

        Button calibrate = button("Calibrate voice");
        calibrate.setContentDescription("Calibrate voice recognition");
        calibrate.setOnClickListener(v -> requireAudio(AUDIO_CALIBRATE));
        toolbar.addView(calibrate);

        scroll.addView(toolbar);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        voiceStatus = new TextView(this);
        voiceStatus.setTextSize(15);
        voiceStatus.setTextColor(Color.rgb(92, 87, 96));
        voiceStatus.setPadding(dp(16), dp(5), dp(16), dp(8));
        root.addView(voiceStatus);

        editor = new RichEditText(this);
        editor.setBackgroundColor(Color.rgb(253, 252, 249));
        root.addView(editor, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        stats = new TextView(this);
        stats.setTextSize(15);
        stats.setTextColor(Color.rgb(92, 87, 96));
        stats.setPadding(dp(16), dp(8), dp(16), dp(10));
        root.addView(stats);

        editor.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!loading) scheduleSave();
                handler.removeCallbacks(spellRunnable);
                handler.postDelayed(spellRunnable, 550);
                updateStatsSoon();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        editor.setOnScrollChangeListener((v, sx, sy, ox, oy) -> updateStats());

        fontSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,
                                                  android.view.View view, int position, long id) {
                if (editor == null) return;
                String selected = FontManager.labels()[position];
                editor.setDocumentFont(selected);
                if (!loading) scheduleSave();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        setContentView(root);
    }

    private void loadDocument() {
        loading = true;
        try {
            DocumentData data = repository.load(fileName);
            title = data.title;
            setTitle(title);
            if (documentTitle != null) documentTitle.setText(title);
            editor.setText(data.text);
            editor.setDocumentFont(data.fontKey);
            editor.restoreSpans(data.spans);
            selectFont(data.fontKey);
            editor.setSelection(editor.length());
            saveState = "Saved";
        } catch (Exception e) {
            Toast.makeText(this, "Could not open this document", Toast.LENGTH_LONG).show();
        } finally {
            loading = false;
            updateStatsSoon();
        }
    }

    private void selectFont(String font) {
        String[] labels = FontManager.labels();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(font)) {
                fontSpinner.setSelection(i, false);
                return;
            }
        }
        fontSpinner.setSelection(0, false);
    }

    private void scheduleSave() {
        if (loading) return;
        saveState = "Saving…";
        updateStats();
        handler.removeCallbacks(saveRunnable);
        handler.postDelayed(saveRunnable, 700);
    }

    private DocumentData snapshot() {
        DocumentData data = new DocumentData();
        data.title = title;
        data.text = editor.getText().toString();
        data.fontKey = editor.getDocumentFont();
        data.spans = editor.serializeSpans();
        return data;
    }

    private void saveAsync() {
        long generation = saveGeneration.incrementAndGet();
        DocumentData data = snapshot();
        io.execute(() -> {
            try {
                repository.save(fileName, data);
                runOnUiThread(() -> {
                    if (generation == saveGeneration.get()) {
                        saveState = "Saved";
                        updateStats();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (generation == saveGeneration.get()) {
                        saveState = "Save failed";
                        updateStats();
                    }
                });
            }
        });
    }

    private void saveSync() {
        handler.removeCallbacks(saveRunnable);
        long generation = saveGeneration.incrementAndGet();
        DocumentData data = snapshot();
        try {
            io.submit(() -> {
                repository.save(fileName, data);
                return null;
            }).get();
            if (generation == saveGeneration.get()) saveState = "Saved";
        } catch (Exception e) {
            saveState = "Save failed";
            Toast.makeText(this, "Could not save document", Toast.LENGTH_LONG).show();
        }
        updateStats();
    }

    private void updateStatsSoon() {
        editor.post(this::updateStats);
    }

    private void checkSpelling() {
        if (localDictionary != null && localDictionary.isLoaded() && editor != null)
            localDictionary.markParagraph(editor);
    }

    private void updateStats() {
        if (stats == null || editor == null) return;
        int words = DocumentRepository.countWords(editor.getText().toString());
        int pageHeight = Math.max(dp(600),
                Math.round(editor.getWidth() * (297f / 210f)));
        int contentHeight = editor.getLayout() == null
                ? editor.getHeight()
                : editor.getLayout().getHeight() + editor.getPaddingTop() + editor.getPaddingBottom();
        int pages = Math.max(1, (int) Math.ceil(contentHeight / (double) pageHeight));
        int page = Math.max(1, Math.min(pages, editor.getScrollY() / pageHeight + 1));
        stats.setText(String.format(Locale.UK, "Words: %,d   •   Page %d of %d   •   %s",
                words, page, pages, saveState));
    }

    private void requireAudio(int action) {
        pendingAudioAction = action;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            performAudioAction(action);
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                      int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performAudioAction(pendingAudioAction);
        } else if (requestCode == REQ_AUDIO) {
            Toast.makeText(this, "Microphone permission is needed for speech.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void performAudioAction(int action) {
        if (action == AUDIO_CALIBRATE) showCalibration();
        else if (action == AUDIO_DICTATE) startDictation();
        pendingAudioAction = 0;
    }

    private void showCalibration() {
        if (dictation.isRunning()) stopDictation();
        VoiceCalibrator.show(this, new VoiceCalibrator.Callback() {
            @Override public void onFinished(VoiceProfile profile) {
                updateVoiceStatus();
            }
            @Override public void onError(String message) {
                voiceStatus.setText("Voice calibration: try that sample again");
            }
        });
    }

    private void startDictation() {
        dictateButton.setText("Stop dictation");
        dictation.start(new OfflineDictationEngine.Callback() {
            @Override public void onStatus(String status) {
                runOnUiThread(() -> voiceStatus.setText(status));
            }

            @Override public void onText(String text) {
                runOnUiThread(() -> insertDictatedText(text));
            }

            @Override public void onError(String message) {
                runOnUiThread(() -> {
                    dictateButton.setText("Dictate");
                    voiceStatus.setText("Dictation error: " + message);
                });
            }
        });
    }

    private void stopDictation() {
        dictation.stop();
        dictateButton.setText("Dictate");
        updateVoiceStatus();
    }

    private void insertDictatedText(String spoken) {
        if (spoken == null || spoken.isBlank()) return;
        Editable e = editor.getText();
        int start = Math.max(0, editor.getSelectionStart());
        int end = Math.max(0, editor.getSelectionEnd());
        int a = Math.min(start, end);
        int b = Math.max(start, end);

        String text = spoken;
        if (a > 0 && !Character.isWhitespace(e.charAt(a - 1))
                && !text.startsWith("\n") && !text.matches("^[,.;!?].*")) {
            text = " " + text;
        }
        if (!text.endsWith("\n")) text = text + " ";
        e.replace(a, b, text);
        editor.setSelection(Math.min(e.length(), a + text.length()));
    }

    private void updateVoiceStatus() {
        VoiceProfile p = VoiceProfile.load(this);
        if (!p.calibrated) {
            voiceStatus.setText("Voice: not calibrated");
        } else if (p.minPitchHz > 0 && p.maxPitchHz > 0) {
            voiceStatus.setText(String.format(Locale.UK,
                    "Voice: calibrated ✓   range seen %.0f–%.0f Hz",
                    p.minPitchHz, p.maxPitchHz));
        } else {
            voiceStatus.setText("Voice: calibrated ✓");
        }
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setMinHeight(dp(50));
        b.setMinWidth(dp(68));
        return b;
    }

    private TextView separator() {
        TextView line = new TextView(this);
        line.setBackgroundColor(Color.rgb(205, 201, 207));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(1), dp(34));
        params.setMargins(dp(6), 0, dp(6), 0);
        line.setLayoutParams(params);
        return line;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onPause() {
        super.onPause();
        if (!loading) saveSync();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(saveRunnable);
        handler.removeCallbacks(spellRunnable);
        if (dictation != null) dictation.release();
        io.shutdown();
        super.onDestroy();
    }
}
