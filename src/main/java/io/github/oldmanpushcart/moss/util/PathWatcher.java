package io.github.oldmanpushcart.moss.util;

import java.io.IOException;
import java.nio.file.*;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * 文件路径监听器
 */
public class PathWatcher {

    private final Path root;
    private final Listener listener;

    private volatile boolean isRunning = false;
    private volatile WatchService service;
    private volatile Thread tasker;

    private PathWatcher(Builder builder) {
        this.root = builder.root;
        this.listener = builder.listener;
    }

    /**
     * 启动监听
     *
     * @throws IOException 监听失败
     */
    public synchronized void start() throws IOException {

        if (isRunning) {
            throw new IllegalStateException("PathWatcher is running");
        }

        /*
         * 这里对目录进行监听，当目录发生变化时，会触发事件，然后进行后续处理
         * 1. 优先注册，确保启动过程中有增量变化能被感知
         * 2. 然后遍历，检索所有已存在的文件能被感知
         * 3. 过程中会有可能出现文件被重复监听到事件，交给上游处理
         * 4. 启动过程中如果失败，需要确保对应的资源能在异常处理中回收
         */
        try {
            service = FileSystems.getDefault().newWatchService();
            register(root);
            scanner();
        } catch (IOException ioEx) {
            closeQuietly(service);
            throw ioEx;
        }

        // 启动监听线程
        tasker = new Thread(this::process);
        tasker.setDaemon(true);
        tasker.setName("PathWatcher-Tasker-%s".formatted(root));
        tasker.start();

        // 标记为已启动
        isRunning = true;

    }

    /**
     * 停止监听
     */
    public synchronized void stop() {

        if (!isRunning) {
            throw new IllegalStateException("PathWatcher is not running");
        }

        // 中断监听线程
        tasker.interrupt();
        tasker = null;

        // 关闭文件系统监听服务
        closeQuietly(service);

        // 标记为已停止
        isRunning = false;

    }

    // 处理监听
    private void process() {
        while (!Thread.currentThread().isInterrupted()) {
            try {

                // 阻塞等待事件产生
                final var key = service.take();

                // 还原文件路径
                final var path = (Path) key.watchable();

                // 处理Path所有的事件
                key.pollEvents()
                        .forEach(wEvent -> {
                            final var kind = wEvent.kind();
                            final var source = path.resolve((Path) wEvent.context());
                            try {
                                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    register(source);
                                    processEvent(new Event(Type.CREATE, source));
                                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    processEvent(new Event(Type.MODIFY, source));
                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    processEvent(new Event(Type.DELETE, source));
                                }
                            } catch (Throwable ex) {
                                listener.onError(path, ex);
                            }
                        });

                // 重新注册，这样才能接收后续的事件
                key.reset();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // 注册当前目录及其子目录的监听器
    private void register(Path root) throws IOException {
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        root.register(
                service,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        try (final var stream = Files.newDirectoryStream(root)) {
            for (final var path : stream) {
                register(path);
            }
        }
    }

    // 扫描当前目录及其子目录下已存在的文件
    private void scanner() throws IOException {
        try (final var stream = Files.walk(root)) {
            stream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    processEvent(new Event(Type.EXISTS, path));
                }
            });
        }
    }

    // 处理事件
    private void processEvent(Event event) {
        try {
            listener.onEvent(event);
        } catch (Throwable ex) {
            listener.onError(event.source(), ex);
        }
    }

    /**
     * 事件类型
     */
    public enum Type {
        EXISTS,
        CREATE,
        MODIFY,
        DELETE,
    }

    /**
     * 事件
     *
     * @param type   事件类型
     * @param source 事件源
     */
    public record Event(Type type, Path source) {

    }

    /**
     * 监听器
     */
    public interface Listener {

        void onEvent(Event event) throws IOException;

        void onError(Path path, Throwable cause);
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Path root;
        private Listener listener;

        public Builder root(Path root) {
            this.root = root;
            return this;
        }

        public Builder listener(Listener listener) {
            this.listener = listener;
            return this;
        }

        public PathWatcher build() {
            return new PathWatcher(this);
        }

    }

    public static void main(String... args) throws IOException, InterruptedException {
        final var watcher = PathWatcher.newBuilder()
                .root(Path.of("./data/knowledge"))
                .listener(new Listener() {

                    @Override
                    public void onEvent(Event event) {
                        switch (event.type()) {
                            case EXISTS -> System.out.printf("[EXISTS] %s%n", event.source());
                            case CREATE -> System.out.printf("[CREATE] %s%n", event.source());
                            case MODIFY -> System.out.printf("[MODIFY] %s%n", event.source());
                            case DELETE -> System.out.printf("[DELETE] %s%n", event.source());
                        }
                    }

                    @Override
                    public void onError(Path path, Throwable cause) {
                        cause.printStackTrace();
                    }

                })
                .build();

        watcher.start();
        Thread.sleep(1000 * 60 * 5);
        watcher.stop();

    }

}
