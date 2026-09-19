package uk.brimstone.simpletype;

import android.content.Context;
import android.graphics.Typeface;

public final class FontManager {
    public static final String OPEN_DYSLEXIC = "OpenDyslexic";
    public static final String OPEN_DYSLEXIC_3 = "OpenDyslexic 3";
    public static final String OPEN_DYSLEXIC_MONO = "OpenDyslexic Mono";

    private FontManager() {}

    public static String[] labels() {
        return new String[]{OPEN_DYSLEXIC, OPEN_DYSLEXIC_3, OPEN_DYSLEXIC_MONO};
    }

    public static Typeface get(Context context, String key) {
        if (OPEN_DYSLEXIC_3.equals(key))
            return context.getResources().getFont(R.font.opendyslexic3);
        if (OPEN_DYSLEXIC_MONO.equals(key))
            return context.getResources().getFont(R.font.opendyslexic_mono_regular);
        return context.getResources().getFont(R.font.opendyslexic);
    }
}
