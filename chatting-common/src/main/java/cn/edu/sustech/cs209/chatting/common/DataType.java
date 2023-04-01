package cn.edu.sustech.cs209.chatting.common;

public interface DataType {
    String MESSAGE_LOGIN_PERMITTED = "1"; //允许登录
    String MESSAGE_LOGIN_FORBIDDEN = "2"; //登录失败
    String MESSAGE_COMM_MES = "3";//普通信息包
    String MESSAGE_GET_ONLINE_FRIEND = "4"; //请求得到在线用户列表
    String MESSAGE_RET_ONLINE_FRIEND = "5";//返回在线用户列表
    String MESSAGE_CLIENT_EXIT = "6"; //客户请求退出
    String MESSAGE_CLIENT_NO_EXIST = "7"; //发送目标不存在
    String MESSAGE_CLIENT_OFFLINE = "8"; //发送目标不在线
}
