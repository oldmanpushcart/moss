package io.github.oldmanpushcart.moss.util;

import java.io.File;
import java.nio.file.Files;

public class FileUtils {

    public static String probeContentType(File file) {
        try {
            final var mime = Files.probeContentType(file.toPath());
            if (null == mime || mime.isBlank()) {
                throw new IllegalArgumentException();
            }
            return mime;
        } catch (Throwable e) {
            return "application/octet-stream";
        }
    }

}
