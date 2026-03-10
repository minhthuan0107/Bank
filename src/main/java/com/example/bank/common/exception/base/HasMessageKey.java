package com.example.bank.common.exception.base;
/*
 * Interface để lấy message key từ các exception
 * Được sử dụng trong JwtExceptionFilter để xử lý lỗi một cách thống nhất
 */
public interface HasMessageKey {
    String getMessageKey();
}
