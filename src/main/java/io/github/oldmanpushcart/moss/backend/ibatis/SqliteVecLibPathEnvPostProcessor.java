package io.github.oldmanpushcart.moss.backend.ibatis;

import io.github.oldmanpushcart.moss.util.env.ArchType;
import io.github.oldmanpushcart.moss.util.env.OsType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * SQLiteVec扩展库路径环境变量处理器
 */
public class SqliteVecLibPathEnvPostProcessor implements EnvironmentPostProcessor {

    private static final String SQLITE_VEC_LIB_KEY = "SQLITE_VEC_LIB";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        try {

            // 加载SQLiteVec扩展库
            final var path = load();

            // 在springboot中设置环境变量
            environment.getPropertySources()
                    .addLast(new MapPropertySource("dynamicSqliteProperties",
                            Map.of(
                                    SQLITE_VEC_LIB_KEY,
                                    path.toString()
                            )));

        } catch (IOException ioEx) {
            throw new RuntimeException("Load sqlite-vec lib occur error!", ioEx);
        }

    }

    /**
     * 加载SQLiteVec扩展库
     *
     * @return 路径
     * @throws IOException 加载失败
     */
    private static Path load() throws IOException {

        // 根据操作系统类型确定后缀
        final var suffix = switch (OsType.CURRENT) {
            case WINDOWS -> "dll";
            case LINUX -> "so";
            case MACOS -> "dylib";
            default -> throw new IOException("Unsupported OS: %s".formatted(OsType.CURRENT));
        };

        // 确定库文件名
        final var filename = "vec0.%s".formatted(suffix);

        // 确定库资源路径
        final var resourceURLString = "backend/lib/sqlite-vec/%s/%s/%s".formatted(OsType.CURRENT, ArchType.CURRENT, filename);

        // 从资源中加载
        try (final var input = SqliteVecLibPathEnvPostProcessor.class.getClassLoader().getResourceAsStream(resourceURLString)) {

            // 如果没有找到对应的扩展，则说明无法支持当前操作系统或CPU架构
            if (null == input) {
                throw new IOException("Unsupported OS:%s ARCH:%s;".formatted(
                        OsType.CURRENT,
                        ArchType.CURRENT
                ));
            }

            // 创建临时文件作为sqlite-vec的库文件
            final var targetPath = Files.createTempDirectory("moss-sqlite-vec-").resolve(filename);

            // 复制资源到临时库文件
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 返回加载好的库文件
            return targetPath;
        }

    }


}
