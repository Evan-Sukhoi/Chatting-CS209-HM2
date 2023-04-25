package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Chat;
import cn.edu.sustech.cs209.chatting.common.DataType;
import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerService {

  public static ConcurrentHashMap<String, ServerThread> threadMap = new ConcurrentHashMap<>();
  public static ConcurrentHashMap<String, List<Chat>> userChatMap = new ConcurrentHashMap<>();
  public static ConcurrentHashMap<String, Chat> chatList = new ConcurrentHashMap<>();
  public static List<Chat> chatListByTime = new ArrayList<>();

  public static void addThread(String userID, ServerThread thread) throws IOException {
    threadMap.put(userID, thread);
    refreshOnlineList();
  }

  public static void refreshOnlineList() throws IOException {
    String onlineList = getOnlineList();
    for (ServerThread thread : threadMap.values()) {
      thread.sendOnlineList(onlineList);
    }
  }

  public static boolean checkThread(String userID) {
    return threadMap.containsKey(userID);
  }

  public static void saveChatToFile(Chat chat) {
    String fileName = "data/chat_" + chat.getChatID() + ".ser";
    try (ObjectOutputStream oos = new ObjectOutputStream(
        Files.newOutputStream(Paths.get(fileName)))) {
      oos.writeObject(chat);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void loadChats() {
    File folder = new File("data");
    if (!folder.exists()) {
      folder.mkdirs();
    }

    File[] files = folder.listFiles(
        (dir, name) -> name.startsWith("chat_") && name.endsWith(".ser"));
    if (files != null) {
      // 处理没有时间的聊天记录
      LocalDateTime defaultDateTime = LocalDateTime.of(2023, 1, 1, 0, 0); // 设置默认值
      for (File file : files) {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
          Chat chat = (Chat) ois.readObject();
          chat.setLatestTime(chat.getLatestTime() != null ? chat.getLatestTime() : defaultDateTime);
          chatList.put(chat.getChatID(), chat);
          chatListByTime.add(chat);
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
      }
      // sort chatListByTime by time
      chatListByTime.sort((c1, c2) -> c2.getLatestTime().compareTo(c1.getLatestTime()));
      for (Chat chat : chatListByTime) {
        for (String userId : chat.getMembers()) {
          userChatMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(chat);
        }
      }
    }
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

  public static void removeThread(String userID) throws IOException {
    if (threadMap.containsKey(userID)) {
      threadMap.remove(userID);
      System.out.println("用户【" + userID + "】已下线");
      refreshOnlineList();
    }
  }

  public static void sendPrvMsg(Message rtnMsg) {
    String receiver = rtnMsg.getSendTo();
    if (checkThread(receiver)) {
      ServerThread thread = threadMap.get(receiver);
      thread.sendMsg(rtnMsg);
    }
    Chat chat = chatList.get(rtnMsg.getChatID());
    chat.getMessages().add(rtnMsg);
    chat.setLatestTime(rtnMsg.getSentTime());
    chat.unread = true;
  }

  public static void sendGrpMsg(Message rtnMsg) {
    String chatID = rtnMsg.getChatID();
    Chat chat = chatList.get(chatID);
    String[] members = chat.getMembers();
    for (String member : members) {
      if (checkThread(member)) {
        ServerThread thread = threadMap.get(member);
        thread.sendMsg(rtnMsg);
      }
    }
    chat.getMessages().add(rtnMsg);
    chat.setLatestTime(rtnMsg.getSentTime());
    chat.unread = true;
  }

  public static void sendChat(Chat chat, String receiver) {
    ServerThread thread = threadMap.get(receiver);
    thread.sendChat(chat);
  }

  public static void sendOfflineMsg(String userID) {
    Message msg = new Message();
    threadMap.remove(userID);
    System.out.println("用户【" + userID + "】已下线");
    for (String user : threadMap.keySet()) {
      msg.setSendTo(user);
      msg.setDataType(DataType.MESSAGE_OFFLINE_INFORM);
      msg.setSentBy(userID);
      ServerThread thread = threadMap.get(user);
      System.out.println("发送离线消息给" + user);
      thread.sendMsg(msg);
    }
  }
  public static void sendPingRtn(Message msg) {
    Message rtnMsg = new Message();
    rtnMsg.setDataType(DataType.MESSAGE_PING_RET);
    ServerThread thread = threadMap.get(msg.getSentBy());
    thread.sendMsg(rtnMsg);

  }
}
