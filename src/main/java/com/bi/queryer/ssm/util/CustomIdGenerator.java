package com.bi.queryer.ssm.util;

import java.util.concurrent.ThreadLocalRandom;

public class CustomIdGenerator {

    // 定义ID允许的字符池：数字 + 大小写字母（共62个字符）
    private static final char[] CHAR_POOL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int POOL_LENGTH = CHAR_POOL.length;

    /**
     * 生成指定长度的随机字符串
     * @param length 字符长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("字符长度必须大于0");
        }
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < length; i++) {
            // 随机选取字符池中的字符
            int randomIndex = random.nextInt(POOL_LENGTH);
            sb.append(CHAR_POOL[randomIndex]);
        }
        return sb.toString();
    }
}
