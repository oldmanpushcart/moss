package io.github.oldmanpushcart.moss;

import io.github.oldmanpushcart.moss.gui.controller.SplashController;
import io.github.oldmanpushcart.moss.infra.boot.BootEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class MossGuiApplication extends Application {

    private volatile ConfigurableApplicationContext springCtx;

    @Override
    public void start(Stage primaryStage) throws Exception {
        splashing();
    }

    private CompletionStage<?> splashing() throws Exception {

        final var loader = new FXMLLoader(getClass().getResource("/gui/fxml/splash.fxml"));
        final var root = loader.<Parent>load();
        final var controller = loader.<SplashController>getController();

        final var stage = new Stage();
        stage.setScene(new Scene(root));
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();


        return CompletableFuture

                // 异步启动spring
                .runAsync(() -> {
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
                })

                // spring启动完成后隐藏splash
                .thenAccept(unused -> Platform.runLater(stage::close));

    }

    @Override
    public void stop() throws Exception {
        if (null != springCtx) {
            springCtx.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
