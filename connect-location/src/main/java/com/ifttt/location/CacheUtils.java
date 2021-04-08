package com.ifttt.location;

import android.content.Context;
import com.ifttt.connect.analytics.tape.QueueFile;
import java.io.File;
import java.io.IOException;

final class CacheUtils {

    static QueueFile createQueueFile(Context context, String fileName) throws IOException {
        File folder = context.getFilesDir();
        if (!(folder.exists() || folder.mkdirs() || folder.isDirectory())) {
            throw new IOException("Could not create directory at " + folder);
        }

        File file = new File(folder, fileName);
        try {
            return new QueueFile.Builder(file).build();
        } catch (IOException e) {
            if (file.delete()) {
                return new QueueFile.Builder(file).build();
            } else {
                throw new IOException("Could not create queue file (" + fileName + ") in " + folder + ".");
            }
        }
    }

    private CacheUtils() {
        throw new AssertionError();
    }
}
