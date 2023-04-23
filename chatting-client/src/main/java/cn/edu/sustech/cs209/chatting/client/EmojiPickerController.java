package cn.edu.sustech.cs209.chatting.client;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.stage.Stage;

public class EmojiPickerController implements Initializable {

  @FXML
  private AnchorPane rootPane;
  @FXML
  private VBox rootVBox;
  @FXML
  private ScrollPane emojiScrollPane;
  @FXML
  private GridPane emojiGridPane;

  private static List<String> EMOJI_FILE_NAMES;
  static String EMOJI_RESOURCE_PATH; // 修改为emoji资源文件夹的路径

  public EmojiPickerController() {
    String curDir = System.getProperty("user.dir");
    EMOJI_RESOURCE_PATH = curDir + File.separator + "emojis";
    EMOJI_FILE_NAMES = new ArrayList<>();
    try {
      Path emojiPath = Paths.get(EMOJI_RESOURCE_PATH);
      if (Files.isDirectory(emojiPath)) {
        FilenameFilter imageFilter = (dir, name) -> {
          String lowerCaseName = name.toLowerCase();
          return lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".jpg")
              || lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".gif");
        };
        File[] files = new File(EMOJI_RESOURCE_PATH).listFiles(imageFilter);
        if (files != null) {
          EMOJI_FILE_NAMES = Arrays.stream(files).map(File::getName).collect(Collectors.toList());
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public interface EmojiSelectionListener {

    void onEmojiSelected(String emojiFileName) throws IOException;
  }

  private EmojiSelectionListener listener;

  public void setEmojiSelectionListener(EmojiSelectionListener listener) {
    this.listener = listener;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    emojiGridPane.setPadding(new Insets(10));

    List<ImageView> emojiImageViews = new ArrayList<>();

    for (int i = 0; i < EMOJI_FILE_NAMES.size(); i++) {
      String emojiFileName = EMOJI_FILE_NAMES.get(i);
      Image emojiImage = new Image(EMOJI_RESOURCE_PATH + File.separator + emojiFileName);
      ImageView imageView = new ImageView(emojiImage);
      imageView.setFitWidth(32);
      imageView.setFitHeight(32);
      imageView.setPreserveRatio(true);
      imageView.setSmooth(true);
      imageView.setCache(true);

      imageView.setOnMouseClicked(event -> {
        try {
          listener.onEmojiSelected(emojiFileName);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
      });

      emojiImageViews.add(imageView);
      emojiGridPane.add(imageView, i % 6, i / 6);
    }
  }
}
