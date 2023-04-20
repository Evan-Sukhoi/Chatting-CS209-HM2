package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerThread extends Thread {

    private Socket socket;
    private User user;
    public boolean inProgress = false;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public ServerThread(Socket socket, User user) {
        this.socket = socket;
        this.user = user;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        System.out.println("服务器与客户【" + user.getUserID() + "】保持通信……");
        inProgress = true;
        try {
            while (inProgress) {
                ois = new ObjectInputStream(socket.getInputStream());
                Object obj = ois.readObject();

                if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    System.out.println("Server received Message");
                    act(msg);
                } else if (obj instanceof Chat) {
                    Chat chat = (Chat) obj;
                    System.out.println("Server received Chat");
                    act(chat);
                } else {
                    System.out.println("Unknown object received.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stopRunning();
        }
    }

    public void stopRunning() {
        inProgress = false;
        System.out.println("服务器与客户【" + user.getUserID() + "】通信结束");
        try {
            if (ois != null) {
                ois.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (socket != null) {
                socket.close();
            }
            // 通知 ServerService 移除该用户
            ServerService.removeThread(user.getUserID());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void act(Message msg) {
        try {
            switch (msg.getDataType()) {

                case DataType.MESSAGE_GET_ONLINE_FRIEND:
                    System.out.println(
                        "服务器收到来自客户【" + user.getUserID() + "】的在线好友列表请求");
                    //构建一个Message对象，准备回复
                    Message rtnMsg = new Message();
                    rtnMsg.setDataType(DataType.MESSAGE_RET_ONLINE_FRIEND);
                    rtnMsg.setSendTo(msg.getSentBy());
                    rtnMsg.setSentBy("Server");
                    rtnMsg.setData(ServerService.getOnlineList());
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(rtnMsg);
                    break;

                case DataType.MESSAGE_GET_ALL_FRIEND:
                    System.out.println(
                        "服务器收到来自客户【" + user.getUserID() + "】的所有好友列表请求");
                    //构建一个Message对象，准备回复
                    Message rtnMsg2 = new Message();
                    rtnMsg2.setDataType(DataType.MESSAGE_RET_ALL_FRIEND);
                    rtnMsg2.setSendTo(msg.getSentBy());
                    rtnMsg2.setSentBy("Server");
                    rtnMsg2.setData(ServerService.getAllList());
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    oos.writeObject(rtnMsg2);
                    break;

                case DataType.MESSAGE_TEXT_MESSAGE:
                    // TODO: Common message display
                    System.out.println("服务器收到来自客户【" + user.getUserID() + "】的文本消息");
                    //准备回复
                    if (ServerService.chatList.get(msg.getChatID()).getChatType().equals("private")) {
                        System.out.println("服务器将客户【" + user.getUserID() + "】的私聊文本消息发送给客户【" + msg.getSendTo() + "】");
                        ServerService.sendPrvMsg(msg);
                    } else if (ServerService.chatList.get(msg.getChatID()).getChatType().equals("group")) {
                        System.out.println("服务器将客户【" + user.getUserID() + "】的群聊文本消息发送给群聊【" + msg.getSendTo() + "】");
                        ServerService.sendGrpMsg(msg);
                    }
                    break;

                case DataType.MESSAGE_CLIENT_EXIT:
                    System.out.println("服务器收到来自客户【" + user.getUserID() + "】的退出请求");
                    inProgress = false;
                    break;

                case DataType.MESSAGE_FILE_MESSAGE:
                case DataType.MESSAGE_IMAGE_MESSAGE:
                    //准备回复
                    System.out.println("服务器收到来自客户【" + user.getUserID() + "】的文件/图片消息: " + msg.getFileName());
                    if (ServerService.chatList.get(msg.getChatID()).getChatType().equals("private")) {
                        System.out.println("服务器将客户【" + user.getUserID() + "】的私聊文件/图片消息发送给客户【" + msg.getSendTo() + "】");
                        ServerService.sendPrvMsg(msg);
                    } else if (ServerService.chatList.get(msg.getChatID()).getChatType().equals("group")) {
                        System.out.println("服务器将客户【" + user.getUserID() + "】的群聊文件/图片消息发送群聊【" + msg.getSendTo() + "】");
                        ServerService.sendGrpMsg(msg);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void act(Chat chat) {
        ServerService.chatList.put(chat.getChatID(), chat);
        ServerService.saveChatToFile(chat);
        // Server add the chat
        for (String mem : chat.getMembers()) {
            if (ServerService.userChatMap.containsKey(mem)) {
                ServerService.userChatMap.get(mem).add(chat);

            } else {
                List<Chat> chats = new ArrayList<>();
                chats.add(chat);
                ServerService.userChatMap.put(mem, chats);
            }
        }
        // send the chat to members
        for (String mem : chat.getMembers()) {
            if (ServerService.checkThread(mem)) {
                ServerService.sendChat(chat, mem);
            }
        }
    }

    public void sendChat(Chat chat) {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(chat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMsg(Message msg) {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendOnlineList(String onlineList) throws IOException {
        //构建一个Message对象，把退出后的在线好友列表发送给客户端
        Message rtnMsg4 = new Message();
        rtnMsg4.setDataType(DataType.MESSAGE_RET_ONLINE_FRIEND);
        rtnMsg4.setSendTo(user.getUserID());
        rtnMsg4.setSentBy("Server");
        rtnMsg4.setData(ServerService.getOnlineList());
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(rtnMsg4);
    }
}
