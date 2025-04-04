package io.github.oldmanpushcart.moss.frontend.javafx.view;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

/**
 * 附件列表视图组件
 */
public class AttachmentListView extends AnchorPane {

    @FXML
    private TableView<Item> attachmentTable;

    @FXML
    private TableColumn<Item, CheckBox> selectCol;

    @FXML
    private TableColumn<Item, String> pathCol;

    @FXML
    private TableColumn<Item, String> sizeCol;

    @FXML
    private TableColumn<Item, Node> optsCol;

    public AttachmentListView() {
        final var loader = new FXMLLoader(getClass().getResource("/frontend/fxml/chat/attachment-list-view.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void initialize() {
        initializeTableColumns();
        autoResizeColumns();
        enableDragDrop();
    }

    /*
     * 初始化表格列
     */
    private void initializeTableColumns() {

        // 选择列
        selectCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return item.checkBoxProperty();
        });

        // 路径列
        pathCol.setCellFactory(col -> new TooltipStringTableCell<>());
        pathCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var file = item.file();
            return new SimpleStringProperty(file.getAbsolutePath());
        });

        // 大小列
        sizeCol.setCellFactory(col -> new TooltipStringTableCell<>());
        sizeCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            final var file = item.file();
            return new SimpleStringProperty(byteCountToDisplaySize(file.length()));
        });

        // 操作列
        optsCol.setCellValueFactory(data -> {
            final var item = data.getValue();
            return item.optsNodeProperty();
        });

    }

    /*
     * 自动扩展路径列
     */
    private void autoResizeColumns() {

        // 自动扩展路径列
        pathCol.prefWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        new Callable<>() {

                            @Override
                            public Double call() {
                                return computeEffectiveWidth()
                                       - attachmentTable.getPadding().getLeft()
                                       - attachmentTable.getPadding().getRight()
                                       - selectCol.getWidth()
                                       - sizeCol.getWidth()
                                       - optsCol.getWidth();
                            }

                            private double computeEffectiveWidth() {
                                final var width = attachmentTable.getWidth();
                                final var scrollBar = attachmentTable.lookup(".scroll-bar:vertical");
                                final var scrollBarWidth = (scrollBar != null && scrollBar.isVisible())
                                        ? scrollBar.getBoundsInLocal().getWidth()
                                        : 0;
                                return width - scrollBarWidth;
                            }

                        },
                        attachmentTable.widthProperty()
                ));

    }

    /*
     * 启用拖拽
     */
    private void enableDragDrop() {

        // 拖入
        attachmentTable.setOnDragOver(event -> {
            final var dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // 松手
        attachmentTable.setOnDragDropped(event -> {
            final var dragboard = event.getDragboard();
            if (dragboard.hasFiles()) {
                dragboard.getFiles().forEach(file -> {
                    final var item = new Item(file);
                    final var items = attachmentTable.getItems();
                    if (!items.contains(item)) {
                        items.add(item);
                    }
                });
            }
            event.setDropCompleted(true);
            event.consume();
        });

    }

    /**
     * 获取选中的文件
     *
     * @return 文件列表
     */
    public List<File> selected() {
        return attachmentTable.getItems().stream()
                .filter(Item::isSelected)
                .map(Item::file)
                .toList();
    }

    /**
     * 表格项
     */
    @Getter()
    @Accessors(fluent = true)
    @AllArgsConstructor
    private final class Item {

        private final File file;

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
                                setOnAction(event -> attachmentTable.getItems().remove(Item.this));
                            }}
                    );
                }});

        public boolean isSelected() {
            return checkBoxProperty.get().isSelected();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Item item && item.file.equals(file);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

    }

}
