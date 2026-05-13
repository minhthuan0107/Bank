package com.example.bank.listener;

import com.example.bank.event.DepositOrderCreatedEvent;
import com.example.bank.repository.user.UserRepository;
import com.example.bank.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DepositOrderMailListener {

    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDepositOrderCreated(DepositOrderCreatedEvent event) {
        List<String> adminEmails = userRepository.findActiveAdminEmails();

        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn(
                    "DEPOSIT-ORDER-MAIL-SKIP reason=NO_ACTIVE_ADMIN_EMAIL orderNo={}",
                    event.orderNo()
            );
            return;
        }

        for (String adminEmail : adminEmails) {
            mailService.sendDepositOrderCreatedToAdmin(adminEmail, event);
        }

        log.info(
                "DEPOSIT-ORDER-MAIL-ENQUEUED orderNo={} adminCount={}",
                event.orderNo(),
                adminEmails.size()
        );
    }
}
