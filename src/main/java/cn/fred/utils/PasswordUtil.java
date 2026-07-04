package cn.fred.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public static String encode(String rawPwd) {
        return encoder.encode(rawPwd);
    }
    public static boolean match(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}