package io.github.oldmanpushcart.moss.gui;

import io.github.oldmanpushcart.moss.gui.controller.ChatController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DebugGuiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        final var loader = new FXMLLoader(getClass().getResource("/gui/fxml/chat/chat.fxml"));
        final var root = loader.<Parent>load();
        final var controller = loader.<ChatController>getController();

        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
