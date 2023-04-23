package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  private String userID;

  private String password;

  public User(String username, String password) {
    this.userID = username;
    this.password = password;
  }

  public User() {
  }

  public String getUserID() {
    return userID;
  }

  public String getPassword() {
    return password;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
