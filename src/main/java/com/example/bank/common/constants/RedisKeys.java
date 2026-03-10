package com.example.bank.common.constants;

public class RedisKeys {
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
}
