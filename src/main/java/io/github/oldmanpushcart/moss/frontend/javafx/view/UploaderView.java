package io.github.oldmanpushcart.moss.frontend.javafx.view;

import io.github.oldmanpushcart.moss.backend.uploader.Uploader;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

public class UploaderView extends AnchorPane {

    @FXML
    private TableView<Item> uploaderTable;

    @FXML
    private TableColumn<Item, CheckBox> selectCol;

    @FXML
    private TableColumn<Item, String> lengthCol;

    @FXML
    private TableColumn<Item, String> modelCol;

    @FXML
    private TableColumn<Item, String> filenameCol;

    @FXML
    private TableColumn<Item, String> uploadedCol;

    @FXML
    private TableColumn<Item, String> expiresAtCol;

    @FXML
    private TableColumn<Item, String> createdAtCol;

    @FXML
    private TableColumn<Item, Node> optsCol;

    @FXML
    private Button deleteSelectedButton;

    @FXML
    private Button selectAllButton;

    @FXML
    private Button deselectAllButton;

    @FXML
    private Button flushButton;

    @Setter
    @Accessors(chain = true)
    private Uploader uploader;

    private final BooleanProperty enabledProperty = new SimpleBooleanProperty();

    public UploaderView() {
        final var loader = new FXMLLoader(getClass().getResource("/frontend/fxml/chat/uploader-view.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void lock() {
        setDisable(true);
    }

    private void unlock() {
        setDisable(false);
    }

    /**
     * 加载表格数据
     */
    public void load() {
        lock();
        try {
            final var items = uploader.listAll().stream()
                    .map(Item::new)
                    .toList();
            uploaderTable.getItems().clear();
            uploaderTable.getItems().addAll(items);
        } finally {
            unlock();
        }
    }

    public UploaderView bindEnabledProperty(BooleanProperty enabledProperty) {
        this.enabledProperty.bind(enabledProperty);
        return this;
    }

    @FXML
    private void initialize() {

        initializeTableColumns();
        bindingEnabledProperty();

        // 全选
        selectAllButton.setOnAction(event ->
                uploaderTable.getItems().forEach(item ->
                        item.checkBoxProperty().get().setSelected(true)));

        // 取消全选
        deselectAllButton.setOnAction(event ->
                uploaderTable.getItems().forEach(item ->
                        item.checkBoxProperty().get().setSelected(false)));

        // 刷新表格
        flushButton.setOnAction(event -> {
            lock();
            uploader.flush()
                    .thenApply(entries -> entries.stream()
                            .map(Item::new)
                            .toList())
                    .thenAccept(items->
                            Platform.runLater(() -> {
                                uploaderTable.getItems().clear();
                                uploaderTable.getItems().addAll(items);
                            }))
                    .whenComplete((items, ex) ->
                            Platform.runLater(this::unlock));
        });

        // 删除选中项
        deleteSelectedButton.setOnAction(event -> {
            final var items = uploaderTable.getItems();
            final var deleteItems = items
                    .stream()
                    .filter(Item::isSelected)
                    .toList();
            final var deleteEntryIds = deleteItems.stream()
                    .map(Item::entry)
                    .map(Uploader.Entry::entryId)
                    .toList();
            lock();
            uploader.deleteByIds(deleteEntryIds)
                    .whenComplete((unused, ex) ->
                            Platform.runLater(() -> {
                                if (null == ex) {
                                    items.removeAll(deleteItems);
                                }
                                unlock();
                            }));
        });

    }

    /*
     * 初始化表格列
     */
    private void initializeTableColumns() {

        selectCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return item.checkBoxProperty();
        });

        modelCol.setCellFactory(col -> new TooltipStringTableCell<>());
        modelCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(item.entry().model());
        });

        lengthCol.setCellFactory(col -> new TooltipStringTableCell<>());
        lengthCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(byteCountToDisplaySize(item.entry().length()));
        });

        filenameCol.setCellFactory(col -> new TooltipStringTableCell<>());
        filenameCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var filename = URLDecoder.decode(String.valueOf(item.entry().filename()), StandardCharsets.UTF_8);
            return new SimpleStringProperty(filename);
        });

        uploadedCol.setCellFactory(col -> new TooltipStringTableCell<>());
        uploadedCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(String.valueOf(item.entry().uploaded()));
        });

        expiresAtCol.setCellFactory(col -> new TooltipStringTableCell<>());
        expiresAtCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            if (null != item.entry().expiresAt()) {
                final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());
                return new SimpleStringProperty(formatter.format(item.entry().expiresAt()));
            } else {
                return new SimpleStringProperty("永久生效");
            }
        });

        createdAtCol.setCellFactory(col -> new TooltipStringTableCell<>());
        createdAtCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            return new SimpleStringProperty(formatter.format(item.entry().createdAt()));
        });

        optsCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return item.optsNodeProperty();
        });

    }

    private void bindingEnabledProperty() {
        visibleProperty().bind(enabledProperty);
        managedProperty().bind(enabledProperty);
        enabledProperty.addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                load();
            }
        });
    }

    /**
     * 表格项
     */
    @Getter()
    @Accessors(fluent = true)
    @AllArgsConstructor
    private final class Item {

        private final Uploader.Entry entry;

        private final ObjectProperty<CheckBox> checkBoxProperty = new SimpleObjectProperty<>(
                new CheckBox() {{
                    setSelected(true);
                }});

        private final ObjectProperty<Node> optsNodeProperty = new SimpleObjectProperty<>(
                new HBox() {{
                    getChildren().addAll(
                            new Hyperlink() {{
                                setText("删除");
                                setStyle("-fx-text-fill: #E34234; -fx-font-weight: bold;");
                                setOnAction(event -> {
                                    lock();
                                    uploader.delete(Item.this.entry.entryId())
                                            .whenComplete((unused, ex) ->
                                                    Platform.runLater(() -> {
                                                        if (null == ex) {
                                                            uploaderTable.getItems().remove(Item.this);
                                                        }
                                                        unlock();
                                                    }));
                                });
                            }}
                    );
                }});

        public boolean isSelected() {
            return checkBoxProperty.get().isSelected();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Item item && item.entry.entryId() == entry.entryId();
        }

        @Override
        public int hashCode() {
            return Long.hashCode(entry.entryId());
        }

    }

}
