package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;

public class ClientThread extends Thread {

    private Socket socket;
    private User user;
    public Controller controller;
    public boolean inProgress = false;

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
        } catch (IOException | ClassNotFoundException e) {
            inProgress = false;
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
                    controller.chatContentList.refresh();
                }
                controller.chatList.refresh();
                break;
            case DataType.MESSAGE_RET_ALL_FRIEND:
                allFriends = new ArrayList<>(Arrays.asList(message.getData().split(",")));
                allFriends.remove(user.getUserID());
                System.out.println("all friends: " + allFriends);
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
        controller.items.add(chat);
        controller.chatList.setItems(controller.items);
        controller.chatList.refresh();
    }
}
