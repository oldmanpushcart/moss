package io.github.oldmanpushcart.moss.frontend.javafx.view;

import io.github.oldmanpushcart.moss.frontend.commonmark.extension.VideoLinkHtmlExtension;
import io.github.oldmanpushcart.moss.frontend.javafx.JsBridge;
import io.github.oldmanpushcart.moss.util.JacksonUtils;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class MessageView extends AnchorPane {

    private static final String PAGE = "/frontend/statics/html/message-view-index.html";

    private static final Parser markdownParser = Parser.builder()
            .extensions(List.of(
                    TablesExtension.create()
            ))
            .build();
    private static final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
            .extensions(List.of(
                    TablesExtension.create(),
                    VideoLinkHtmlExtension.create()
            ))
            .build();

    private final AtomicBoolean isPageLoaded = new AtomicBoolean(false);

    @Getter
    @Accessors(fluent = true)
    private final StringProperty contentProperty = new SimpleStringProperty(this, "Content");

    @Getter
    @Accessors(fluent = true)
    private final BooleanProperty buttonBarEnabledProperty = new SimpleBooleanProperty(this, "ButtonBar Enabled", true);

    @Getter
    @Accessors(fluent = true)
    private final BooleanProperty redoButtonEnabledProperty = new SimpleBooleanProperty(this, "Redo Button Enabled", true);

    @FXML
    private WebView contentWebView;

    @Getter
    @FXML
    private Button redoButton;

    @FXML
    private Button copyButton;

    @Getter
    @FXML
    private ToggleButton speakToggleButton;

    @FXML
    private HBox buttonBar;

    public MessageView() {
        final var loader = new FXMLLoader(getClass().getResource("/frontend/fxml/chat/message-view.fxml"));
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

        // 监听宽度变化，确保内容高度自适应
        widthProperty().addListener((observable, oldValue, newValue) -> {
            adjustContentViewHeight();
        });

        bindingContentProperty();
        bindingCopyButtonOnAction();
        bindingEnableButtonBarProperty();
        bindingEnableRedoButtonProperty();
        initializeContentWebView();
    }

    private void initializeContentWebView() {

        final var engine = contentWebView.getEngine();

        engine.setJavaScriptEnabled(true);

        // 加载JsBridge
        new JsBridge(contentWebView)

                /*
                 * 监听高度变化，并自适应高度
                 * 因为有些图片是异步加载的，加载之后会导致内容高度发生变化。
                 * 所以此时需要监听到内容高度的变化，反向通知WebView的高度进行调整
                 */
                .register("onDocumentElementScrollHeightChanged", (identity, argumentJson) -> {
                    adjustContentViewHeight();
                    return null;
                });

        /*
         * 内容页加载完毕，则标记isPageLoaded加载完成
         */
        engine.getLoadWorker().stateProperty()
                .addListener((obs, oldState, newState) -> {
                    if (newState == Worker.State.SUCCEEDED) {
                        isPageLoaded.set(true);
                    }
                });

        // 加载内容页
        engine.load(requireNonNull(getClass().getResource(PAGE)).toExternalForm());

        // 过滤掉body的Y轴滚屏事件，避免内容页滚动和页面滚动冲突
        contentWebView.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                event.consume();
                contentWebView.getParent().fireEvent(event);
            }
        });

    }

    // 绑定内容属性：修改内容，并自适应高度
    private void bindingContentProperty() {
        contentProperty
                .addListener((obs, oldValue, newValue) -> {

                    final var innerHtml = htmlRenderer.render(markdownParser.parse(newValue));
                    final var escapedHtml = JacksonUtils.toJson(innerHtml);

                    executeScriptSafely(() -> {

                        final var engine = contentWebView.getEngine();

                        // 修改聊天内容
                        engine.executeScript("document.getElementById('message-content').innerHTML = %s;"
                                .formatted(
                                        escapedHtml
                                ));

                        // 自适应高度
                        adjustContentViewHeight();

                    });

                });
    }

    // 自适应WebView的内容高度
    private void adjustContentViewHeight() {
        final var engine = contentWebView.getEngine();
        executeScriptSafely(() -> {
            final var height = (Integer) engine.executeScript("document.documentElement.scrollHeight;");
            contentWebView.setPrefHeight(height.doubleValue());
        });
    }

    // 绑定按钮栏：显示/隐藏
    private void bindingEnableButtonBarProperty() {
        buttonBar.visibleProperty().bind(buttonBarEnabledProperty);
        buttonBar.managedProperty().bind(buttonBarEnabledProperty);
    }

    // 绑定重做按钮：显示/隐藏
    private void bindingEnableRedoButtonProperty() {
        redoButton.visibleProperty().bind(redoButtonEnabledProperty);
        redoButton.managedProperty().bind(redoButtonEnabledProperty);
    }

    // 绑定复制按钮：复制到剪贴板
    private void bindingCopyButtonOnAction() {
        copyButton.setOnAction(event -> {
            final var content = new ClipboardContent();
            content.putString(contentProperty.get());
            Clipboard.getSystemClipboard().setContent(content);
        });
    }

    /**
     * 执行脚本
     *
     * @param scriptExecutor 脚本执行器
     */
    private void executeScriptSafely(Runnable scriptExecutor) {
        if (isPageLoaded.get()) {
            Platform.runLater(scriptExecutor);
        } else {
            contentWebView.getEngine().getLoadWorker().stateProperty()
                    .addListener((obs, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            Platform.runLater(scriptExecutor);
                        }
                    });
        }
    }

    public String getContent() {
        return contentProperty.get();
    }

    public void setContent(String content) {
        contentProperty.set(content);
    }

    public void setContent(CharSequence content) {
        setContent(content.toString());
    }

    public boolean isButtonBarEnabled() {
        return buttonBarEnabledProperty.get();
    }

    public void setButtonBarEnabled(boolean enable) {
        buttonBarEnabledProperty.set(enable);
    }

    public boolean isRedoButtonEnabled() {
        return redoButtonEnabledProperty.get();
    }

    public void setRedoButtonEnabled(boolean enable) {
        redoButtonEnabledProperty.set(enable);
    }

}
