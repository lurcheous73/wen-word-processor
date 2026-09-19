package uk.brimstone.simpletype;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DocumentData {
    public String title = "Untitled";
    public String text = "";
    public String fontKey = FontManager.OPEN_DYSLEXIC;
    public JSONArray spans = new JSONArray();

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("version", 1);
        o.put("title", title);
        o.put("text", text);
        o.put("font", fontKey);
        o.put("spans", spans);
        return o;
    }

    public static DocumentData fromJson(String raw) throws JSONException {
        JSONObject o = new JSONObject(raw);
        DocumentData d = new DocumentData();
        d.title = o.optString("title", "Untitled");
        d.text = o.optString("text", "");
        d.fontKey = o.optString("font", FontManager.OPEN_DYSLEXIC);
        d.spans = o.optJSONArray("spans");
        if (d.spans == null) d.spans = new JSONArray();
        return d;
    }
}
