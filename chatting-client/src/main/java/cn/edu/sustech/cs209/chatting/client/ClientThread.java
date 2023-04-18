package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import com.sun.tools.classfile.ConstantPool.CPRefInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientThread extends Thread {

    private Socket socket;
    private User user;

    List<String> onlineFriends;

    List<String> allFriends;
    private ObjectInputStream ois;

    public ClientThread(Socket socket, User user) {
        this.socket = socket;
        this.user = user;
    }

    public  ClientThread(){}

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public void setUser(User user){
        this.user = user;
    }

    @Override
    public void run() {
        try {
            while (true) {
                ois = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) ois.readObject();
                System.out.println(msg.getSentBy() + " says: " + msg.getData());
                action(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void action(Message message) throws IOException, ClassNotFoundException {
        switch (message.getDataType()) {
            case DataType.MESSAGE_RET_ONLINE_FRIEND:
                onlineFriends = new ArrayList<>(Arrays.asList(message.getData().split(",")));
                onlineFriends.remove(user.getUserID());
                System.out.println("online friends: " + onlineFriends);
                break;
            case DataType.MESSAGE_COMM_MES:
                // TODO: Common message display
                break;
            case DataType.MESSAGE_RET_ALL_FRIEND:
                allFriends = new ArrayList<>(Arrays.asList(message.getData().split(",")));
                allFriends.remove(user.getUserID());
                System.out.println("all friends: " + allFriends);
                break;

        }
    }
}
