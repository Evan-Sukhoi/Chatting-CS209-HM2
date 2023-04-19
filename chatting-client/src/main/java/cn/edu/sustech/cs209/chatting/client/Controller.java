package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.client.ClientService.ConnectionFailedException;
import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.DataType;
import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import javafx.util.Pair;

public class Controller implements Initializable {

    @FXML
    public TextArea inputArea;
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

                            // 将会话列表的ListView显示为聊天名称
                            Platform.runLater(() -> {
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
                                        // 将聊天内容显示在聊天窗口
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

    private void windowClosing() {
        Message logoutMessage = new Message(username, DataType.MESSAGE_CLIENT_EXIT);
        clientService.sendMessage(logoutMessage);
        System.out.println("Client apply to exit.");
        // 停止客户端线程
        clientService.stopRunning();
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
     * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the
     * first three usernames, sorted in lexicographic order, then use ellipsis with the number of
     * users, for example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the
     * ellipsis, for example: UserA, UserB (2)
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
     * Sends the message to the currently selected chat. Blank messages are not allowed. After
     * sending the message, you should clear the text input field.
     */
    @FXML
    public void doSendMessage() {
        String msgContent = inputArea.getText();
        if (msgContent.equals("")) {
            return;
        }
        Chat chat = chatList.getSelectionModel().getSelectedItem();
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

        chatContentList.getItems().add(msg);
    }

    // 当聊天对象发生变化时调用此方法
    public void changeChat(List<Message> messagesForNewChat) {
        // 将新聊天的消息列表转换为ObservableList
        ObservableList<Message> newMessages = FXCollections.observableArrayList(messagesForNewChat);

        // 更新ListView中的消息
        chatContentList.setItems(newMessages);
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
                public void updateItem(Message msg, boolean empty) { // 更新列表
                    super.updateItem(msg, empty);
                    if (empty || Objects.isNull(msg)) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }

                    HBox wrapper = new HBox();
                    Label nameLabel = new Label(msg.getSentBy());
                    Label msgLabel = new Label(msg.getData());

                    nameLabel.setPrefSize(50, 20);
                    nameLabel.setWrapText(true);
                    nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

                    if (username.equals(msg.getSentBy())) {
                        wrapper.setAlignment(Pos.TOP_RIGHT);
                        wrapper.getChildren().addAll(msgLabel, nameLabel);
                        msgLabel.setPadding(new Insets(0, 20, 0, 0));
                    } else {
                        wrapper.setAlignment(Pos.TOP_LEFT);
                        wrapper.getChildren().addAll(nameLabel, msgLabel);
                        msgLabel.setPadding(new Insets(0, 0, 0, 20));
                    }

                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(wrapper);
                }
            };
        }
    }
}
