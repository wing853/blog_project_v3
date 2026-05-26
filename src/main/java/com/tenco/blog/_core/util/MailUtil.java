package com.tenco.blog._core.util;

import java.util.Random;

public class MailUtil {
    // 6자리 랜덤 숫자 생성(100000~999999)
    public static String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);

        return String.valueOf(code);
    }
}
