package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class ClientThread extends Thread {

  private Socket socket;
  private User user;
  public Controller controller;
  public boolean inProgress = false;
  private boolean isServerOnline = true;
  private final Object pingLock = new Object();

  List<String> onlineFriends = new ArrayList<>();

  List<String> allFriends = new ArrayList<>();
  private ObjectInputStream ois;

  public ClientThread(Socket socket, User user, Controller controller) {
    this.socket = socket;
    this.user = user;
    this.controller = controller;
  }

  public ClientThread() {
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public void run() {
    inProgress = true;
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        if (inProgress) {
          pingServer();
          System.out.println("Ping server.");
        } else {
          timer.cancel();
        }
      }
    }, 3000, 10000); // 每隔5秒钟ping一次服务器

    try {
      while (true) {
        ois = new ObjectInputStream(socket.getInputStream());
        Object obj = ois.readObject();
        if (obj instanceof Message) {
          Message msg = (Message) obj;
          System.out.println(msg.getSentBy() + " says: " + msg.getData());
          act(msg);
        } else if (obj instanceof Chat) {
          Chat chat = (Chat) obj;
          act(chat);
        }
      }
    } catch (IOException e) {
      inProgress = false;
      System.out.println("progress stopped");

      // 显示警告框 - 服务器没有回复ping
      Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Offline Inform");
        alert.setHeaderText(null);
        ButtonType okButton = new ButtonType("OK",
            ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.setContentText("Connection error! Please check the server and login again.");
        alert.showAndWait();
      });
      controller.windowClosingForServer();

    } catch (ClassNotFoundException ee) {
      controller.windowClosing();
    }
  }

  private void pingServer() {
    try {
      // 向服务器发送ping消息
      Message pingMessage = new Message(user.getUserID(), DataType.MESSAGE_PING);
      ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
      oos.writeObject(pingMessage);
      oos.flush();

      isServerOnline = false;
//            // 等待服务器响应
//            synchronized (pingLock) {
//                pingLock.wait(10000); // 等待1秒钟
//            }

      Thread.sleep(100);

      if (!isServerOnline) {
        // 服务器未响应，认为服务器已离线
        Message offlineMessage = new Message(user.getUserID(),
            DataType.MESSAGE_OFFLINE_INFORM);

        System.out.println("[no ping reply]");
        offlineMessage.setSentBy("server");
        act(offlineMessage);
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void act(Message message) throws IOException, ClassNotFoundException {
    switch (message.getDataType()) {
      case DataType.MESSAGE_RET_ONLINE_FRIEND:
        onlineFriends = new ArrayList<>(Arrays.asList(message.getData().split(",")));
        onlineFriends.remove(user.getUserID());
        Platform.runLater(() -> {
          controller.currentOnlineCnt.setText("Online: " + (onlineFriends.size() + 1));
        });
        System.out.println("online friends: " + onlineFriends);
        break;
      case DataType.MESSAGE_TEXT_MESSAGE:
      case DataType.MESSAGE_FILE_MESSAGE:
      case DataType.MESSAGE_IMAGE_MESSAGE:
        if (message.getSentBy().equals(user.getUserID())) {
          return;
        }
        System.out.println(message.getSentBy() + " send a text/file/image message.");
        Chat chat = Controller.currentChats.get(message.getChatID());
        chat.getMessages().add(message);
        try {
          // 如果当前聊天窗口是这个消息的Chat，刷新聊天窗口
          if (chat.getChatID().equals(
              controller.chatList.getSelectionModel().getSelectedItem().getChatID())) {
            controller.chatContentList.getItems().add(message);
            controller.chatContentList.refresh();
          }
        } catch (NullPointerException e) {
          // 如果当前没有选择窗口，则选择这个Chat
          System.out.println("No chat selected.");
          controller.chatList.getSelectionModel().select(chat);
        }
        // 把这个chat放在最前面
        controller.items.remove(chat);
        controller.items.add(0, chat);
        controller.chatList.setItems(controller.items);
        controller.chatList.refresh();
        controller.chatContentList.refresh();
        break;
      case DataType.MESSAGE_RET_ALL_FRIEND:
        allFriends = new ArrayList<>(Arrays.asList(message.getData().split(",")));
        allFriends.remove(user.getUserID());
        System.out.println("all friends: " + allFriends);
        break;
      case DataType.MESSAGE_OFFLINE_INFORM:
        System.out.println(message.getSentBy() + " is offline.");
        if (!message.getSentBy().equals("server")) {
          onlineFriends.remove(message.getSentBy());
        }
        Platform.runLater(() -> {
          controller.currentOnlineCnt.setText("Online: " + (onlineFriends.size() + 1));

          // 显示警告框 - 服务器没有回复ping
          Alert alert = new Alert(Alert.AlertType.WARNING);
          alert.setTitle("Offline Inform");
          alert.setHeaderText(null);
          ButtonType okButton = new ButtonType("OK",
              ButtonBar.ButtonData.OK_DONE);
          alert.getButtonTypes().setAll(okButton);
          if (!message.getSentBy().equals("server")) {
            alert.setContentText("Your friend " + message.getSentBy() + " is offline.");
            alert.showAndWait();
          } else {
            alert.setContentText(
                "Oops, the server seems not reply the ping. Please check and retry.");
            alert.showAndWait();
//                        controller.windowClosingForServer();
          }
        });
        break;
      case DataType.MESSAGE_PING_RET:
        System.out.println("get server ping return");
        isServerOnline = true;
        break;
    }
  }

  private void act(Chat chat) {
    if (Controller.currentChats.containsKey(chat.getChatID())) {
      return;
    }
    System.out.println(
        user.getUserID() + " get a new chat [" + chat.getChatID() + "] from server.");
    Controller.currentChats.put(chat.getChatID(), chat);
    Platform.runLater(() -> {
      controller.items.add(chat);
      controller.chatList.setItems(controller.items);
      controller.chatList.refresh();
    });
  }
}
