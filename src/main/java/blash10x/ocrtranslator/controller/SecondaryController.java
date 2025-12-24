// SecondaryController.java
package blash10x.ocrtranslator.controller;

import blash10x.ocrtranslator.service.ConfigLoader;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javax.imageio.ImageIO;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

/**
 * Author: myungsik.sung@gmail.com
 */
public class SecondaryController {
  private final String screenshotDir;
  private final String screenshotImageFilename;

  @FXML
  private HBox titleBar; // 이동 가능한 상단 바
  @FXML
  private Pane transparentRegion; // 스크린샷 대상 투명 영역
  @FXML
  private VBox rootVBox; // FXML의 최상위 VBox (창 크기 조절 이벤트 부착용)

  private Stage stage;
  @Setter
  private PrimaryController primaryController;

  // 창 이동 관련 변수
  private double xOffset = 0;
  private double yOffset = 0;

  // 창 크기 조절 관련 변수
  private double startX;
  private double startY;
  private double initialStageX;
  private double initialStageY;
  private double initialStageWidth;
  private double initialStageHeight;
  private ResizeMode currentResizeMode = ResizeMode.NONE;

  private static final double BORDER_WIDTH = 8; // 크기 조절을 위한 경계선 두께
  private static final double MIN_WIDTH = 200;  // 창의 최소 너비
  private static final double MIN_HEIGHT = 100; // 창의 최소 높이

  // 창 크기 조절 모드를 위한 Enum
  private enum ResizeMode {
    NONE, NW, N, NE, E, SE, S, SW, W
  }

  public SecondaryController() {
    ConfigLoader configLoader = ConfigLoader.getConfigLoader();

    screenshotDir = configLoader.getProperty("screenshot.dir");
    screenshotImageFilename = configLoader.getProperty("screenshot.image.filename");
  }

  @FXML
  public void initialize() {
    // --- 창 크기 조절 (rootVBox 전체에 이벤트 부착) ---
    // Scene의 root에 이벤트 리스너를 부착하여 전체 Stage 영역에 대해 감지
    rootVBox.setOnMouseMoved(event -> {
      Scene scene = stage.getScene();
      double titleBarHeight = titleBar.getHeight();
      double x = event.getX(); // Scene 내부 X 좌표
      double y = event.getY(); // Scene 내부 Y 좌표
      double width = scene.getWidth();
      double height = scene.getHeight();

      // 상단 바 영역에서는 리사이즈 커서를 보이지 않도록 함
      if (y < titleBarHeight) { // titleBar 영역
        scene.setCursor(Cursor.DEFAULT);
        currentResizeMode = ResizeMode.NONE;
        return;
      }

      // 커서 종류 설정 및 리사이즈 모드 감지
      if (x < BORDER_WIDTH && y < BORDER_WIDTH + titleBarHeight) { // 좌상단
        scene.setCursor(Cursor.NW_RESIZE); currentResizeMode = ResizeMode.NW;
      } else if (x > width - BORDER_WIDTH && y < BORDER_WIDTH + titleBarHeight) { // 우상단
        scene.setCursor(Cursor.NE_RESIZE); currentResizeMode = ResizeMode.NE;
      } else if (x < BORDER_WIDTH && y > height - BORDER_WIDTH) { // 좌하단
        scene.setCursor(Cursor.SW_RESIZE); currentResizeMode = ResizeMode.SW;
      } else if (x > width - BORDER_WIDTH && y > height - BORDER_WIDTH) { // 우하단
        scene.setCursor(Cursor.SE_RESIZE); currentResizeMode = ResizeMode.SE;
      } else if (x < BORDER_WIDTH) { // 좌측
        scene.setCursor(Cursor.W_RESIZE); currentResizeMode = ResizeMode.W;
      } else if (x > width - BORDER_WIDTH) { // 우측
        scene.setCursor(Cursor.E_RESIZE); currentResizeMode = ResizeMode.E;
      } else if (y < BORDER_WIDTH + titleBarHeight) { // 상단
        scene.setCursor(Cursor.N_RESIZE); currentResizeMode = ResizeMode.N;
      } else if (y > height - BORDER_WIDTH) { // 하단
        scene.setCursor(Cursor.S_RESIZE); currentResizeMode = ResizeMode.S;
      } else { // 기본
        scene.setCursor(Cursor.DEFAULT); currentResizeMode = ResizeMode.NONE;
      }
    });

    rootVBox.setOnMousePressed(event -> {
      // 현재 리사이즈 모드 저장 및 초기 위치/크기 저장
      if (currentResizeMode != ResizeMode.NONE) {
        startX = event.getScreenX();
        startY = event.getScreenY();
        initialStageX = stage.getX();
        initialStageY = stage.getY();
        initialStageWidth = stage.getWidth();
        initialStageHeight = stage.getHeight();
      } else { // 마우스 커서가 경계선이 아닐 때만 이동 모드
        xOffset = stage.getX() - event.getScreenX();
        yOffset = stage.getY() - event.getScreenY();
      }
    });

    rootVBox.setOnMouseDragged(event -> {
      if (currentResizeMode == ResizeMode.NONE) { // 마우스 커서가 경계선이 아닐 때만 이동 모드
        stage.setX(event.getScreenX() + xOffset);
        stage.setY(event.getScreenY() + yOffset);
        return; // 리사이즈 모드가 아니면 처리 안함
      }

      double deltaX = event.getScreenX() - startX;
      double deltaY = event.getScreenY() - startY;

      double newX = initialStageX;
      double newY = initialStageY;
      double newWidth = initialStageWidth;
      double newHeight = initialStageHeight;

      switch (currentResizeMode) {
        case NW:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth - deltaX);
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight - deltaY);
          newX = initialStageX + (initialStageWidth - newWidth);
          newY = initialStageY + (initialStageHeight - newHeight);
          break;
        case N:
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight - deltaY);
          newY = initialStageY + (initialStageHeight - newHeight);
          break;
        case NE:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth + deltaX);
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight - deltaY);
          newY = initialStageY + (initialStageHeight - newHeight);
          break;
        case E:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth + deltaX);
          break;
        case SE:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth + deltaX);
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight + deltaY);
          break;
        case S:
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight + deltaY);
          break;
        case SW:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth - deltaX);
          newHeight = Math.max(MIN_HEIGHT, initialStageHeight + deltaY);
          newX = initialStageX + (initialStageWidth - newWidth);
          break;
        case W:
          newWidth = Math.max(MIN_WIDTH, initialStageWidth - deltaX);
          newX = initialStageX + (initialStageWidth - newWidth);
          break;
        default:
          return; // ResizeMode.NONE인 경우
      }

      stage.setX(newX);
      stage.setY(newY);
      stage.setWidth(newWidth);
      stage.setHeight(newHeight);
    });

    rootVBox.setOnMouseReleased(event -> {
      // 마우스 버튼이 놓이면 리사이즈 모드 초기화 및 커서 기본으로
      currentResizeMode = ResizeMode.NONE;
      stage.getScene().setCursor(Cursor.DEFAULT);
    });

    // 마우스가 창 밖으로 나갔을 때 커서 초기화 (선택 사항)
    rootVBox.setOnMouseExited(event -> {
      if (currentResizeMode == ResizeMode.NONE) {
        stage.getScene().setCursor(Cursor.DEFAULT);
      }
    });

    transparentRegion.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleCapture();
        event.consume();
      }
    });

    rootVBox.setOnKeyPressed(event -> {
      if (event.isAltDown() && event.getCode() == KeyCode.C) {
        System.out.println("Alt + C pressed! Event handled.");
        handleCapture();
        event.consume();
      }
    });
  }

  // Stage 객체를 설정하고 투명성 등을 처리
  public void setStage(Stage stage) {
    this.stage = stage;
    this.stage.initStyle(StageStyle.TRANSPARENT); // 테두리 없는 투명 창
    this.stage.getScene().setFill(null); // Scene의 배경도 null로 설정하여 투명하게
  }

  public void handleCapture() {
    if (stage == null || primaryController == null) {
      System.err.println("Stage or PrimaryController is not set.");
      return;
    }

    // 스크린샷 캡처를 위해 투명 창의 중앙 영역 (transparentRegion)의 화면상 절대 좌표를 계산합니다.
    javafx.geometry.Point2D screenCoords = transparentRegion.localToScreen(0, 0);

    int margin = 3; // exclude: -fx-border-width: 2
    int x = (int) screenCoords.getX() + margin;
    int y = (int) screenCoords.getY() + margin;
    int width = (int) transparentRegion.getWidth() - 2 * margin;
    int height = (int) transparentRegion.getHeight() - 2 * margin;

    // 캡처 영역이 유효한지 확인
    if (width <= 0 || height <= 0) {
      System.err.println("캡처할 영역의 너비 또는 높이가 0 이하입니다. 창 크기를 조절해주세요.");
      return;
    }

    try {
      Robot robot = new Robot();
      BufferedImage screenCapture = robot.createScreenCapture(new Rectangle(x, y, width, height));
      WritableImage fxImage = SwingFXUtils.toFXImage(screenCapture, null);

      File screenCaptureFile = new File(screenshotDir, screenshotImageFilename);
      String fileExtension = FilenameUtils.getExtension(screenCaptureFile.getName());
      ImageIO.write(screenCapture, fileExtension, screenCaptureFile);

      primaryController.displayCapturedImage(fxImage, screenCaptureFile);
      System.out.println("스크린샷이 Primary Stage로 전송되었습니다.");
      // stage.close(); // 캡처 후 창을 닫을 경우
    } catch (AWTException e) {
      System.err.println("화면 캡처 중 오류가 발생했습니다: " + e.getMessage());
    } catch (IOException e) {
      System.err.println("화면 캡처 저장 중 오류가 발생했습니다: " + e.getMessage());
    }
  }

  @FXML
  private void handleClose() {
    if (stage != null) {
      stage.close();
    }
  }
}