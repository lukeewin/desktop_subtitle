package top.lukeewin.subtitle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.yaml.snakeyaml.Yaml;
import top.lukeewin.subtitle.entity.ASRConfigEntity;
import top.lukeewin.subtitle.entity.FontConfigEntity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    private Label label;
    @FXML
    private BorderPane borderPane;
    private FontConfigEntity fontConfig;
    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            Yaml yaml = new Yaml();
            ASRConfigEntity asrConfigEntity = new ASRConfigEntity();
            Map<String, Object> yamlMap = yaml.load(new FileInputStream("src/main/resources/application.yml"));
            Map<String, Object> streamingASRConfigMap = (Map<String, Object>) yamlMap.get("streaming_asr");
            String model_encoder = (String) streamingASRConfigMap.get("model_encoder");
            String model_decoder = (String) streamingASRConfigMap.get("model_decoder");
            String model_tokens = (String) streamingASRConfigMap.get("model_tokens");

            asrConfigEntity.setModelEncoder(model_encoder);
            asrConfigEntity.setModelDecoder(model_decoder);
            asrConfigEntity.setModelTokens(model_tokens);

            Map<String, Object> fontConfigMap = (Map<String, Object>) yamlMap.get("font");
            int fontSize = (int) fontConfigMap.get("size");
            String fontColor = (String) fontConfigMap.get("color");
            fontConfig = new FontConfigEntity(fontSize, fontColor);
            applyFontConfig();

            Task task = new Task(label, asrConfigEntity);
            new Thread(task).start();

            createContextMenu();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createContextMenu() {
        MenuItem settingsItem = new MenuItem("设置");
        settingsItem.setOnAction(event -> openSettings());

        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(settingsItem);

        borderPane.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(borderPane, event.getScreenX(), event.getScreenY());
            } else {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });
    }

    private void openSettings() {
        // 打开设置窗口
        SettingsDialog settingsDialog = new SettingsDialog(label.getScene().getWindow(), fontConfig);
        FontConfigEntity newConfig = settingsDialog.showAndWait().orElse(null);
        if (newConfig != null) {
            this.fontConfig = newConfig;
            applyFontConfig();
            saveFontConfig();
        }
    }

    private void applyFontConfig() {
        label.setStyle("-fx-font-size: " + fontConfig.getSize() + "px; -fx-text-fill: " + fontConfig.getColor() + ";");
    }

    private void saveFontConfig() {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(new FileInputStream("src/main/resources/application.yml"));

            // 更新字体配置
            Map<String, Object> fontConfigMap = new HashMap<>();
            fontConfigMap.put("size", fontConfig.getSize());
            fontConfigMap.put("color", fontConfig.getColor());
            yamlMap.put("font", fontConfigMap);

            // 写回文件
            FileWriter writer = new FileWriter("src/main/resources/application.yml");
            yaml.dump(yamlMap, writer);
            writer.close();
        } catch (Exception e) {
            System.err.println("保存配置失败: " + e.getMessage());
        }
    }
}
