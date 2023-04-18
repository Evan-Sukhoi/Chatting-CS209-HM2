package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable{
    private static final long serialVersionUID = 1L; //版本兼容标志

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    private String dataType;

    private LocalDateTime sentTime;

    public Message(Long timestamp, String sentBy, String sendTo, String data, String dataType, LocalDateTime sentTime) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
        this.dataType = dataType;
        this.sentTime = sentTime;
    }

    public Message(){}

    public Message(String sendBy, String sendTo, String messageGetOnlineFriend) {
        this.sentBy = sendBy;
        this.sendTo = sendTo;
        this.dataType = messageGetOnlineFriend;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public String getDataType(){
        return dataType;
    }

    public LocalDateTime getSentTime() {
        return sentTime;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void setDataType(String dataType){
        this.dataType = dataType;
    }

    public void setSentTime(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }

}
