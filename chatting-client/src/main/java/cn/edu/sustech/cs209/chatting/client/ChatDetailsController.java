package cn.edu.sustech.cs209.chatting.client;

import com.sun.tools.javac.jvm.Items;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import cn.edu.sustech.cs209.chatting.common.Chat;

public class ChatDetailsController {

  @FXML
  private ListView<String> chatDetailsListView;

  public void setChatDetails(Chat chat) {
    // 在这里将聊天信息添加到ListView
    chatDetailsListView.getItems().add("Members:");
    chatDetailsListView.getItems().addAll(chat.getMembers());

    chatDetailsListView.getItems().add(""); // 空行

    chatDetailsListView.getItems().add("Last chat time:");
    chatDetailsListView.getItems().add(chat.getLatestTime().toString());
  }
}