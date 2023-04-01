package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerThread extends Thread {

    private Socket socket;
    private User user;
    private boolean inProgress = false;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public ServerThread(Socket socket, User user) {
        this.socket = socket;
        this.user = user;
    }

    public Socket getSocket(){
        return socket;
    }

    @Override
    public void run() {
        System.out.println("服务器与客户【" + user.getUserID() + "】保持通信……");
        while (inProgress) {
            try {
                ois = new ObjectInputStream(socket.getInputStream());
                Message msg = (Message) ois.readObject();
                act(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void act(Message msg) {
        try {
            switch (msg.getDataType()) {
                case DataType.MESSAGE_GET_ONLINE_FRIEND:
                    // TODO:
                    break;
                case DataType.MESSAGE_COMM_MES:
                    // TODO: Common message display
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
