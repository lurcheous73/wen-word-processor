package uk.brimstone.simpletype;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalDictionary {
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}][\\p{L}'’-]*");
    private final Context context;
    private final Set<String> words = new HashSet<>();
    private volatile boolean loaded;

    public LocalDictionary(Context context) {
        this.context = context.getApplicationContext();
    }

    public void load() throws Exception {
        if (loaded) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("dictionary/en_gb_words.txt"),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                if (!word.isEmpty()) words.add(word);
            }
        }
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean contains(String word) {
        if (!loaded || word == null || word.isBlank()) return true;
        return words.contains(word.toLowerCase(Locale.UK));
    }

    public void markParagraph(RichEditText editor) {
        if (!loaded) return;
        Editable text = editor.getText();
        int cursor = Math.max(0, editor.getSelectionStart());
        int start = cursor;
        while (start > 0 && text.charAt(start - 1) != '\n') start--;
        int end = cursor;
        while (end < text.length() && text.charAt(end) != '\n') end++;

        DictionarySpan[] old = text.getSpans(start, end, DictionarySpan.class);
        for (DictionarySpan span : old) text.removeSpan(span);

        String paragraph = text.subSequence(start, end).toString();
        Matcher matcher = WORD.matcher(paragraph);
        while (matcher.find()) {
            int a = start + matcher.start();
            int b = start + matcher.end();
            String word = matcher.group();
            if (word.length() < 2) continue;

            boolean stillTyping = b == cursor
                    && (cursor == text.length()
                    || (cursor < text.length() && Character.isLetter(text.charAt(cursor))));
            if (stillTyping) continue;

            if (!contains(word)) {
                text.setSpan(new DictionarySpan(), a, b,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static final class DictionarySpan extends CharacterStyle
            implements UpdateAppearance {
        @Override public void updateDrawState(TextPaint tp) {
            tp.setUnderlineText(true);
            tp.underlineColor = 0xffa83a32;
            tp.underlineThickness = 1.5f;
        }
    }
}
