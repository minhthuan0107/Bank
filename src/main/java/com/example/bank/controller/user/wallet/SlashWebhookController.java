package com.example.bank.controller.user.wallet;

import com.example.bank.service.wallet.user.SlashWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SlashWebhookController {

    private final SlashWebhookService slashWebhookService;

    @PostMapping("/slash")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload) {
        slashWebhookService.handleAsync(payload);
        return ResponseEntity.ok().build();
    }
}
