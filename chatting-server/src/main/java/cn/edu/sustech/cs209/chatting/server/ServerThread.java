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

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void run() {
        System.out.println("服务器与客户【" + user.getUserID() + "】保持通信……");
        inProgress = true;
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
                case DataType.MESSAGE_COMM_MES:
                    // TODO: Common message display
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
