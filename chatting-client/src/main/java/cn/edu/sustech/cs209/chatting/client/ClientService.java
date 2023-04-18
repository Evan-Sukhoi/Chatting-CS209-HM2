package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import cn.edu.sustech.cs209.chatting.common.DataType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientService {

    private User user = new User();
    ClientThread thread = new ClientThread();
    private static Socket socket = new Socket();
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    boolean logged = false;

    boolean checkUser(String userID, String password) throws ConnectionFailedException {
        try {
            user.setUserID(userID);
            user.setPassword(password);
            // use client socket to connect to server
            socket = new Socket(InetAddress.getByName("127.0.0.1"), 9999);
            // set "oos" to output stream of client socket
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(user);
            // receive the message from server
            ois = new ObjectInputStream(socket.getInputStream());
            Message msg = (Message) ois.readObject();

            if (msg.getDataType().equals(DataType.MESSAGE_LOGIN_PERMITTED)) {
                thread = new ClientThread(socket, user);
                thread.start();
                logged = true;
            } else {
                socket.close();
            }

        } catch (IOException e) {
            throw new ConnectionFailedException("Failed to connect to server.");
        } catch (ClassNotFoundException e) {
            throw new ConnectionFailedException("Failed to receive server message.");
        }
        return logged;
    }

    public void getOnlineList() {
        Message msg = new Message(user.getUserID(), "Server", DataType.MESSAGE_GET_ONLINE_FRIEND);
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getAllFriendList() {
        Message msg = new Message(user.getUserID(), "Server", DataType.MESSAGE_GET_ALL_FRIEND);
        try{
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ConnectionFailedException extends Exception {

        public ConnectionFailedException(String message) {
            super(message);
        }
    }
}
