package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ClientService {

    private User user = new User();
    private Socket socket = new Socket();
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    boolean logged = false;

    boolean checkUser(String userID) {
        try{
            user.setUserID(userID);
            // use client socket to connect to server
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            // set "oos" to output stream of client socket
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(user);
            // receive the message from server
            ois = new ObjectInputStream(socket.getInputStream());
            Message msg = (Message) ois.readObject();

            if (msg.getDataType().equals(DataType.MESSAGE_LOGIN_PERMITTED)){
                ClientThread thread = new ClientThread(socket, user);
                 thread.start();
                 logged = true;
            }else {
                socket.close();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return logged;
    }

}
