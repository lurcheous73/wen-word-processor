package uk.brimstone.simpletype;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class RichEditText extends EditText {
    private boolean pendingBold;
    private boolean pendingItalic;
    private boolean pendingUnderline;
    private boolean applying;
    private int changedStart;
    private int changedCount;
    private String fontKey = FontManager.OPEN_DYSLEXIC;

    public RichEditText(Context context) {
        super(context);
        init();
    }

    public RichEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSingleLine(false);
        setHorizontallyScrolling(false);
        setInputType(EditorInfo.TYPE_CLASS_TEXT
                | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES
                | EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT);
        setTextSize(20f);
        setLineSpacing(0f, 1.25f);
        setPadding(dp(20), dp(20), dp(20), dp(40));
        setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        setDocumentFont(FontManager.OPEN_DYSLEXIC);

        addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                changedStart = start;
                changedCount = count;
            }

            @Override public void afterTextChanged(Editable e) {
                if (applying || changedCount <= 0) return;
                int end = Math.min(e.length(), changedStart + changedCount);
                if (end <= changedStart) return;
                applying = true;
                if (pendingBold) e.setSpan(new StyleSpan(Typeface.BOLD),
                        changedStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (pendingItalic) e.setSpan(new StyleSpan(Typeface.ITALIC),
                        changedStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (pendingUnderline) e.setSpan(new UnderlineSpan(),
                        changedStart, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                applying = false;
                changedCount = 0;
            }
        });
    }

    public boolean toggleBold() {
        return toggleStyle(Typeface.BOLD);
    }

    public boolean toggleItalic() {
        return toggleStyle(Typeface.ITALIC);
    }

    public boolean toggleUnderline() {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start != end) {
            Editable e = getText();
            UnderlineSpan[] spans = e.getSpans(Math.min(start, end), Math.max(start, end),
                    UnderlineSpan.class);
            if (spans.length > 0) {
                for (UnderlineSpan span : spans) e.removeSpan(span);
                pendingUnderline = false;
            } else {
                e.setSpan(new UnderlineSpan(), Math.min(start, end), Math.max(start, end),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pendingUnderline = true;
            }
        } else {
            pendingUnderline = !pendingUnderline;
        }
        return pendingUnderline;
    }

    private boolean toggleStyle(int style) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        boolean newState;
        if (start != end) {
            int a = Math.min(start, end);
            int b = Math.max(start, end);
            Editable e = getText();
            StyleSpan[] spans = e.getSpans(a, b, StyleSpan.class);
            boolean found = false;
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    e.removeSpan(span);
                    found = true;
                }
            }
            newState = !found;
            if (newState) e.setSpan(new StyleSpan(style), a, b,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            if (style == Typeface.BOLD) {
                pendingBold = !pendingBold;
                newState = pendingBold;
            } else {
                pendingItalic = !pendingItalic;
                newState = pendingItalic;
            }
        }
        if (style == Typeface.BOLD) pendingBold = newState;
        if (style == Typeface.ITALIC) pendingItalic = newState;
        return newState;
    }

    public void setDocumentFont(String key) {
        fontKey = key == null ? FontManager.OPEN_DYSLEXIC : key;
        setTypeface(FontManager.get(getContext(), fontKey));
    }

    public String getDocumentFont() {
        return fontKey;
    }

    public JSONArray serializeSpans() {
        JSONArray array = new JSONArray();
        Editable e = getText();
        for (StyleSpan span : e.getSpans(0, e.length(), StyleSpan.class)) {
            JSONObject o = new JSONObject();
            try {
                o.put("kind", "style");
                o.put("value", span.getStyle());
                o.put("start", e.getSpanStart(span));
                o.put("end", e.getSpanEnd(span));
                array.put(o);
            } catch (Exception ignored) {}
        }
        for (UnderlineSpan span : e.getSpans(0, e.length(), UnderlineSpan.class)) {
            JSONObject o = new JSONObject();
            try {
                o.put("kind", "underline");
                o.put("start", e.getSpanStart(span));
                o.put("end", e.getSpanEnd(span));
                array.put(o);
            } catch (Exception ignored) {}
        }
        return array;
    }

    public void restoreSpans(JSONArray array) {
        if (array == null) return;
        Editable e = getText();
        applying = true;
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            int start = Math.max(0, Math.min(e.length(), o.optInt("start")));
            int end = Math.max(start, Math.min(e.length(), o.optInt("end")));
            if (end <= start) continue;
            if ("style".equals(o.optString("kind"))) {
                e.setSpan(new StyleSpan(o.optInt("value", Typeface.NORMAL)),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if ("underline".equals(o.optString("kind"))) {
                e.setSpan(new UnderlineSpan(), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        applying = false;
    }

    public boolean isPendingBold() { return pendingBold; }
    public boolean isPendingItalic() { return pendingItalic; }
    public boolean isPendingUnderline() { return pendingUnderline; }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
