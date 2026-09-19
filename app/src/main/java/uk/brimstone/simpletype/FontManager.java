package uk.brimstone.simpletype;

import android.content.Context;
import android.graphics.Typeface;

public final class FontManager {
    public static final String OPEN_DYSLEXIC = "OpenDyslexic";
    public static final String OPEN_DYSLEXIC_3 = "OpenDyslexic 3";
    public static final String OPEN_DYSLEXIC_MONO = "OpenDyslexic Mono";
    public static final String TIMES_NEW_ROMAN = "Times New Roman";
    public static final String CAVEAT = "Caveat";

    private FontManager() {}

    public static String[] labels() {
        return new String[]{
                OPEN_DYSLEXIC,
                OPEN_DYSLEXIC_3,
                OPEN_DYSLEXIC_MONO,
                TIMES_NEW_ROMAN,
                CAVEAT
        };
    }

    public static Typeface get(Context context, String key) {
        if (OPEN_DYSLEXIC_3.equals(key))
            return context.getResources().getFont(R.font.opendyslexic3);
        if (OPEN_DYSLEXIC_MONO.equals(key))
            return context.getResources().getFont(R.font.opendyslexic_mono_regular);
        if (TIMES_NEW_ROMAN.equals(key))
            return timesNewRomanOrSerif();
        if (CAVEAT.equals(key))
            return context.getResources().getFont(R.font.caveat);
        return context.getResources().getFont(R.font.opendyslexic);
    }

    private static Typeface timesNewRomanOrSerif() {
        Typeface requested = Typeface.create("Times New Roman", Typeface.NORMAL);
        if (requested == null || requested.equals(Typeface.DEFAULT))
            return Typeface.SERIF;
        return requested;
    }
}
