package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerService {
    public static ConcurrentHashMap<String, ServerThread> threadMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, LinkedBlockingQueue<Message>> msgMap = new ConcurrentHashMap<>();



    public static void addThread(String userID, ServerThread thread) {
        threadMap.put(userID, thread);
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

}
