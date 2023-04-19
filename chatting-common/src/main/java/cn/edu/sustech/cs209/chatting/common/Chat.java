package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public class Chat implements Serializable {
    private static final long serialVersionUID = 1L; //版本兼容标志
    private final String chatID;
    private String chatName; // 聊天名称(私聊名称可能不一样，故不final)
    private final String chatType; // 聊天名称
    private final String[] members; // 聊天成员

    List<Message> chatContentList = new ArrayList<>(); // 聊天内容

    LocalDateTime latestTime; // 最后聊天时间

    public Chat(String username1, String username2) {
        this.members = new String[]{username1, username2};
        this.chatType = "private";
        // 字典序
        if (username1.compareTo(username2) < 0) {
            chatID = username1 + "," + username2;
        } else {
            chatID = username2 + "," + username1;
        }
    }

    public Chat(List<AtomicReference<String>> users, String chatName) {
        this.members = new String[users.size()];
        this.chatType = "group";
        for (int i = 0; i < users.size(); i++) {
            this.members[i] = users.get(i).get();
        }
        this.chatName = chatName;
        Arrays.sort(this.members); // 字典序
        chatID = String.join(",", this.members);
    }

    public String getChatName(String username) {
        if (chatType.equals("private")) {
            if (members[0].equals(username)) {
                return members[1];
            } else {
                return members[0];
            }
        } else {
            return chatName;
        }
    }

    public List<Message> getMessages() {
        return chatContentList;
    }

    public String getChatType() {
        return chatType;
    }

    public String[] getMembers(){
        return members;
    }

    public String getChatID() {
        return chatID;
    }
    public String[] getMembers(String username) {
        if (chatType.equals("private")) {
            if (members[0].equals(username)) {
                return new String[]{members[1]};
            } else {
                return new String[]{members[0]};
            }
        } else {
            return members;
        }
    }
}
