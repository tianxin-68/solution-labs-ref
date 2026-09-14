package com.bi.queryer.util;

import cn.hutool.core.util.StrUtil;
import com.github.luben.zstd.Zstd;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @Author: contributor
 * @CreateTime: 2024-05-28  10:25
 */
public class ZstdUtil {

    private static Base64 base64 = new Base64();

    public static String compress(Object data) {
        byte[] compressArray = Zstd.compress(objectToByte(data));
        String compressStr = base64.encodeAsString(compressArray);
        return compressStr;
    }

    public static Object decompress(String compressStr) {
        if (StrUtil.isBlank(compressStr)) {
            return compressStr;
        }
        byte[] compressArray = base64.decode(compressStr);
        int size = (int) Zstd.decompressedSize(compressArray);
        byte[] decompressArray = new byte[size];
        Zstd.decompress(decompressArray, compressArray);
        return byteToObject(decompressArray);
    }

    public static byte [] objectToByte(Object obj) {
        byte[] bytes = null;
        try {

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(obj);

            bytes = bo.toByteArray();

            bo.close();
            oo.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }

    private  static Object byteToObject( byte [] bytes) {
        Object obj = null;
        try {

            ByteArrayInputStream bi = new ByteArrayInputStream(bytes);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();

            bi.close();
            oi.close();
        } catch (Exception e) {
            System.out.println("translation" + e.getMessage());
            e.printStackTrace();
        }
        return obj;
    }

}
