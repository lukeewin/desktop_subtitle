package top.lukeewin.subtitle;

import com.google.common.io.Resources;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(Resources.getResource("sample.fxml"));
        Parent root = loader.load();

        Controller controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();

        final int width = (int) primaryScreenBounds.getWidth();
        final int height = 80;

        final Scene scene = new Scene(root, width, height);
        scene.setFill(null);

        final Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        stage.setX((primaryScreenBounds.getWidth() - width) / 2);
        stage.setY((primaryScreenBounds.getHeight() - height));
        stage.setAlwaysOnTop(true);

        scene.setOnMouseEntered(event -> {
            scene.getRoot().setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        });

        scene.setOnMouseExited(event -> {
            scene.getRoot().setStyle("-fx-background-color: transparent;");
        });

        stage.setOpacity(1.0);

        // 拖动监听器
        DragUtil.addDragListener(stage, root);
        stage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
