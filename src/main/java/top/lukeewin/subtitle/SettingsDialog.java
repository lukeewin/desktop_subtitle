package top.lukeewin.subtitle;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import top.lukeewin.subtitle.entity.FontConfigEntity;

/**
 * @author Luke Ewin
 * @date 2025/8/22 17:08
 * @blog blog.lukeewin.top
 */
public class SettingsDialog extends Dialog<FontConfigEntity> {

    private Spinner<Integer> fontSizeSpinner;
    private ColorPicker colorPicker;
    private FontConfigEntity initialConfig;

    public SettingsDialog(Window owner, FontConfigEntity initialConfig) {
        this.initialConfig = initialConfig;

        setTitle("字幕设置");
        setHeaderText("调整字幕显示样式");

        // 设置对话框的所有者
        if (owner != null) {
            initOwner(owner);
        }

        // 创建按钮
        ButtonType applyButtonType = new ButtonType("应用", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        fontSizeSpinner = new Spinner<>(10, 72, initialConfig.getSize());
        fontSizeSpinner.setEditable(true);

        // 添加焦点丢失事件处理，确保输入的值被提交
        fontSizeSpinner.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // 失去焦点时
                try {
                    String text = fontSizeSpinner.getEditor().getText();
                    int value = Integer.parseInt(text);
                    // 确保值在范围内 - 使用SpinnerValueFactory获取范围
                    SpinnerValueFactory<Integer> valueFactory = fontSizeSpinner.getValueFactory();
                    if (valueFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
                        SpinnerValueFactory.IntegerSpinnerValueFactory intFactory =
                                (SpinnerValueFactory.IntegerSpinnerValueFactory) valueFactory;
                        if (value < intFactory.getMin()) {
                            value = intFactory.getMin();
                        } else if (value > intFactory.getMax()) {
                            value = intFactory.getMax();
                        }
                    }
                    fontSizeSpinner.getValueFactory().setValue(value);
                } catch (NumberFormatException e) {
                    // 输入不是有效数字，恢复原值
                    fontSizeSpinner.getEditor().setText(fontSizeSpinner.getValue().toString());
                }
            }
        });

        colorPicker = new ColorPicker();
        colorPicker.setValue(Color.web(initialConfig.getColor()));

        grid.add(new Label("字体大小:"), 0, 0);
        grid.add(fontSizeSpinner, 1, 0);
        grid.add(new Label("字体颜色:"), 0, 1);
        grid.add(colorPicker, 1, 1);

        getDialogPane().setContent(grid);

        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                // 确保Spinner的值是最新的
                try {
                    String text = fontSizeSpinner.getEditor().getText();
                    int value = Integer.parseInt(text);
                    // 确保值在范围内 - 使用SpinnerValueFactory获取范围
                    SpinnerValueFactory<Integer> valueFactory = fontSizeSpinner.getValueFactory();
                    if (valueFactory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
                        SpinnerValueFactory.IntegerSpinnerValueFactory intFactory =
                                (SpinnerValueFactory.IntegerSpinnerValueFactory) valueFactory;
                        if (value < intFactory.getMin()) {
                            value = intFactory.getMin();
                        } else if (value > intFactory.getMax()) {
                            value = intFactory.getMax();
                        }
                    }
                    fontSizeSpinner.getValueFactory().setValue(value);
                } catch (NumberFormatException e) {
                    // 输入不是有效数字，使用当前值
                }

                return new FontConfigEntity(
                        fontSizeSpinner.getValue(),
                        toHexString(colorPicker.getValue())
                );
            }
            return null;
        });
    }

    private String toHexString(Color color) {
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
