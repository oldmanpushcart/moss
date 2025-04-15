package io.github.oldmanpushcart.moss;

import io.github.oldmanpushcart.moss.backend.boot.BootEvent;
import io.github.oldmanpushcart.moss.backend.memory.Memory;
import io.github.oldmanpushcart.moss.frontend.javafx.controller.SplashController;
import io.github.oldmanpushcart.moss.frontend.javafx.controller.chat.ChatController;
import io.github.oldmanpushcart.moss.frontend.util.PlatformUtils;
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
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class MossApplication extends Application {

    private static final Image ICON_IMAGE = new Image(Objects.requireNonNull(MossApplication.class.getResourceAsStream("/frontend/statics/image/moss-logo.png")));
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

        final var loader = new FXMLLoader(getClass().getResource("/frontend/fxml/splash.fxml"));
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
        return CompletableFuture
                .runAsync(() -> {

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

        return PlatformUtils

                // 加载主界面
                .runLaterOnPlatform(() -> {

                    // 加载ChatController
                    final var loader = new FXMLLoader(getClass().getResource("/frontend/fxml/chat/chat.fxml"));
                    loader.setControllerFactory(springCtx::getBean);
                    loader.load();

                    final var chatController = loader.<ChatController>getController();
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

                    return chatController;
                })

                // 异步加载记忆
                .thenAccept(chatController -> {
                    final var fragments = springCtx.getBean(Memory.class).recall();
                    Platform.runLater(() -> {
                        chatController.loadingMemory(fragments);
                        chatController.unlockControlPane();
                        chatController.focusInputText();
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
