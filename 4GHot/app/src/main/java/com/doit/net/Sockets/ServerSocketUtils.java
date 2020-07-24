package com.doit.net.Sockets;

import com.doit.net.Data.LTEDataParse;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;


/**
 * Author：Libin on 2020/5/20 15:43
 * Email：1993911441@qq.com
 * Describe：socket服务端
 */
public class ServerSocketUtils {
    private static ServerSocketUtils mInstance;
    private ServerSocket mServerSocket;

    private final static int READ_TIME_OUT = 60000;  //超时时间
    private Map<String, Socket> map = new HashMap<>();


    private ServerSocketUtils() {
        try {
            mServerSocket = new ServerSocket(NetConfig.LOCAL_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //获取单例对象
    public static ServerSocketUtils getInstance() {
        if (mInstance == null) {
            synchronized (ServerSocketUtils.class) {
                if (mInstance == null) {
                    mInstance = new ServerSocketUtils();
                }
            }

        }
        return mInstance;
    }


    /**
     * @param onSocketChangedListener 线程接收连接
     */
    public void startTCP(OnSocketChangedListener onSocketChangedListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (mServerSocket ==null){
                            mServerSocket = new ServerSocket(NetConfig.LOCAL_PORT);
                        }
                        Socket socket = mServerSocket.accept();  //获取socket
                        socket.setSoTimeout(READ_TIME_OUT);      //设置超时
                        String remoteIP = socket.getInetAddress().getHostAddress();  //远程ip
                        map.put(remoteIP, socket);   //存储socket

                        if (onSocketChangedListener != null) {
                            onSocketChangedListener.onConnect();
                        }

                        LogUtils.log("设备连接ip：" + remoteIP);

                        ReceiveThread receiveThread = new ReceiveThread(remoteIP, onSocketChangedListener);
                        receiveThread.start();

                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.log("tcp错误："+e.getMessage());

                    }
                }
            }
        }).start();

    }


    /**
     * 接收线程
     */
    public class ReceiveThread extends Thread {
        private OnSocketChangedListener onSocketChangedListener;
        private String remoteIP;

        public ReceiveThread(String remoteIP, OnSocketChangedListener onSocketChangedListener) {
            this.remoteIP = remoteIP;

            this.onSocketChangedListener = onSocketChangedListener;
        }

        @Override
        public void run() {
            super.run();
            //数据缓存
            byte[] bytesReceived = new byte[1024];
            //接收到流的数量
            int receiveCount;
            LTEDataParse lteDataParse = new LTEDataParse();

            try {
                //获取当前socket
                Socket socket = map.get(remoteIP);
                if (socket == null) {
                    return;
                }

                //获取输入流
                InputStream inputStream = socket.getInputStream();

                //循环接收数据
                while ((receiveCount = inputStream.read(bytesReceived)) != -1) {
                    lteDataParse.parseData(remoteIP ,bytesReceived, receiveCount);
                }

                LogUtils.log("socket被关闭，读取长度：" + receiveCount);

            } catch (IOException ex) {
                LogUtils.log("读取错误:" + ex.toString());
            }

            closeSocket(remoteIP);  //关闭socket
            CacheManager.removeEquip(remoteIP);
            lteDataParse.clearReceiveBuffer();
            onSocketChangedListener.onDisconnect();
        }
    }

    //关闭socket
    public void closeSocket(String ip) {

        Socket socket = map.get(ip);

        if (socket != null ) {
            if (!socket.isClosed()){
                //关闭socket
                try {
                    socket.shutdownInput();
                    socket.close();//临时
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            map.remove(ip);
        }

    }


    /**
     * 发送数据
     *
     * @param tempByte
     * @return
     */
    public void sendData(String ip,byte[] tempByte) {

        Socket socket = map.get(ip);
        if (socket != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(tempByte);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtils.log("socket发送失败："+e.getMessage());
                    }
                }
            }.start();
        }

    }


    /**
     * 遍历发送数据
     *
     * @param tempByte
     * @return
     */
    public void sendData(byte[] tempByte) {
        for (String ip : map.keySet()) {
            sendData(ip,tempByte);
        }
    }

}
