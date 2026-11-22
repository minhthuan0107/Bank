package com.example.bank.common.config.infini;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class InfiniSignerTest {
    @Test void signatureBindsExactBodyAndQuery() throws Exception {
        var keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] body = "{\"amount\":\"10.00\"}".getBytes(StandardCharsets.UTF_8);
        String digest = InfiniSigner.digest(body);
        String target = "/v2/cards/status?id=a%2Fb&page=1";
        String timestamp = "1794250000";
        String nonce = "nonce";
        String signature = InfiniSigner.sign(keys.getPrivate(), "key", "GET", target, timestamp, nonce, digest);
        String input = String.join("\n", "INFINI-OPENAPI-V2", "key", "GET", target, timestamp, nonce, digest) + "\n";
        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(keys.getPublic());
        verifier.update(input.getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(Base64.getDecoder().decode(signature)));
        verifier.initVerify(keys.getPublic());
        verifier.update(input.replace("a%2Fb", "a/b").getBytes(StandardCharsets.UTF_8));
        assertFalse(verifier.verify(Base64.getDecoder().decode(signature)));
        assertNotEquals(digest, InfiniSigner.digest("{}".getBytes(StandardCharsets.UTF_8)));
        assertTrue(InfiniSigner.digest(new byte[0]).startsWith("SHA-256="));
    }
}
