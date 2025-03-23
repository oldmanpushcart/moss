package io.github.oldmanpushcart.moss;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@EnableScheduling
@SpringBootApplication
public class MossApplication {

    static {
        copySqliteVecExt();
    }

    private static void copySqliteVecExt() {
        final var sourcePath = "sqlite-vec/vec0.dll";
        final var targetPath = Paths.get(System.getProperty("user.dir"), "sqlite-vec", "vec0.ext");
        if (!Files.exists(targetPath)) {
            try {
                Files.createDirectories(targetPath.getParent());
                try (final var input = Objects.requireNonNull(MossApplication.class.getClassLoader().getResourceAsStream(sourcePath))) {
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ioEx) {
                throw new RuntimeException("Failed to copy sqlite-vec/vec0.dll", ioEx);
            }
        }
    }

}
