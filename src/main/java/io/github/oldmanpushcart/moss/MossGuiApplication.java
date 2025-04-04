package io.github.oldmanpushcart.moss;

import io.github.oldmanpushcart.moss.gui.controller.SplashController;
import io.github.oldmanpushcart.moss.gui.controller.chat.ChatController;
import io.github.oldmanpushcart.moss.infra.boot.BootEvent;
import io.github.oldmanpushcart.moss.infra.memory.Memory;
import io.github.oldmanpushcart.moss.util.PlatformUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
public class MossGuiApplication extends Application {

    private static final Image ICON_IMAGE = new Image(Objects.requireNonNull(MossGuiApplication.class.getResourceAsStream("/gui/statics/image/moss-icon.png")));
    private volatile ConfigurableApplicationContext springCtx;

    @Override
    public void start(Stage primaryStage) throws Exception {
        displaySplashStage()
                .thenCompose(unused -> displayMainStage(primaryStage))
                .whenComplete((v, ex) -> {
                    if (null != ex) {
                        log.error("moss://startup failed!", ex);
                        Platform.exit();
                    } else {
                        log.debug("moss://startup completed!");
                    }
                });
    }

    // 显示启动窗口
    private CompletionStage<?> displaySplashStage() throws Exception {

        final var loader = new FXMLLoader(getClass().getResource("/gui/fxml/splash.fxml"));
        final var root = loader.<Parent>load();
        final var controller = loader.<SplashController>getController();

        final var stage = new Stage();
        stage.getIcons().add(ICON_IMAGE);
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        return loadingSpringCtx(controller)

                // spring启动完成后隐藏splash
                .thenAccept(unused -> Platform.runLater(stage::close));

    }

    // 加载Spring容器
    private CompletionStage<?> loadingSpringCtx(SplashController controller) {
        return CompletableFuture.runAsync(() -> {

            final var preLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(MossApplication.class.getClassLoader());
            try {
                final var arguments = getParameters().getRaw().toArray(new String[0]);
                final var springApp = new SpringApplication(MossApplication.class);
                springApp.addListeners(

                        // 监听启动开始
                        (ApplicationListener<ApplicationStartingEvent>) event ->
                                Platform.runLater(controller::updateProgressBegin),

                        // 监听启动进度
                        (ApplicationListener<BootEvent>) event ->
                                Platform.runLater(() -> controller.updateProgress(event.progress(), event.tips())),

                        // 监听启动完成
                        (ApplicationListener<ApplicationReadyEvent>) event ->
                                Platform.runLater(controller::updateProgressFinish)

                );
                springCtx = springApp.run(arguments);
            } finally {
                Thread.currentThread().setContextClassLoader(preLoader);
            }
        });
    }

    // 显示主窗口
    private CompletionStage<?> displayMainStage(Stage stage) {
        final var chatController = springCtx.getBean(ChatController.class);
        return PlatformUtils

                // 加载主界面
                .runOnPlatform(() -> {

                    // 加载ChatController
                    final var loader = new FXMLLoader(getClass().getResource("/gui/fxml/chat/chat.fxml"));
                    loader.setControllerFactory(clazz -> chatController);
                    loader.load();

                    // 锁定操作面板
                    chatController.lockControlPane();

                    // 渲染主界面
                    stage.getIcons().clear();
                    stage.getIcons().add(ICON_IMAGE);
                    stage.setTitle("MOSS - 人类的渺小，是伟大的开始！");
                    stage.setScene(new Scene(loader.getRoot()));
                    stage.initStyle(StageStyle.DECORATED);
                    stage.setResizable(true);
                    stage.centerOnScreen();
                    stage.setWidth(1024);
                    stage.setHeight(800);
                    stage.show();

                })

                // 加载记忆体
                .thenCompose(unused -> {
                    final var memory = springCtx.getBean(Memory.class);
                    final var fragments = memory.recall();
                    return PlatformUtils.runOnPlatform(() -> {

                        // 渲染记忆体内容
                        chatController.loadingMemory(fragments);

                        // 解锁操作面板
                        chatController.unlockControlPane();

                    });
                });

    }


    @Override
    public void stop() {
        if (null != springCtx) {
            springCtx.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
