package com.example.bank.service.wallet.user.creation;

import com.example.bank.common.constants.MessageKeys;
import com.example.bank.common.exception.wallet.WalletException;
import com.example.bank.entity.wallet.Card;
import org.springframework.http.HttpStatus;

public final class CardProviderGuard {
    private CardProviderGuard() {}
    public static void requireSlash(Card card) {
        if ("INFINI".equals(card.getProvider()) || card.getSlashCardId() == null) {
            throw new WalletException(MessageKeys.CARD_PROVIDER_OPERATION_NOT_MIGRATED, HttpStatus.CONFLICT);
        }
    }
}
