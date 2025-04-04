package io.github.oldmanpushcart.moss.frontend.javafx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

public class SplashController {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label tipsLabel;

    public void updateProgress(double progress, String tips) {
        progressBar.setProgress(progress);
        tipsLabel.setText(tips);
    }

    public void updateProgressBegin() {
        updateProgress(0.0, "准备启动");
    }

    public void updateProgressFinish() {
        updateProgress(1.0, "启动完成");
    }

}
