package uk.brimstone.simpletype;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class AssetCopy {
    private AssetCopy() {}

    public static void copyTree(AssetManager assets, String assetPath, File destination)
            throws Exception {
        String[] children = assets.list(assetPath);
        if (children != null && children.length > 0) {
            if (!destination.exists() && !destination.mkdirs())
                throw new IllegalStateException("Cannot create " + destination);
            for (String child : children) {
                copyTree(assets, assetPath + "/" + child, new File(destination, child));
            }
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (InputStream in = assets.open(assetPath);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        }
    }
}
