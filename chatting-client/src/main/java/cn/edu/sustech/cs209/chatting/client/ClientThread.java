package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import java.io.ObjectInputStream;
import java.net.Socket;

public class ClientThread extends Thread {

    private Socket socket;
    private User user;
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
            ois = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Message message = (Message) ois.readObject();
                System.out.println(message.getSentBy() + " says: " + message.getData());
                action(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void action(Message message) {
        switch (message.getDataType()) {
            case DataType.MESSAGE_RET_ONLINE_FRIEND:
                // TODO: Online friend list
                break;
            case DataType.MESSAGE_COMM_MES:
                // TODO: Common message display
                break;

        }
    }
}
