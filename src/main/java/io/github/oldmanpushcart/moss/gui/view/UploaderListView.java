package io.github.oldmanpushcart.moss.gui.view;

import io.github.oldmanpushcart.moss.infra.uploader.UploadEntry;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

public class UploaderListView extends AnchorPane {

    @FXML
    private TableView<Item> uploaderTable;

    @FXML
    private TableColumn<Item, CheckBox> selectCol;

    @FXML
    private TableColumn<Item, String> mimeCol;

    @FXML
    private TableColumn<Item, String> sizeCol;

    @FXML
    private TableColumn<Item, String> modelCol;

    @FXML
    private TableColumn<Item, String> sourceCol;

    @FXML
    private TableColumn<Item, String> uploadCol;

    @FXML
    private TableColumn<Item, String> expiresAtCol;

    @FXML
    private TableColumn<Item, String> createdAtCol;

    @FXML
    private TableColumn<Item, String> updatedAtCol;

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

    private Supplier<List<UploadEntry>> loadAction;
    private Consumer<List<UploadEntry>> deleteAction;

    public UploaderListView() {
        final var loader = new FXMLLoader(getClass().getResource("/gui/fxml/chat/uploader-list-view.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置删除选中项的动作
     *
     * @param action 删除动作
     * @return this
     */
    public UploaderListView setOnDeleteAction(Consumer<List<UploadEntry>> action) {
        this.deleteAction = action;
        return this;
    }

    /**
     * 设置加载表格数据的动作
     *
     * @param action 加载动作
     * @return this
     */
    public UploaderListView setOnLoadAction(Supplier<List<UploadEntry>> action) {
        this.loadAction = action;
        return this;
    }

    /**
     * 加载表格数据
     *
     * @return this
     */
    public UploaderListView load() {
        flushButton.fire();
        return this;
    }

    @FXML
    private void initialize() {

        initializeTableColumns();

        // 全选
        selectAllButton.setOnAction(event -> {
            for (final var item : uploaderTable.getItems()) {
                item.checkBoxProperty().get().setSelected(true);
            }
        });

        // 取消全选
        deselectAllButton.setOnAction(event -> {
            for (final var item : uploaderTable.getItems()) {
                item.checkBoxProperty().get().setSelected(false);
            }
        });

        // 刷新表格
        flushButton.setOnAction(event -> {
            uploaderTable.getItems().clear();
            if (loadAction != null) {
                final var entries = loadAction.get();
                if (entries != null) {
                    uploaderTable.getItems().addAll(entries.stream().map(Item::new).toList());
                }
            }
        });

        // 删除选中项
        deleteSelectedButton.setOnAction(event -> {
            if (deleteAction != null) {
                final var deletes = uploaderTable.getItems()
                        .stream()
                        .filter(Item::isSelected)
                        .toList();
                final var entries = deletes.stream()
                        .filter(Item::isSelected)
                        .map(Item::entry)
                        .toList();
                deleteAction.accept(entries);
                uploaderTable.getItems().removeAll(deletes);
            }
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

        mimeCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(item.entry().mime());
        });

        sizeCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(byteCountToDisplaySize(item.entry().length()));
        });

        modelCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(item.entry().model());
        });

        sourceCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(String.valueOf(item.entry().source()));
        });

        uploadCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return new SimpleStringProperty(String.valueOf(item.entry().upload()));
        });

        expiresAtCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            if(null != item.entry().expiresAt()) {
                final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        .withZone(ZoneId.systemDefault());
                return new SimpleStringProperty(formatter.format(item.entry().expiresAt()));
            } else {
                return new SimpleStringProperty("永久生效");
            }
        });

        createdAtCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            return new SimpleStringProperty(formatter.format(item.entry().createdAt()));
        });

        updatedAtCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            return new SimpleStringProperty(formatter.format(item.entry().updatedAt()));
        });

        optsCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return item.optsNodeProperty();
        });

    }


    /**
     * 表格项
     */
    @Getter()
    @Accessors(fluent = true)
    @AllArgsConstructor
    private final class Item {

        private final UploadEntry entry;

        private final ObjectProperty<CheckBox> checkBoxProperty = new SimpleObjectProperty<>(
                new CheckBox() {{
                    setSelected(true);
                }});

        private final ObjectProperty<Node> optsNodeProperty = new SimpleObjectProperty<>(
                new HBox() {{
                    getChildren().addAll(
                            new Hyperlink() {{
                                setText("删除");
                                setOnAction(event -> {
                                    if (deleteAction != null) {
                                        deleteAction.accept(List.of(Item.this.entry));
                                        uploaderTable.getItems().remove(Item.this);
                                    }
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
