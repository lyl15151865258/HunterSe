package com.ott.hunterse.tcp;

import com.ott.hunterse.bean.EventMsg;
import com.ott.hunterse.constant.Constant;
import com.ott.hunterse.utils.MathUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCP客户端模式
 * Created by LiYuliang on 2022/06/13 0013.
 *
 * @author LiYuliang
 * @version 2022/06/13
 */

public class TcpClient implements Runnable {

    private Socket socket;
    private boolean isRunning;
    private InputStream inputStream;
    private OutputStream outputStream;
    private InetAddress inetAddress;
    private int port;

    public TcpClient() {
        isRunning = true;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        if (socket == null) {
            try {
                socket = new Socket(inetAddress, port);
                new SocketThread(socket).start();
                EventMsg msg = new EventMsg();
                msg.setTag(Constant.TCP_CLIENT_CONNECTED_SUCCESS);
                msg.setMsg(socket.getRemoteSocketAddress().toString());
                EventBus.getDefault().post(msg);
            } catch (IOException e) {
                e.printStackTrace();
                EventMsg msg = new EventMsg();
                msg.setTag(Constant.TCP_CLIENT_DESTINATION_NOT_FOUND);
                EventBus.getDefault().post(msg);
            }
        }
    }

    public void close() {
        if (socket != null) {
            try {
                socket.close();
                socket = null;
                isRunning = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SocketThread extends Thread {

        private final Socket socket;

        private SocketThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                while (isRunning) {
                    inputStream = socket.getInputStream();
                    if (inputStream.available() > 0) {
                        final byte[] buffer = new byte[1024];
                        final int len = inputStream.read(buffer);
                        String data = MathUtils.bytesToHexString(buffer, len);
                        EventMsg msg = new EventMsg();
                        msg.setTag(Constant.TCP_CLIENT_CORRECT_READ);
                        msg.setMsg(data);
                        EventBus.getDefault().post(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                EventMsg msg = new EventMsg();
                msg.setTag(Constant.TCP_CLIENT_CONNECTED_FAIL);
                EventBus.getDefault().post(msg);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendMessage(String message) {
        try {
            byte[] data = MathUtils.hexStringToByte(message);
            outputStream = socket.getOutputStream();
            outputStream.write(data);
            EventMsg msg = new EventMsg();
            msg.setTag(Constant.TCP_CLIENT_CORRECT_WRITE);
            EventBus.getDefault().post(msg);
        } catch (IOException e) {
            EventMsg msg = new EventMsg();
            msg.setTag(Constant.TCP_CLIENT_ERROR_WRITE);
            EventBus.getDefault().post(msg);
        }
    }

}
