package uk.brimstone.simpletype;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentRepository {
    private static final String EXT = ".stype";
    private static final long MAX_FILE_BYTES = 24L * 1024L * 1024L;
    private static final Pattern WORD =
            Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}'’_-]*");
    private final File dir;

    public DocumentRepository(Context context) {
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (root == null) root = context.getFilesDir();
        dir = new File(root, "Wen");
        migrateLegacyFolder(new File(root, "SimpleType"));
        if (!dir.exists() && !dir.mkdirs())
            throw new IllegalStateException("Could not create Wen document folder");
        if (!dir.isDirectory())
            throw new IllegalStateException("Wen document location is not a folder");
        recoverTemporaryFiles();
    }

    public File getDirectory() {
        return dir;
    }

    private void migrateLegacyFolder(File legacy) {
        if (!legacy.exists() || legacy.equals(dir)) return;
        if (!dir.exists()) dir.mkdirs();
        File[] files = legacy.listFiles();
        if (files != null) {
            for (File source : files) {
                if (!source.isFile()
                        || !(source.getName().endsWith(EXT)
                        || source.getName().endsWith(EXT + ".tmp"))) continue;
                File target = new File(dir, source.getName());
                try {
                    if (!target.exists())
                        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception ignored) {}
            }
        }
        File[] remaining = legacy.listFiles();
        if (remaining != null && remaining.length == 0) legacy.delete();
    }

    private void recoverTemporaryFiles() {
        File[] temps = dir.listFiles((d, name) -> name.endsWith(EXT + ".tmp"));
        if (temps == null) return;
        for (File temp : temps) {
            String targetName = temp.getName().substring(0, temp.getName().length() - 4);
            File target = new File(dir, targetName);
            try {
                readValidated(temp);
            } catch (Exception invalidTemp) {
                preserveFile(temp, targetName + ".recovery-failed");
                continue;
            }

            try {
                if (!target.exists()) {
                    moveReplace(temp, target);
                    continue;
                }

                File preserved = preserveFile(target, targetName + ".pre-recovery");
                if (preserved == null)
                    continue;

                try {
                    moveReplace(temp, target);
                } catch (Exception promotionFailed) {
                    try {
                        moveReplace(preserved, target);
                    } catch (Exception ignored) {}
                    throw promotionFailed;
                }
            } catch (Exception ignored) {
                // Leave both files in place if recovery cannot be completed safely.
            }
        }
    }

    public String create(String requestedTitle) throws Exception {
        String title = cleanTitle(requestedTitle);
        String stem = safeFileStem(title);
        File f = new File(dir, stem + EXT);
        int n = 2;
        while (f.exists()) {
            if (n > 9999)
                throw new IllegalStateException("Too many documents with the same name");
            f = new File(dir, stem + " (" + n++ + ")" + EXT);
        }

        DocumentData d = new DocumentData();
        d.title = title;
        save(f.getName(), d);
        return f.getName();
    }

    public synchronized DocumentData load(String fileName) throws Exception {
        return readValidated(fileFor(fileName));
    }

    public synchronized void save(String fileName, DocumentData data) throws Exception {
        File target = fileFor(fileName);
        File temp = new File(dir, target.getName() + ".tmp");
        byte[] bytes = data.toJson().toString(2).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_BYTES)
            throw new IllegalStateException("Document is too large to save");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(bytes);
            out.flush();
            out.getFD().sync();
        }
        moveReplace(temp, target);
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
                result.add(new DocInfo(f.getName(),
                        "Could not open · " + f.getName(), f.lastModified(), 0));
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

    private DocumentData readValidated(File file) throws Exception {
        if (file == null || !file.isFile())
            throw new IllegalArgumentException("Document does not exist");
        long size = file.length();
        if (size <= 0 || size > MAX_FILE_BYTES)
            throw new IllegalArgumentException("Document size is invalid");
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length > MAX_FILE_BYTES)
            throw new IllegalArgumentException("Document is too large");
        return DocumentData.fromJson(new String(bytes, StandardCharsets.UTF_8));
    }

    private void moveReplace(File source, File target) throws Exception {
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File preserveFile(File source, String preferredName) {
        if (source == null || !source.exists()) return null;
        File preserved = new File(dir, preferredName);
        int suffix = 2;
        while (preserved.exists())
            preserved = new File(dir, preferredName + "." + suffix++);
        try {
            Files.move(source.toPath(), preserved.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return preserved;
        } catch (Exception ignored) {
            return null;
        }
    }

    private File fileFor(String fileName) {
        if (fileName == null || fileName.isBlank())
            throw new IllegalArgumentException("Missing document name");
        String onlyName = new File(fileName).getName();
        if (!onlyName.endsWith(EXT))
            throw new IllegalArgumentException("Unsupported document type");
        File target = new File(dir, onlyName);
        try {
            if (!target.getCanonicalFile().getParentFile().equals(dir.getCanonicalFile()))
                throw new SecurityException("Invalid document path");
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid document path", e);
        }
        return target;
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
