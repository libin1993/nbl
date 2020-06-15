package com.doit.net.Sockets;

import com.doit.net.Data.DataCenterManager;
import com.doit.net.Data.LTEDataParse;
import com.doit.net.Model.CacheManager;
import com.doit.net.Utils.LogUtils;

import org.apache.commons.net.SocketClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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

                        Socket socket = mServerSocket.accept();  //获取socket
                        socket.setSoTimeout(READ_TIME_OUT);      //设置超时
                        String remoteIP = socket.getInetAddress().getHostAddress();  //远程ip
                        map.put(remoteIP, socket);   //存储socket
                        CacheManager.DEVICE_IP = remoteIP;  //当前设备ip

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
            Socket socket;
            LTEDataParse lteDataParse = new LTEDataParse();
            try {
                //获取当前socket
                socket = map.get(remoteIP);
                if (socket == null) {
                    return;
                }

                //获取输入流
                InputStream inputStream = socket.getInputStream();

                //循环接收数据
                while (true) {
                    //读取服务端发送给客户端的数据
                    receiveCount = inputStream.read(bytesReceived);
                    if (receiveCount <= -1) {
                        LogUtils.log("break read!");

                        onSocketChangedListener.onDisconnect();
                        closeSocket(remoteIP);  //关闭socket
                        lteDataParse.clearReceiveBuffer();
                        break;
                    }

                    lteDataParse.parseData(remoteIP ,bytesReceived, receiveCount);
                    //将数据交给数据中心管理员处理
//                    DataCenterManager.parseData(remoteIP, String.valueOf(remotePort),
//                            bytesReceived, receiveCount);
                    //收到数据
                }
            } catch (IOException ex) {
                LogUtils.log("读取错误:" + ex.toString());
                onSocketChangedListener.onDisconnect();
                closeSocket(remoteIP);  //关闭socket
//                DataCenterManager.clearDataBuffer(remoteIP);
                lteDataParse.clearReceiveBuffer();
            }
        }
    }

    //关闭socket
    public void closeSocket(String ip) {
        if (LTEDataParse.set !=null){
            LTEDataParse.set.remove(ip);
        }

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
        LogUtils.log("发送数据:"+ip+":" + Arrays.toString(tempByte));

        Socket socket = map.get(ip);
        if (socket != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(tempByte);
                    } catch (Exception ex) {
                        ex.printStackTrace();
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
