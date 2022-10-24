package com.ott.hunterse.constant;

/**
 * 常量值
 * Created at 2022-06-14 0014 18:04:17
 *
 * @author LiYuliang
 * @version 1.0
 */
public class Constant {

    // 默认运动指令（静止）,需以小于500ms的周期（建议周期20ms）发送
    public static final String DEFAULT_CAN_COMMAND = "08000001110000000000000000";
    // 小车CAN转Wifi网关的TCP Server的IP地址
    public static final String TCP_SERVER_IP = "192.168.1.11";
    // 小车CAN转Wifi网关的TCP Server的端口
    public static final int TCP_SERVER_PORT = 4001;
    // 指令发送间隔（单位ms），20ms即50Hz
    public static final int SEND_FREQUENCY = 20;

    public final static int TCP_CLIENT_CORRECT_READ = 0x11;               //数据读取成功
    public final static int TCP_CLIENT_CORRECT_WRITE = 0x12;              //数据发送成功
    public final static int TCP_CLIENT_ERROR_WRITE = 0x13;                //数据发送失败
    public final static int TCP_CLIENT_CONNECTED_FAIL = 0x14;             //连接断开
    public final static int TCP_CLIENT_CONNECTED_SUCCESS = 0x15;          //连接服务器成功
    public final static int TCP_CLIENT_DESTINATION_NOT_FOUND = 0x16;      //服务器地址不存在

}