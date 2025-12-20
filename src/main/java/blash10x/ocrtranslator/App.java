// src/main/java/blash10x/ocrtranslator/App.java
package blash10x.ocrtranslator;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Author: myungsik.sung@gmail.com
 */
public class App extends Application {

  @Override
  public void init() throws Exception {
    super.init();
  }

  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("primary-view.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 622, 830);
    stage.setTitle("OCR Translator");
    stage.setScene(scene);
    stage.show();

    Window window = scene.getWindow();
    stage.setY(window.getY() + 100);

    stage.setOnCloseRequest(event -> Platform.exit());
  }

  public static void main(String[] args) {
    launch();
  }
}