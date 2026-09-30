package org.woheller69.gptassist;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PhotoContextFileStore {
    private static final long MAX_AGE_MS = 24L * 60L * 60L * 1000L;
    private final Context context;
    public PhotoContextFileStore(Context context) { this.context = context.getApplicationContext(); }

    public Uri write(String contents) throws IOException {
        File directory = new File(context.getCacheDir(), "photo_context");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Unable to create context cache");
        cleanup(directory);
        File output = new File(directory, "PHOTO_CONTEXT.txt");
        try (FileOutputStream stream = new FileOutputStream(output, false)) {
            stream.write(contents.getBytes(StandardCharsets.UTF_8));
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".photo_context", output);
    }

    public void cleanup() { cleanup(new File(context.getCacheDir(), "photo_context")); }
    private static void cleanup(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - MAX_AGE_MS;
        for (File file : files) if (file.lastModified() < cutoff) file.delete();
    }
}
