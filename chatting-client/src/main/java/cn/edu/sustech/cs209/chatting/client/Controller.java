package cn.edu.sustech.cs209.chatting.client;

import static cn.edu.sustech.cs209.chatting.client.EmojiPickerController.EMOJI_RESOURCE_PATH;

import cn.edu.sustech.cs209.chatting.client.ClientService.ConnectionFailedException;
import cn.edu.sustech.cs209.chatting.client.EmojiPickerController.EmojiSelectionListener;
import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.DataType;
import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;

public class Controller implements Initializable {

  @FXML
  public TextArea inputArea;
  public Label currentUsername;
  public Label currentOnlineCnt;
  @FXML
  ListView<Message> chatContentList = new ListView<>(); // 聊天内容
  @FXML
  public ListView<Chat> chatList = new ListView<>();  // 会话列表ListView
  public ObservableList<Chat> items = FXCollections.observableArrayList(); // 会话列表Items
  public static ConcurrentHashMap<String, Chat> currentChats = new ConcurrentHashMap<>(); // 会话列表 map
  ClientService clientService;
  String username;
  String password;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    clientService = new ClientService(this);

    Dialog<Pair<String, String>> dialog = new Dialog<>();
    dialog.setTitle("Login or Sign in");
    dialog.setHeaderText(null);

    // 设置按钮
    dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    // 创建用户名和密码的输入框
    TextField usernameField = new TextField();
    usernameField.setPromptText("Username");
    PasswordField passwordField = new PasswordField();
    passwordField.setPromptText("Password");

    // 将输入框添加到对话框
    VBox content = new VBox(10);
    content.getChildren().addAll(usernameField, passwordField);
    dialog.getDialogPane().setContent(content);

    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == ButtonType.OK) {
        return new Pair<>(usernameField.getText(), passwordField.getText());
      } else {
        Platform.exit();
        return null;
      }
    });

//        dialog.setOnCloseRequest(event -> {
//                Platform.exit();
//        });

    boolean validUsername = false;
    while (!validUsername) {

      Optional<Pair<String, String>> result = dialog.showAndWait();

      if (result.isPresent()) {
        username = result.get().getKey();
        password = result.get().getValue();

        if (username.trim().isEmpty() || password.trim().isEmpty()) {
          // 显示警告框
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setTitle("");
          alert.setHeaderText(null);
          alert.setContentText("Please enter username or password!");

          ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
          alert.getButtonTypes().setAll(okButton);

          alert.showAndWait();
          // 保持循环，让用户重新输入
        } else {
          try {
            if (clientService.checkUser(username, password)) {
              // 用户名有效
              validUsername = true;
              chatContentList.setCellFactory(new MessageCellFactory());
              // 在成功登录后，为聊天窗口添加关闭请求处理程序
              Platform.runLater(this::addCloseRequestHandler);

              // 将当前用户名显示在聊天窗口的标题栏
              Platform.runLater(() -> currentUsername.setText("User: " + username));

              // 将当前在线人数显示在聊天窗口的标题栏
              Platform.runLater(() -> currentOnlineCnt.setText(
                  "Online: " + (clientService.getOnlineCount() + 1)));

              // 将会话列表的ListView显示为聊天名称
              Platform.runLater(() -> {
//                chatList.setStyle("-fx-control-inner-background: #F5F5FA;"
//                    + " -fx-control-inner-background-alt: #F5F6FA;");
                chatList.setItems(items);
                chatList.setCellFactory(param -> new ListCell<Chat>() {
                  @Override
                  protected void updateItem(Chat chat, boolean empty) {
                    super.updateItem(chat, empty);
                    if (empty || chat == null) {
                      Platform.runLater(() -> setText(null));
                    } else {
                      synchronized (chat) {
                        String chatName = chat.getChatName(username);
                        if (!chat.unread){
//                          Platform.runLater(() -> setStyle("-fx-background: #F5F5FA;"));
                          Platform.runLater(() -> setTextFill(Color.BLACK));
                        }else {
//                          Platform.runLater(() -> setStyle("-fx-background: #DBFDFF;"));
                          Platform.runLater(() -> setTextFill(Color.GREEN));
                        }
                        Platform.runLater(() -> setText(chatName));
                      }
                    }
                  }
                });
              });

              // 为会话列表添加点击事件
              chatList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                  Chat chat = chatList.getSelectionModel().getSelectedItem();
                  if (chat != null) {
                    chat.unread = false;
                    // 将聊天内容显示在聊天窗口
                    chatList.refresh();
                    ObservableList<Message> messages = FXCollections.observableArrayList();
                    messages.addAll(chat.getMessages());
                    chatContentList.setItems(messages);
                    chatContentList.scrollTo(chat.getMessages().size() - 1);
                  }
                }
              });
            } else {
              // 显示警告框
              Alert alert = new Alert(Alert.AlertType.WARNING);
              alert.setTitle("Login Failed");
              alert.setHeaderText(null);
              alert.setContentText(
                  "The user has logged in, or wrong password.\nPlease check the username or password and try again.");

              ButtonType okButton = new ButtonType("OK",
                  ButtonBar.ButtonData.OK_DONE);
              alert.getButtonTypes().setAll(okButton);

              alert.showAndWait();
              // 保持循环，让用户重新输入
            }
          } catch (ConnectionFailedException e) {
            // 显示警告框
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Connection Error");
            alert.setHeaderText(null);
            alert.setContentText(
                "Cannot connect to the server. Please check your network connection and try again.");

            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okButton);

            alert.showAndWait();
            // 保持循环，让用户重新输入
          }
        }
      } else {
        // 用户点击了 "Cancel" 按钮，直接关闭窗口
        Platform.exit();
        break;
      }
    }
  }

  public void addCloseRequestHandler() {
    // 获取聊天窗口的Stage
    Stage chatWindowStage = (Stage) chatContentList.getScene().getWindow();

    // 设置关闭请求处理程序
    chatWindowStage.setOnCloseRequest(event -> {
      windowClosing();
    });
  }

  public void windowClosing() {
    Message logoutMessage = new Message(username, DataType.MESSAGE_CLIENT_EXIT);
    clientService.sendMessage(logoutMessage);
    System.out.println("Client apply to exit.");
    // 停止客户端线程
    clientService.stopRunning();

    Platform.runLater(() -> {
      Platform.exit();
      System.exit(0);
    });
  }

  public void windowClosingForServer() {
    // 停止客户端线程
    System.out.println("Quit due to losing connection.");
    clientService.stopRunning();
//        Platform.exit();
  }

  @FXML
  public void createPrivateChat() throws InterruptedException {
    AtomicReference<String> user = new AtomicReference<>();

    // FIXME: get the user list from server, the current user's name should be filtered out
    clientService.getAllFriendList();
    Thread.sleep(1000);
    if (clientService.thread.allFriends.size() == 0) {
      System.out.println("no friends");
      // 显示警告框 - 当前没有好友
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("No Friends");
      alert.setHeaderText(null);
      alert.setContentText("Oops, there is no friend.");
      ButtonType okButton = new ButtonType("OK",
          ButtonBar.ButtonData.OK_DONE);
      alert.getButtonTypes().setAll(okButton);
      alert.showAndWait();
    } else {

      Stage stage = new Stage();
      ComboBox<String> userSel = new ComboBox<>();
      userSel.getItems().addAll(clientService.thread.allFriends);
      userSel.getSelectionModel().selectFirst();

      Button okBtn = new Button("OK");
      try {
        okBtn.setOnAction(e -> {
          user.set(userSel.getSelectionModel().getSelectedItem());
          stage.close();
        });

        Label titleLabel = new Label("Select a Friend:");
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 40, 40, 40));
        box.getChildren().addAll(titleLabel, userSel, okBtn);
        stage.setScene(new Scene(box));
        stage.showAndWait();

        Chat chat = new Chat(username, user.get());
        // TODO: if the current user already chatted with the selected user, just open the chat with that user
        // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
        if (!currentChats.containsKey(chat.getChatID())) {
          currentChats.put(chat.getChatID(), chat);
          items.add(chat);
          chatList.setItems(items);
          LocalDateTime now = LocalDateTime.now();
          chat.setLatestTime(now);
          // send the new chat to server
          clientService.sendChat(chat);
        }
        chatList.getSelectionModel().select(chat);
        changeChat(chat.getMessages());
      } catch (RuntimeException e) {
        return;
      }
    }
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() throws InterruptedException, IOException {
    List<AtomicReference<String>> users = new ArrayList<>();
    clientService.getAllFriendList();
    Thread.sleep(1000);

    Stage stage = new Stage();
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("groupSel.fxml"));
    // 加载FXML文件
    fxmlLoader.load();
    // 获取FXML文件中的所有命名空间组件
    Map<String, Object> namespace = fxmlLoader.getNamespace();

    System.out.println(namespace);

    // 通过fx:id值从命名空间映射中获取ListView组件
    ListView<CheckBox> allFriends = (ListView<CheckBox>) namespace.get("allFriends");

    // 将List<String>转换为List<CheckBox>并添加到ListView中
    List<CheckBox> checkBoxes = clientService.thread.allFriends.stream()
        .map(CheckBox::new)
        .collect(Collectors.toList());
    allFriends.getItems().addAll(checkBoxes);

    stage.setScene(new Scene((Parent) namespace.get("groupSel")));
    stage.setTitle("Select Friends");
    Button btn_OK = (Button) namespace.get("btn_OK");
    Button btn_Cancel = (Button) namespace.get("btn_Cancel");
    btn_OK.setOnAction(e -> {
      // 获取所有选中的CheckBox
      List<CheckBox> selectedCheckBoxes = allFriends.getItems().stream()
          .filter(CheckBox::isSelected)
          .collect(Collectors.toList());
      // 将选中的CheckBox的文本添加到users中
      users.addAll(selectedCheckBoxes.stream()
          .map(checkBox -> new AtomicReference<>(checkBox.getText()))
          .collect(Collectors.toList()));
      stage.close();
    });
    btn_Cancel.setOnAction(e -> {
      stage.close();
    });
    stage.showAndWait();

    // 将当前用户添加到users中
    users.add(new AtomicReference<>(username));

    // 将users按照字典序排序
    users.sort(Comparator.comparing(AtomicReference::get));
    String name = "";

    // 处理群聊名称
    if (users.size() > 3) {
      name =
          users.get(0).get() + ", " + users.get(1).get() + ", " + users.get(2).get() + "... ("
              + users.size() + ")";
    } else {
      for (AtomicReference<String> user : users) {
        name += user.get() + ", ";
      }
      name = name.substring(0, name.length() - 2) + " (" + users.size() + ")";
    }

    Chat chat = new Chat(users, name);
    if (!currentChats.contains(chat)) {
      currentChats.put(chat.getChatID(), chat);
      items.add(chat);
      chatList.setItems(items);

      clientService.sendChat(chat);
    }
    chatList.getSelectionModel().select(chat);
    changeChat(chat.getMessages());
  }

  /**
   * Sends the message to the currently selected chat. Blank messages are not allowed. After sending
   * the message, you should clear the text input field.
   */
  @FXML
  public void doSendMessage() {
    String msgContent = inputArea.getText();
    if (msgContent.equals("")) {
      return;
    }
    Chat chat = chatList.getSelectionModel().getSelectedItem();
    try {

      Message msg = new Message(username, chat.getChatID(), msgContent);

      if (chat.getChatType().equals("private")) {
        msg.setSendTo(chat.getMembers(username)[0]);
      }
      // 设置消息发送时间
      LocalDateTime time = LocalDateTime.now();
      msg.setSentTime(time);
      // 设置消息类型（text）
      msg.setDataType(DataType.MESSAGE_TEXT_MESSAGE);

      // 发送消息
      clientService.sendMessage(msg);

      inputArea.clear();
      chat.getMessages().add(msg);
      chat.setLatestTime(time);

      // 将这个chat放到最前面
      items.remove(chat);
      items.add(0, chat);
      chatList.setItems(items);
      chatList.refresh();
      chatList.getSelectionModel().select(chat);

      chatContentList.getItems().add(msg);
      chatContentList.refresh();
    } catch (NullPointerException e) {
      // 显示警告框
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("");
      alert.setHeaderText(null);
      alert.setContentText("Please choice a chat first");

      ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
      alert.getButtonTypes().setAll(okButton);

      alert.showAndWait();
    }
  }

  public void doSendEmoji() {
    EmojiSelectionListener listener = new EmojiSelectionListener() {
      @Override
      public void onEmojiSelected(String emojiFileName) throws IOException {
        // 在这里处理所选的emoji，插入到聊天输入框
        File emojiFile = new File(EMOJI_RESOURCE_PATH, emojiFileName);
        packFile(emojiFile, "image");
      }
    };

    // 然后将listener作为参数传递给showEmojiPicker方法
    showEmojiPicker(listener);
  }

  public void showEmojiPicker(EmojiSelectionListener listener) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("EmojiPicker.fxml"));
      Parent root = loader.load();

      EmojiPickerController controller = loader.getController();
      controller.setEmojiSelectionListener(listener);

      Stage emojiPickerStage = new Stage();
      emojiPickerStage.setTitle("Emoji");
      Scene scene = new Scene(root, 220, 300);
      emojiPickerStage.setScene(scene);
      emojiPickerStage.setResizable(false);
      emojiPickerStage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void doSendImage() {
    // 创建一个文件选择器对象
    FileChooser fileChooser = new FileChooser();

    // 设置文件选择器的标题
    fileChooser.setTitle("Select Image");

    // 设置文件类型过滤器
    FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter("Image Files",
        "*.jpg", "*.png", "*.gif", "*.jpeg", "*.bmp", "*.ico");
    fileChooser.getExtensionFilters().add(imageFilter);

    // 显示文件选择窗口并获取选择的文件
    File selectedFile = fileChooser.showOpenDialog(null);

    // 检查用户是否选择了文件
    if (selectedFile != null) {
      // 在这里处理选定的文件
      packFile(selectedFile, "image");
    }
  }

  public void doSendFile() {
    // 创建一个文件选择器对象
    FileChooser fileChooser = new FileChooser();

    // 设置文件选择器的标题
    fileChooser.setTitle("Select File");

    // 显示文件选择窗口并获取选择的文件
    File selectedFile = fileChooser.showOpenDialog(null);

    // 检查用户是否选择了文件
    if (selectedFile != null) {
      // 在这里处理选定的文件
      packFile(selectedFile, "file");
    }
  }

  private void packFile(File selectedFile, String type) {
    String filePath = selectedFile.getAbsolutePath();
    System.out.println("Selected image: " + filePath);
    Chat chat = chatList.getSelectionModel().getSelectedItem();

    // 读取文件内容到字节数组
    byte[] fileContent;
    try {
      fileContent = Files.readAllBytes(Paths.get(filePath));
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Failed to read file: " + filePath);
      return; // 读取文件失败，结束方法
    }

    // 使用字节数组创建 Message 对象
    Message msg = new Message(username, chat.getChatID(), fileContent);
    msg.setFileName(selectedFile.getName());

    // 设置消息发送时间
    LocalDateTime time = LocalDateTime.now();
    msg.setSentTime(time);
    // 设置消息类型（text）
    if (type.equals("image")) {
      msg.setDataType(DataType.MESSAGE_IMAGE_MESSAGE);
    } else if (type.equals("file")) {
      msg.setDataType(DataType.MESSAGE_FILE_MESSAGE);
    }

    // 设置发送对象
    if (chat.getChatType().equals("private")) {
      msg.setSendTo(chat.getMembers(username)[0]);
    }

    // 发送消息
    clientService.sendMessage(msg);

    // 更新聊天列表、时间
    chat.getMessages().add(msg);
    chat.setLatestTime(time);

    // 将这个chat放到最前面
    items.remove(chat);
    items.add(0, chat);
    chatList.setItems(items);
    chatList.refresh();
    chatList.getSelectionModel().select(chat);

    chatContentList.getItems().add(msg);
    chatContentList.refresh();
  }

  // 当聊天对象发生变化时调用此方法
  public void changeChat(List<Message> messagesForNewChat) {
    // 将新聊天的消息列表转换为ObservableList
    ObservableList<Message> newMessages = FXCollections.observableArrayList(messagesForNewChat);

    // 更新ListView中的消息
    chatContentList.setItems(newMessages);
  }

  public void showDetails() {
    Chat chat = chatList.getSelectionModel().getSelectedItem();
    if (chat != null) {
      try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatDetails.fxml"));
        Parent root = loader.load();

        ChatDetailsController controller = loader.getController();
        controller.setChatDetails(chat);

        Stage chatDetailsStage = new Stage();
        chatDetailsStage.setTitle("Chat Details");
        Scene scene = new Scene(root, 600, 400);
        chatDetailsStage.setScene(scene);
        chatDetailsStage.show();
      } catch (IOException e) {
        // 显示警告框
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("");
        alert.setHeaderText(null);
        alert.setContentText("Please choice a chat first");

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);

        alert.showAndWait();
      }
    }
  }

  public void showOnlineFriend(){
    Stage stage = new Stage();
    stage.setTitle("Online Friends");
    ListView<String> listView = new ListView<>();
    ObservableList<String> items = FXCollections.observableArrayList();
    items.addAll(clientService.getOnlineFriendList());
    listView.setItems(items);
    Scene scene = new Scene(listView, 200, 300);
    stage.setScene(scene);
    stage.show();
  }


  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);

          if (empty || Objects.isNull(msg)) {
            Platform.runLater(() -> {
              setGraphic(null);
              setText(null);
            });
            return;
          }

          chatContentList.setStyle("-fx-control-inner-background: #F5F5FA;"
              + " -fx-control-inner-background-alt: #F5F6FA;");

          HBox wrapper = new HBox();
          wrapper.setStyle("-fx-background-color: transparent;");
          Label nameLabel = new Label(msg.getSentBy());
          Node contentNode;

          nameLabel.setPrefSize(30, 30);
          nameLabel.setWrapText(true);
          nameLabel.setStyle(
              "-fx-border-color: black; -fx-border-width: 1px; -fx-border-radius: 15px; -fx-alignment: center;");

          if (msg.getDataType().equals(DataType.MESSAGE_FILE_MESSAGE)) {
            System.out.println("File name: " + msg.getFileName());
            contentNode = new Label(msg.getFileName());
            contentNode.setOnMouseClicked(e -> saveFile(msg, msg.getFileName()));
          } else if (msg.getDataType().equals(DataType.MESSAGE_IMAGE_MESSAGE)) {
            ImageView imageView = new ImageView(
                new Image(new ByteArrayInputStream(msg.getDataStream())));
            imageView.setFitHeight(150);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
            contentNode = imageView;
            contentNode.setOnMouseClicked(e -> saveFile(msg, "image.png"));
          } else {
            contentNode = new Label(msg.getData());
          }

          if (username.equals(msg.getSentBy())) {
            contentNode.setStyle("-fx-text-fill: #FFFFFF;");
            StackPane contentWrapper = new StackPane(contentNode);
            contentWrapper.setStyle("-fx-background-color: #00CCFF; "
                + "-fx-padding: 10px;"
                + " -fx-border-color: #00CCFF; "
                + "-fx-border-width: 2px; "
                + "-fx-border-radius: 10px; "
                + "-fx-background-radius: 5px; ");
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(contentWrapper, nameLabel);
          } else {
            contentNode.setStyle("-fx-text-fill: #000000;");
            StackPane contentWrapper = new StackPane(contentNode);
            contentWrapper.setStyle("-fx-background-color: #FFFFFF; "
                + "-fx-padding: 10px;"
                + " -fx-border-color: #FFFFFF; "
                + "-fx-border-width: 2px; "
                + "-fx-border-radius: 10px; "
                + "-fx-background-radius: 5px; ");
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, contentWrapper);
          }

          Platform.runLater(() -> {
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            setGraphic(wrapper);
          });
        }


        private void saveFile(Message msg, String fileName) {
          FileChooser fileChooser = new FileChooser();
          fileChooser.setTitle("Save File");
          fileChooser.setInitialFileName(fileName);
          File selectedFile = fileChooser.showSaveDialog(getScene().getWindow());

          if (selectedFile != null) {
            try {
              Files.write(selectedFile.toPath(), msg.getDataStream());
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      };
    }
  }
}
