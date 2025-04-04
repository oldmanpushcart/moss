package io.github.oldmanpushcart.moss.frontend.javafx.view;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

/**
 * 带Tooltip的文本表格列
 *
 * @param <S>
 */
class TooltipStringTableCell<S> extends TableCell<S, String> {
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setTooltip(null);
        } else {
            setText(item);
            setTooltip(new Tooltip(item));
        }
    }
}
