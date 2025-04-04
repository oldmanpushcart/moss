package io.github.oldmanpushcart.moss.frontend;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.HashMap;
import java.util.Map;

/**
 * JS桥接
 */
public class JsBridge {

    public static final String MEMBER_NAME = "jsBridge";
    private final Map<String, JsMethodHandler> handlersMap = new HashMap<>();

    public JsBridge(WebView view) {
        install(view, this);
    }

    // 给WebView安装JsBridge
    private static void install(WebView view, JsBridge bridge) {
        view.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {

                // 注册window.jsBridge
                final var window = (JSObject) view.getEngine().executeScript("window");
                window.setMember(MEMBER_NAME, new JsBridgeFacade(bridge));

                Platform.runLater(()-> {
                    // 通知JsBridge初始化完成
                    view.getEngine().executeScript("""
                        if(JsBridge) {
                            JsBridge.completed();
                        }
                        """);
                });

            }
        });
    }

    /**
     * 注册方法处理器
     *
     * @param identity 标识符
     * @param handler  处理器
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public JsBridge register(String identity, JsMethodHandler handler) {
        handlersMap.put(identity, handler);
        return this;
    }

    /**
     * JsBridge 调用门面
     *
     * @param bridge JsBridge
     */
    public record JsBridgeFacade(JsBridge bridge) {

        /**
         * 判断标识符是否有被注册
         *
         * @param identity 标识符
         * @return TRUE | FALSE
         */
        public boolean has(String identity) {
            return bridge.handlersMap.containsKey(identity);
        }

        /**
         * 调用
         *
         * @param identity     标识符
         * @param argumentJson 参数JSON
         * @return 返回值
         */
        public Object call(String identity, String argumentJson) {
            final var handler = bridge.handlersMap.get(identity);
            if (null == handler) {
                throw new IllegalArgumentException("No handler for identity: " + identity);
            }
            return handler.handle(identity, argumentJson);
        }

    }

    /**
     * JS方法处理器
     */
    @FunctionalInterface
    public interface JsMethodHandler {

        /**
         * 处理
         *
         * @param identity     标识符
         * @param argumentJson 参数JSON
         * @return 返回值
         */
        Object handle(String identity, String argumentJson);

    }

}
