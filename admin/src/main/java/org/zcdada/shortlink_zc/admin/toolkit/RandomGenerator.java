package org.zcdada.shortlink_zc.admin.toolkit;

import java.util.Random;

public class RandomGenerator {


    static String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static  String generateRandomString(){
        return  generateRandomString(6);
    }


    /**
     * 生成固定长度的数字+英文字母混杂字符串（含大小写）
     * @param length 期望的字符串长度（必须≥1）
     * @return 随机生成的字符串
     */
    public static String generateRandomString(int length) {
        // 字符库：0-9 + A-Z + a-z（共62种可能）
        Random random = new Random(); // 随机数生成器（够用！）
        StringBuilder sb = new StringBuilder(length); // 高效拼接字符串

        for (int i = 0; i < length; i++) {
            // 从字符库中随机选一个字符
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }


}
