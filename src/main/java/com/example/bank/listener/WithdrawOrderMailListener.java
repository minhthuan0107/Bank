package com.example.bank.listener;

import com.example.bank.event.WithdrawOrderPendingAdminEvent;
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
public class WithdrawOrderMailListener {

    private final UserRepository userRepository;
    private final MailService mailService;

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWithdrawOrderPendingAdmin(WithdrawOrderPendingAdminEvent event) {
        List<String> adminEmails = userRepository.findActiveAdminEmails();
        if (adminEmails == null || adminEmails.isEmpty()) {
            log.warn(
                    "WITHDRAW-ORDER-MAIL-SKIP reason=NO_ACTIVE_ADMIN_EMAIL orderNo={}",
                    event.orderNo()
            );
            return;
        }
        for (String adminEmail : adminEmails) {
            mailService.sendWithdrawOrderPendingAdminToAdmin(adminEmail, event);
        }
        log.info(
                "WITHDRAW-ORDER-MAIL-ENQUEUED orderNo={} adminCount={}",
                event.orderNo(),
                adminEmails.size()
        );
    }
}
