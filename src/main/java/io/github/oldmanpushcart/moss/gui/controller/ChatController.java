package io.github.oldmanpushcart.moss.gui.controller;

import io.github.oldmanpushcart.moss.gui.view.AttachmentListView;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

public class ChatController {


    @FXML
    private VBox messagesBox;

    @FXML
    private AttachmentListView attachmentListView;

    @FXML
    private ToggleButton autoSpeakToggleButton;

    @FXML
    private ToggleButton attachmentToggleButton;

    @FXML
    private TextArea inputTextArea;

    @FXML
    private ToggleButton enterToggleButton;

    @FXML
    private void initialize() {
        hiddenAttachmentListView();
        initializeAttachmentToggleButton();
    }

    private void hiddenAttachmentListView() {
        attachmentListView.setVisible(false);
        attachmentListView.setManaged(false);
    }

    private void initializeAttachmentToggleButton() {
        attachmentToggleButton.selectedProperty()
                .addListener((observable, oldValue, newValue) -> {
                    attachmentListView.setVisible(newValue);
                    attachmentListView.setManaged(newValue);
                });
    }

}
