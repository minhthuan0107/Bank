package com.example.bank.common.constants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedisKeys {
    private static final String PREFIX = "bank";
    private RedisKeys() {
    }

    private static final DateTimeFormatter HOUR_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHH");

    public static String otpValueKey(String id) {
        return "otp:value:" + id;
    }

    public static String otpCooldownKey(String id) {
        return "otp:cooldown:" + id;
    }

    public static String otpHourlyKey(String id) {
        return "otp:hour:" + id;
    }

    public static String otpIpHourlyKey(String ip) {
        return "otp:ip:hour:" + ip;
    }

    public static String depositOrderLimit(Long userId) {
        return "wallet:deposit:order:limit:" + userId + ":" +
                LocalDateTime.now().format(HOUR_FORMAT);
    }

    public static String withdrawOtpValueKey(String orderNo) {
        return "otp:withdraw:value:" + orderNo;
    }

    public static String withdrawOtpCooldownKey(String email) {
        return "otp:withdraw:cooldown:email:" + email;
    }

    public static String withdrawOtpEmailHourlyKey(String email) {
        return "otp:withdraw:hour:email:" + email;
    }

    public static String withdrawOtpAttemptKey(String orderNo) {
        return "otp:withdraw:attempt:" + orderNo;
    }

    public static String withdrawLockKey(Long userId) {
        return PREFIX + ":withdraw:lock:user:" + userId;
    }

    public static String withdrawRateLimitUser(Long userId) {
        return "rl:withdraw:user:" + userId;
    }

    public static String withdrawRateLimitIp(String ip) {
        return "rl:withdraw:ip:" + ip;
    }
}








