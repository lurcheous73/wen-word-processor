package uk.brimstone.simpletype;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class OfflineDictationEngine {
    private static final int RATE = 16000;

    public interface Callback {
        void onStatus(String status);
        void onText(String text);
        void onError(String message);
    }

    private final Context context;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private Model model;

    public OfflineDictationEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void start(Callback callback) {
        if (running.get()) return;
        running.set(true);
        worker = new Thread(() -> runRecognition(callback), "SimpleType-Dictation");
        worker.start();
    }

    public void stop() {
        running.set(false);
    }

    public synchronized void release() {
        running.set(false);
        if (model != null) {
            model.close();
            model = null;
        }
    }

    private void runRecognition(Callback callback) {
        AudioRecord audio = null;
        Recognizer recognizer = null;
        try {
            callback.onStatus("Preparing offline speech…");
            ensureModel();
            if (model == null) model = new Model(modelDirectory().getAbsolutePath());

            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
                throw new SecurityException("Microphone permission is not granted");

            int min = AudioRecord.getMinBufferSize(RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            int bufferBytes = Math.max(min * 2, 8192);
            audio = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, bufferBytes);
            if (audio.getState() != AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("Microphone could not be opened");

            VoiceProfile profile = VoiceProfile.load(context);
            float gain = profile.calibrated ? profile.gain : 1f;
            short[] shorts = new short[bufferBytes / 2];
            byte[] bytes = new byte[shorts.length * 2];
            recognizer = new Recognizer(model, RATE);

            audio.startRecording();
            callback.onStatus("Listening offline…");
            while (running.get()) {
                int n = audio.read(shorts, 0, shorts.length);
                if (n <= 0) continue;
                pcmToBytes(shorts, n, gain, bytes);
                if (recognizer.acceptWaveForm(bytes, n * 2)) {
                    emitResult(recognizer.getResult(), callback);
                }
            }

            emitResult(recognizer.getFinalResult(), callback);
            callback.onStatus("Dictation stopped");
        } catch (Exception e) {
            callback.onError(e.getMessage() == null ? e.toString() : e.getMessage());
        } finally {
            running.set(false);
            if (audio != null) {
                try {
                    if (audio.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                        audio.stop();
                } catch (Exception ignored) {}
                audio.release();
            }
            if (recognizer != null) recognizer.close();
        }
    }

    private void emitResult(String json, Callback callback) {
        try {
            String text = new JSONObject(json).optString("text", "").trim();
            if (!text.isEmpty()) callback.onText(applySpokenPunctuation(text));
        } catch (Exception ignored) {}
    }

    private static String applySpokenPunctuation(String text) {
        String s = " " + text.trim() + " ";
        s = s.replaceAll("(?i)\\s+new paragraph\\s+", "\n\n");
        s = s.replaceAll("(?i)\\s+new line\\s+", "\n");
        s = s.replaceAll("(?i)\\s+full stop\\s+", ". ");
        s = s.replaceAll("(?i)\\s+period\\s+", ". ");
        s = s.replaceAll("(?i)\\s+comma\\s+", ", ");
        s = s.replaceAll("(?i)\\s+question mark\\s+", "? ");
        s = s.replaceAll("(?i)\\s+exclamation mark\\s+", "! ");
        return s.trim();
    }

    private static void pcmToBytes(short[] source, int count, float gain, byte[] dest) {
        for (int i = 0; i < count; i++) {
            int sample = Math.round(source[i] * gain);
            sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
            dest[i * 2] = (byte) (sample & 0xff);
            dest[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
    }

    private File modelDirectory() {
        return new File(context.getFilesDir(), "vosk-model-en");
    }

    private void ensureModel() throws Exception {
        File dir = modelDirectory();
        File marker = new File(dir, ".ready");
        if (marker.exists()) return;
        deleteTree(dir);
        AssetCopy.copyTree(context.getAssets(), "model-en", dir);
        try (FileOutputStream out = new FileOutputStream(marker)) {
            out.write("ready".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }
}
