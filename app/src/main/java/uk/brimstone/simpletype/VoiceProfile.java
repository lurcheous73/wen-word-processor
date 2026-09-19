package uk.brimstone.simpletype;

import android.content.Context;
import android.content.SharedPreferences;

public final class VoiceProfile {
    private static final String PREFS = "voice_profile";
    public boolean calibrated;
    public float averageRms = 0.08f;
    public float gain = 1f;
    public float minPitchHz;
    public float maxPitchHz;

    public static VoiceProfile load(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        VoiceProfile v = new VoiceProfile();
        v.calibrated = p.getBoolean("calibrated", false);
        v.averageRms = p.getFloat("average_rms", 0.08f);
        v.gain = p.getFloat("gain", 1f);
        v.minPitchHz = p.getFloat("min_pitch", 0f);
        v.maxPitchHz = p.getFloat("max_pitch", 0f);
        return v;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("calibrated", calibrated)
                .putFloat("average_rms", averageRms)
                .putFloat("gain", gain)
                .putFloat("min_pitch", minPitchHz)
                .putFloat("max_pitch", maxPitchHz)
                .apply();
    }
}
