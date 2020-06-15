package com.doit.net.Protocol;

import android.text.TextUtils;

import com.doit.net.Model.CacheManager;
import com.doit.net.Sockets.ServerSocketUtils;
import com.doit.net.Utils.UtilDataFormatChange;

import org.apache.http.util.ByteArrayBuffer;

import java.nio.charset.StandardCharsets;

/**
 * Author：Libin on 2020/6/10 11:05
 * Email：1993911441@qq.com
 * Describe：App发送指令
 */
public class LTESendManager {


    public static void sendData(String msgType, String msgCode, String packageContent) {
        sendData(null, msgType, msgCode, packageContent);
    }


    public static void sendData(String ip,String msgType, String msgCode, String packageContent) {
        int length = LTEPackage.HEAD_SIZE;
        byte[] magic = CacheManager.magic;
        byte[] msgId = UtilDataFormatChange.intToByteArray(LTEProtocol.getId());


        if (!TextUtils.isEmpty(packageContent)) {
            byte[] content = packageContent.getBytes(StandardCharsets.US_ASCII);
            length += content.length;
        }
        byte[] dataLength = UtilDataFormatChange.intToByteArray(length);
        byte[] cipherLength = new byte[4];
        byte[] crc = new byte[4];
        byte[] deviceName = CacheManager.deviceName;
        byte[] timestamp = UtilDataFormatChange.intToByteArray((int) (System.currentTimeMillis() / 1000));
        byte[] reserve = new byte[8];
        byte[] type = msgType.getBytes(StandardCharsets.US_ASCII);
        byte[] code = msgCode.getBytes(StandardCharsets.US_ASCII);


        ByteArrayBuffer byteArray = new ByteArrayBuffer(length);
        byteArray.append(magic, 0, magic.length);
        byteArray.append(msgId, 0, msgId.length);
        byteArray.append(dataLength, 0, dataLength.length);
        byteArray.append(cipherLength, 0, cipherLength.length);
        byteArray.append(crc, 0, crc.length);
        byteArray.append(deviceName, 0, deviceName.length);
        byteArray.append(timestamp, 0, timestamp.length);
        byteArray.append(reserve, 0, reserve.length);
        byteArray.append(type, 0, type.length);
        byteArray.append(code, 0, code.length);
        if (!TextUtils.isEmpty(packageContent)) {
            byte[] content = packageContent.getBytes(StandardCharsets.US_ASCII);
            byteArray.append(content, 0, content.length);
        }

        byte[] bytes = byteArray.toByteArray();
        if (TextUtils.isEmpty(ip)){
            ServerSocketUtils.getInstance().sendData(bytes);
        }else {
            ServerSocketUtils.getInstance().sendData(ip,bytes);
        }

    }

}
