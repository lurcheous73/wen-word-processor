package uk.brimstone.simpletype;

import android.content.Context;
import android.graphics.Typeface;

public final class FontManager {
    public static final String OPEN_DYSLEXIC = "OpenDyslexic";
    public static final String SANS = "Sans";
    public static final String MONO = "Mono";

    private FontManager() {}

    public static String[] labels() {
        return new String[]{OPEN_DYSLEXIC, SANS, MONO};
    }

    public static Typeface get(Context context, String key) {
        if (MONO.equals(key)) return Typeface.MONOSPACE;
        if (SANS.equals(key)) return Typeface.SANS_SERIF;
        return context.getResources().getFont(R.font.opendyslexic);
    }
}
