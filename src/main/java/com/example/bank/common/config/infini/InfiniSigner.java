package com.example.bank.common.config.infini;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public final class InfiniSigner {
    private InfiniSigner() {}

    public static String digest(byte[] body) throws GeneralSecurityException {
        return "SHA-256=" + Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(body));
    }

    public static String sign(PrivateKey key, String keyId, String method, String target,
                              String timestamp, String nonce, String digest) throws GeneralSecurityException {
        String input = String.join("\n", "INFINI-OPENAPI-V2", keyId, method,
                target, timestamp, nonce, digest) + "\n";
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(key);
        signer.update(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signer.sign());
    }
}
