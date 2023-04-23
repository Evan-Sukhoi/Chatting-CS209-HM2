package cn.edu.sustech.cs209.chatting.server;


import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import cn.edu.sustech.cs209.chatting.server.ServerService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

// server Main
public class Main {

  ServerSocket svrSocket;

  //User Database
  public static ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();

  public static void main(String[] args) {
    new Main();
  }

  public Main() {
    try {
      // 从文件中读取用户信息，初始化用户数据库
      String currentDir = System.getProperty("user.dir");
      userMap = ServerService.readUserMap(currentDir + "\\users.csv");
      // 从文件中读取Chat信息，初始化Chat数据库
      ServerService.loadChats();
      System.out.println("用户数据库初始化完成");

      System.out.println("在9999端口监听……");
      svrSocket = new ServerSocket(9999);

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                try {
//                    ServerSocket svrSocket = new ServerSocket(9998);
//                    Socket socket = new Socket();
//                    ServerService.sendServerOfflineMsg(socket);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
        System.out.println("Server closing. Saving all chats...");
        for (Chat chat : ServerService.chatList.values()) {
          ServerService.saveChatToFile(chat);
        }
        System.out.println("All chats saved. Goodbye!");
      }));

      while (true) {
        Socket socket = svrSocket.accept();
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        User user = (User) ois.readObject();
        //构建一个Message对象，准备回复
        Message msg = new Message();
        boolean login = false;
        //验证账号密码是否正确
        if (!isUser(user.getUserID())) {
          System.out.println("为用户【" + user.getUserID() + "】注册...");
          userMap.put(user.getUserID(), user);
          ServerService.writeUser(user, currentDir + "\\users.csv");
          System.out.println("用户【" + user.getUserID() + "】注册成功");
          login = true;
        } else if (!ServerService.checkThread(user.getUserID()) && userMap.get(
            user.getUserID()).getPassword().equals(user.getPassword())) {
          System.out.println("用户【" + user.getUserID() + "】登录成功");
          login = true;
        } else {
          msg.setDataType(DataType.MESSAGE_LOGIN_FORBIDDEN);
          oos.writeObject(msg);
          socket.close();
        }

        if (login) {
          msg.setDataType(DataType.MESSAGE_LOGIN_PERMITTED);
          oos.writeObject(msg);
          ServerThread thread = new ServerThread(socket, user);
          thread.start();

          if (ServerService.userChatMap.containsKey(user.getUserID())) {
            System.out.println("为用户【" + user.getUserID() + "】更新聊天和离线消息");
            for (Chat chat : ServerService.userChatMap.get(user.getUserID())) {
              thread.sendChat(chat);
            }
          }
          ServerService.addThread(user.getUserID(), thread);

          // 添加一个关闭线程的检查
          Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (thread.inProgress) {
              thread.stopRunning();
              try {
                ServerService.removeThread(user.getUserID());
              } catch (IOException e) {
                System.out.println("退出聊天时更新刷新在线用户列表失败");
              }
            }
          }));
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        svrSocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static boolean isUser(String userID) {
    if (userMap.containsKey(userID)) {
      return true;
    }
    return false;
  }
}
