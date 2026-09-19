package uk.brimstone.simpletype;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentRepository {
    private static final String EXT = ".stype";
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}'’_-]*");
    private final File dir;

    public DocumentRepository(Context context) {
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (root == null) root = context.getFilesDir();
        dir = new File(root, "SimpleType");
        if (!dir.exists()) dir.mkdirs();
    }

    public File getDirectory() {
        return dir;
    }

    public String create(String requestedTitle) throws Exception {
        String title = cleanTitle(requestedTitle);
        String stem = safeFileStem(title);
        File f = new File(dir, stem + EXT);
        int n = 2;
        while (f.exists()) f = new File(dir, stem + " (" + n++ + ")" + EXT);

        DocumentData d = new DocumentData();
        d.title = title;
        save(f.getName(), d);
        return f.getName();
    }

    public synchronized DocumentData load(String fileName) throws Exception {
        File f = fileFor(fileName);
        String raw = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        return DocumentData.fromJson(raw);
    }

    public synchronized void save(String fileName, DocumentData data) throws Exception {
        File target = fileFor(fileName);
        File temp = new File(dir, target.getName() + ".tmp");
        byte[] bytes = data.toJson().toString(2).getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(bytes);
            out.flush();
            out.getFD().sync();
        }
        if (target.exists() && !target.delete())
            throw new IllegalStateException("Could not replace " + target.getName());
        if (!temp.renameTo(target))
            throw new IllegalStateException("Could not save " + target.getName());
    }

    public void delete(String fileName) {
        File f = fileFor(fileName);
        if (f.exists()) f.delete();
    }

    public List<DocInfo> list() {
        List<DocInfo> result = new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(EXT));
        if (files == null) return result;
        for (File f : files) {
            try {
                DocumentData d = load(f.getName());
                result.add(new DocInfo(
                        f.getName(), d.title, f.lastModified(), countWords(d.text)));
            } catch (Exception ignored) {
                result.add(new DocInfo(f.getName(), f.getName(), f.lastModified(), 0));
            }
        }
        return result;
    }

    public int totalWordCount() {
        int total = 0;
        for (DocInfo d : list()) total += d.wordCount;
        return total;
    }

    public static int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        int count = 0;
        Matcher m = WORD.matcher(text);
        while (m.find()) count++;
        return count;
    }

    private File fileFor(String fileName) {
        String onlyName = new File(fileName).getName();
        return new File(dir, onlyName);
    }

    private static String cleanTitle(String title) {
        String t = title == null ? "" : title.trim();
        return t.isEmpty() ? "Untitled" : t;
    }

    private static String safeFileStem(String title) {
        String stem = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (stem.isEmpty()) stem = "Untitled";
        if (stem.length() > 80) stem = stem.substring(0, 80);
        return stem;
    }

    public static final class DocInfo {
        public final String fileName;
        public final String title;
        public final long modified;
        public final int wordCount;

        public DocInfo(String fileName, String title, long modified, int wordCount) {
            this.fileName = fileName;
            this.title = title;
            this.modified = modified;
            this.wordCount = wordCount;
        }

        @Override public String toString() {
            return String.format(Locale.UK, "%s · %,d words", title, wordCount);
        }
    }
}
