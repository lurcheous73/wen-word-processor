package uk.brimstone.simpletype;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class VoiceCalibrator {
    private static final int RATE = 16000;
    private static final int SECONDS = 2;

    public interface Callback {
        void onFinished(VoiceProfile profile);
        void onError(String message);
    }

    private VoiceCalibrator() {}

    public static void show(Activity activity, Callback callback) {
        String[] steps = {
                "Use her normal comfortable voice.",
                "Use her comfortable higher voice.",
                "Use her comfortable lower voice.",
                "Use the voice as it naturally changes."
        };
        float[] rms = new float[steps.length];
        float[] pitch = new float[steps.length];
        int[] index = {0};

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(activity, 24), dp(activity, 18), dp(activity, 24), dp(activity, 12));

        TextView heading = new TextView(activity);
        heading.setTextSize(22);
        heading.setText("Voice calibration");
        box.addView(heading);

        TextView instruction = new TextView(activity);
        instruction.setTextSize(18);
        instruction.setPadding(0, dp(activity, 12), 0, dp(activity, 8));
        box.addView(instruction);

        TextView phrase = new TextView(activity);
        phrase.setTextSize(17);
        phrase.setText("Say: “The quick brown fox jumps over the lazy dog.”");
        box.addView(phrase);

        TextView status = new TextView(activity);
        status.setTextSize(16);
        status.setPadding(0, dp(activity, 12), 0, dp(activity, 12));
        box.addView(status);

        Button record = new Button(activity);
        record.setText("Record sample 1 of 4");
        box.addView(record, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(box)
                .setNegativeButton("Close", null)
                .create();

        Runnable update = () -> {
            if (index[0] >= steps.length) return;
            instruction.setText(steps[index[0]]);
            record.setText(String.format(Locale.UK, "Record sample %d of %d",
                    index[0] + 1, steps.length));
            status.setText("Ready");
            record.setEnabled(true);
        };

        record.setOnClickListener(v -> {
            record.setEnabled(false);
            status.setText("Listening…");
            int slot = index[0];
            new Thread(() -> {
                try {
                    Sample s = capture(activity);
                    rms[slot] = s.rms;
                    pitch[slot] = s.pitchHz;
                    activity.runOnUiThread(() -> {
                        index[0]++;
                        if (index[0] < steps.length) {
                            update.run();
                        } else {
                            VoiceProfile profile = buildProfile(rms, pitch);
                            profile.save(activity);
                            status.setText(String.format(Locale.UK,
                                    "Calibrated ✓  approximately %.0f–%.0f Hz",
                                    profile.minPitchHz, profile.maxPitchHz));
                            record.setText("Calibration complete");
                            record.setEnabled(false);
                            callback.onFinished(profile);
                        }
                    });
                } catch (Exception e) {
                    activity.runOnUiThread(() -> {
                        record.setEnabled(true);
                        status.setText("Could not record. Try again.");
                        callback.onError(e.getMessage() == null ? "Recording error" : e.getMessage());
                    });
                }
            }, "SimpleType-Calibration").start();
        });

        dialog.setOnShowListener(d -> update.run());
        dialog.show();
    }

    private static Sample capture(Activity activity) throws Exception {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Microphone permission is not granted");
        int min = AudioRecord.getMinBufferSize(RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(min * 2, 4096);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED)
            throw new IllegalStateException("Microphone could not be opened");

        short[] all = new short[RATE * SECONDS];
        short[] buffer = new short[bufferSize / 2];
        int offset = 0;
        recorder.startRecording();
        try {
            while (offset < all.length) {
                int wanted = Math.min(buffer.length, all.length - offset);
                int n = recorder.read(buffer, 0, wanted);
                if (n <= 0) continue;
                System.arraycopy(buffer, 0, all, offset, n);
                offset += n;
            }
        } finally {
            recorder.stop();
            recorder.release();
        }

        double sum = 0;
        for (short value : all) {
            double x = value / 32768.0;
            sum += x * x;
        }
        float rms = (float) Math.sqrt(sum / Math.max(1, all.length));
        float pitch = estimatePitch(all);
        return new Sample(rms, pitch);
    }

    private static float estimatePitch(short[] data) {
        List<Float> values = new ArrayList<>();
        final int frame = 1024;
        final int minLag = RATE / 2000;
        final int maxLag = RATE / 50;

        for (int start = 0; start + frame < data.length; start += frame) {
            double energy = 0;
            for (int i = 0; i < frame; i++) {
                double x = data[start + i] / 32768.0;
                energy += x * x;
            }
            if (energy / frame < 0.00002) continue;

            double best = 0;
            int bestLag = 0;
            for (int lag = minLag; lag <= maxLag && lag < frame / 2; lag++) {
                double corr = 0;
                double a = 0;
                double b = 0;
                for (int i = 0; i < frame - lag; i += 2) {
                    double x = data[start + i];
                    double y = data[start + i + lag];
                    corr += x * y;
                    a += x * x;
                    b += y * y;
                }
                double norm = corr / Math.sqrt(Math.max(1.0, a * b));
                if (norm > best) {
                    best = norm;
                    bestLag = lag;
                }
            }
            if (bestLag > 0 && best > 0.35)
                values.add((float) RATE / bestLag);
        }

        if (values.isEmpty()) return 0f;
        Collections.sort(values);
        return values.get(values.size() / 2);
    }

    private static VoiceProfile buildProfile(float[] rms, float[] pitch) {
        VoiceProfile p = new VoiceProfile();
        p.calibrated = true;
        float sum = 0;
        float minPitch = Float.MAX_VALUE;
        float maxPitch = 0;
        for (int i = 0; i < rms.length; i++) {
            sum += rms[i];
            if (pitch[i] > 0) {
                minPitch = Math.min(minPitch, pitch[i]);
                maxPitch = Math.max(maxPitch, pitch[i]);
            }
        }
        p.averageRms = sum / Math.max(1, rms.length);
        p.gain = clamp(0.10f / Math.max(0.01f, p.averageRms), 0.5f, 4.0f);
        p.minPitchHz = minPitch == Float.MAX_VALUE ? 0 : minPitch;
        p.maxPitchHz = maxPitch;
        return p;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int dp(Activity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private static final class Sample {
        final float rms;
        final float pitchHz;
        Sample(float rms, float pitchHz) {
            this.rms = rms;
            this.pitchHz = pitchHz;
        }
    }
}
