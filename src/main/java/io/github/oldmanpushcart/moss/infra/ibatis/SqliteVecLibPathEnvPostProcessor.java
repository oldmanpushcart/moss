package io.github.oldmanpushcart.moss.infra.ibatis;

import io.github.oldmanpushcart.moss.util.env.CpuArch;
import io.github.oldmanpushcart.moss.util.env.OsType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * SQLiteVec扩展库路径环境变量处理器
 */
public class SqliteVecLibPathEnvPostProcessor implements EnvironmentPostProcessor {

    private static final String SQLITE_VEC_LIB_KEY = "SQLITE_VEC_LIB";
    private static final String OS = OsType.CURRENT.name().toLowerCase();
    private static final String ARCH = CpuArch.CURRENT.name().toLowerCase();
    private static final String ROOT = System.getProperty("user.dir");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        final var filename = "vec0%s".formatted(
                switch (OsType.CURRENT) {
                    case WINDOWS -> ".dll";
                    case LINUX -> ".so";
                    case MACOS -> ".dylib";
                    default -> "";
                });
        final var resourcePath = "infra/lib/sqlite-vec/%s/%s/%s".formatted(OS, ARCH, filename);
        final var targetPath = Paths.get(ROOT, "lib", "sqlite-vec", OS, ARCH, filename);

        if (!Files.exists(targetPath)) {
            try {

                try (final var input = SqliteVecLibPathEnvPostProcessor.class.getClassLoader().getResourceAsStream(resourcePath)) {

                    // 如果没有找到对应的扩展，则说明无法支持当前操作系统或CPU架构
                    if (null == input) {
                        throw new RuntimeException("Load sqlite/sqlite-vec failed: unsupported OS=%s;ARCH=%s;".formatted(
                                OsType.CURRENT,
                                CpuArch.CURRENT
                        ));
                    }

                    // 复制到目标路径
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);

                }
            } catch (IOException ioEx) {
                throw new RuntimeException("Load sqlite/sqlite-vec failed!", ioEx);
            }
        }

        environment.getPropertySources()
                .addLast(new MapPropertySource("dynamicSqliteProperties",
                        Map.of(
                                SQLITE_VEC_LIB_KEY,
                                targetPath.toFile().getAbsolutePath()
                        )));
    }

}
