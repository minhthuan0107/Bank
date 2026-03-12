package com.example.bank.common.constants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RedisKeys {
    private RedisKeys() {}
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




}
