package uk.brimstone.simpletype;

import android.graphics.Typeface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DocumentData {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_TITLE_CHARS = 200;
    public static final int MAX_TEXT_CHARS = 5_000_000;
    public static final int MAX_SPANS = 50_000;

    public String title = "Untitled";
    public String text = "";
    public String fontKey = FontManager.OPEN_DYSLEXIC;
    public JSONArray spans = new JSONArray();

    public JSONObject toJson() throws JSONException {
        validateForSave();
        JSONObject o = new JSONObject();
        o.put("version", CURRENT_VERSION);
        o.put("title", title);
        o.put("text", text);
        o.put("font", fontKey);
        o.put("spans", spans);
        return o;
    }

    public static DocumentData fromJson(String raw) throws JSONException {
        if (raw == null || raw.isBlank())
            throw new JSONException("Document is empty");

        JSONObject o = new JSONObject(raw);
        int version = o.optInt("version", CURRENT_VERSION);
        if (version < 1 || version > CURRENT_VERSION)
            throw new JSONException("Unsupported document version: " + version);

        DocumentData d = new DocumentData();
        d.title = sanitiseTitle(o.optString("title", "Untitled"));
        d.text = o.optString("text", "");
        if (d.text.length() > MAX_TEXT_CHARS)
            throw new JSONException("Document text is too large");

        String font = o.optString("font", FontManager.OPEN_DYSLEXIC);
        d.fontKey = isKnownFont(font) ? font : FontManager.OPEN_DYSLEXIC;

        JSONArray source = o.optJSONArray("spans");
        d.spans = sanitiseSpans(source, d.text.length());
        return d;
    }

    public void validateForSave() throws JSONException {
        title = sanitiseTitle(title);
        if (text == null) text = "";
        if (text.length() > MAX_TEXT_CHARS)
            throw new JSONException("Document text is too large");
        if (!isKnownFont(fontKey))
            fontKey = FontManager.OPEN_DYSLEXIC;
        spans = sanitiseSpans(spans, text.length());
    }

    private static JSONArray sanitiseSpans(JSONArray source, int textLength)
            throws JSONException {
        JSONArray clean = new JSONArray();
        if (source == null) return clean;
        if (source.length() > MAX_SPANS)
            throw new JSONException("Too many formatting spans");

        for (int i = 0; i < source.length(); i++) {
            JSONObject span = source.optJSONObject(i);
            if (span == null) continue;

            String kind = span.optString("kind", "");
            int start = span.optInt("start", -1);
            int end = span.optInt("end", -1);
            if (start < 0 || end <= start || end > textLength) continue;

            JSONObject safe = new JSONObject();
            if ("underline".equals(kind)) {
                safe.put("kind", "underline");
            } else if ("style".equals(kind)) {
                int style = span.optInt("value", Typeface.NORMAL);
                if (style != Typeface.BOLD && style != Typeface.ITALIC
                        && style != Typeface.BOLD_ITALIC) continue;
                safe.put("kind", "style");
                safe.put("value", style);
            } else {
                continue;
            }
            safe.put("start", start);
            safe.put("end", end);
            clean.put(safe);
        }
        return clean;
    }

    private static String sanitiseTitle(String value) {
        String title = value == null ? "" : value.trim();
        title = title.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "");
        if (title.isBlank()) title = "Untitled";
        if (title.length() > MAX_TITLE_CHARS)
            title = title.substring(0, MAX_TITLE_CHARS);
        return title;
    }

    private static boolean isKnownFont(String value) {
        if (value == null) return false;
        for (String font : FontManager.labels()) {
            if (font.equals(value)) return true;
        }
        return false;
    }
}
