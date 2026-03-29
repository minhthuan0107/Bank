package com.example.bank.enums.wallet;

public enum CardTxnStatus {
    PENDING,    // vừa tạo, chưa xử lý xong
    SUCCESS,    // thành công
    FAILED,     // thất bại
    CANCELLED   // bị hủy (optional)
}
