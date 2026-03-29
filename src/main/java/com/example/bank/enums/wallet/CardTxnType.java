package com.example.bank.enums.wallet;

public enum CardTxnType {
    TOPUP,      // nạp tiền vào thẻ (tăng limit)
    WITHDRAW,   // rút tiền khỏi thẻ (giảm limit)
    PAYMENT,    // chi tiêu qua thẻ (Slash webhook về)
    REFUND      // hoàn tiền

}
