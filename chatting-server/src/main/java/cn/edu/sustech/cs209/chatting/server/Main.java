package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    ServerSocket svrSocket;
    private static ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        try {
            System.out.println("在9999端口监听……");
            svrSocket = new ServerSocket(9999);
            while (true) {
                Socket socket = svrSocket.accept();
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                User user = (User) ois.readObject();
                //构建一个Message对象，准备回复
                Message msg = new Message();
                //验证账号密码是否正确
                if (isUser(user.getUserID())) {
                    System.out.println("用户【" + user.getUserID() + "】不存在");
                    msg.setDataType(DataType.MESSAGE_LOGIN_PERMITTED);
                    oos.writeObject(msg);
                    ServerThread thread = new ServerThread(socket, user);
                    thread.start();
                    ServerService.addThread(user.getUserID(), thread);
                    ServerService.sendOffLineMessage(user.getUserID(), oos);
                } else {
                    msg.setDataType(DataType.MESSAGE_LOGIN_FORBIDDEN);
                    oos.writeObject(msg);
                    socket.close();
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
        if (!userMap.containsKey(userID)) {
            return true;
        }
        return false;
    }
}
