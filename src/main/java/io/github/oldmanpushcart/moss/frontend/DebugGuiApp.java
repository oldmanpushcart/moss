package io.github.oldmanpushcart.moss.frontend;

import io.github.oldmanpushcart.moss.frontend.javafx.view.MessageView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DebugGuiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        final var messageView = new MessageView();


        primaryStage.setScene(new Scene(messageView));
        primaryStage.show();

        messageView.setContent("""
                ## 个人信息
                |项目|值|
                |---|---|
                |姓名|李夏驰|
                |邮箱|oldmanpushcart@gmail.com|
                |电话|13989838402|
                |主页|[Github](https://github.com/oldmanpushcart)|
                """
        );
    }

    public static void main(String[] args) {
        launch(args);
    }

}
