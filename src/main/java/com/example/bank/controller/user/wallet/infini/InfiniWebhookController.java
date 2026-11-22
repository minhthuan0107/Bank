package com.example.bank.controller.user.wallet.infini;

import com.example.bank.service.wallet.infini.InfiniWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Receives Infini webhooks. Returns 200 only after the event is stored; anything else makes Infini retry. */
@RestController
@RequestMapping("${api.prefix}/webhook")
@RequiredArgsConstructor
public class InfiniWebhookController {

    private final InfiniWebhookService webhookService;

    @PostMapping("/infini")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Webhook-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-Webhook-Signature-Version", required = false) String signatureVersion,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            // Raw bytes: the signature covers the exact body.
            @RequestBody byte[] rawBody
    ) {
        webhookService.receive(timestamp, eventId, signatureVersion, signature, rawBody);
        return ResponseEntity.ok().build();
    }
}
