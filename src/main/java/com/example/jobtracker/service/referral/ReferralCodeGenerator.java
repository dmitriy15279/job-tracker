package com.example.jobtracker.service.referral;

import com.example.jobtracker.config.ReferralProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ReferralCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final int length;

    public ReferralCodeGenerator(ReferralProperties properties) {
        this.length = properties.codeLength();
    }

    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
