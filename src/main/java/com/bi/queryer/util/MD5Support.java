package com.bi.queryer.util;

import java.security.MessageDigest;
import java.util.Random;

public class MD5Support {
    private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public final static String MD5(String s) {

        try {
            byte[] strTemp = s.getBytes("UTF-8");
            //byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                //右移四位并截掉高四位
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                //直接截掉高四位
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }

    public final static String MD5(String s, String encoding) {
        try {
            byte[] strTemp;
            if (encoding != null)
                strTemp = s.getBytes(encoding);
            else
                strTemp = s.getBytes();
            //byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                //右移四位并截掉高四位
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                //直接截掉高四位
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }
    
//  94e8cde4612b3fd390677d42e7b22002
//    public static void main(String[] avgs){
//    	String s="1qaz!QAZ";
//    	String s1=MD5Support.MD5(s);
//    	System.out.println(s1);
//    }
    
    public static String randomString(int length) { //length表示生成字符串的长度  
        String base = "abcdefghijklmnopqrstuvwxyz0123456789";     
        Random random = new Random();     
        StringBuffer sb = new StringBuffer();     
        for (int i = 0; i < length; i++) {     
            int number = random.nextInt(base.length());     
            sb.append(base.charAt(number));     
        }     
        return sb.toString();     
     }  
    
	public static void main(String [] a){
		System.out.println(MD5Support.MD5("洗美销售&项目覆盖"));
		System.out.println(MD5Support.MD5("bi@test"));
		String str = "bowen.jiang";
		/*String encodeStr = Base64.encode(str.getBytes());
		System.out.println("base64:" +  encodeStr);
		try {
			System.out.println(new String(Base64.decode(encodeStr)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//cf03bcc2b6bbc85e9276d95b5f6e871c
		//cf03bcc2b6bbc85e9276d95b5f6e871c
		System.out.println("--------------------------------------");
		String userName = "bitest";
		System.out.println(MD5(userName));
		System.out.println(randomString(16));
		String testUrl = "userName=" + userName + "&loginToken=" + MD5(userName) + randomString(16);
		System.out.println("testUrl:" + testUrl);
	}
}
