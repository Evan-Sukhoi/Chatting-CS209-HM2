package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import java.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerService {

    public static ConcurrentHashMap<String, ServerThread> threadMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, LinkedBlockingQueue<Message>> msgMap = new ConcurrentHashMap<>();

    public static void addThread(String userID, ServerThread thread) {
        threadMap.put(userID, thread);
    }

    public static boolean checkThread(String userID) {
        return threadMap.containsKey(userID);
    }

    public static String getOnlineList() {
        StringBuilder sb = new StringBuilder();
        for (String userID : threadMap.keySet()) {
            sb.append(userID).append(",");
        }
        return sb.toString();
    }

    public static String getAllList() {
        StringBuilder sb = new StringBuilder();
        for (String userID : Main.userMap.keySet()) {
            sb.append(userID).append(",");
        }
        return sb.toString();
    }

    public static void sendOffLineMessage(String userId, ObjectOutputStream oos) {
//        Vector<Message> vector = messageMap.get(userId); //得到库存信息
//        if (!(vector == null || vector.isEmpty())) {
//            try {
//                //说明当前用户有待发送消息
//                Socket socket = getSocketById(userId);
//                while (!vector.isEmpty()) {
//                    Message message = vector.get(0);
//                    //将消息按顺序发出去
//                    oos = new ObjectOutputStream(socket.getOutputStream());
//                    oos.writeObject(message);
//                    vector.remove(message);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public static ConcurrentHashMap<String, User> readUserMap(String filePath) {
        ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                String username = values[0].trim();
                String password = values[1].trim();
                User user = new User(username, password);
                userMap.put(username, user);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userMap;
    }

    public static void writeUser(User user, String filePath) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, true))) {
            bw.write(user.getUserID() + "," + user.getPassword());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
