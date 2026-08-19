package com.ntt.language_center_management.util;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class JwtUtils {

    private final String secret;
    private final long expirationMs;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret phải có ít nhất 32 byte");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("Thời hạn JWT phải lớn hơn 0");
        }
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        try {
            JWSSigner signer = new MACSigner(secret);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(email)
                    .expirationTime(new Date(System.currentTimeMillis() + expirationMs))
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet);

            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Lỗi tạo token", exception);
        }
    }

    public String validateTokenAndGetUsername(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token không được để trống");
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Thuật toán token không hợp lệ");
            }

            JWSVerifier verifier = new MACVerifier(secret);

            if (signedJWT.verify(verifier)) {
                Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
                if (expiration != null && expiration.after(new Date())) {
                    String subject = signedJWT.getJWTClaimsSet().getSubject();
                    if (subject == null || subject.isBlank()) {
                        throw new IllegalArgumentException("Token không chứa thông tin người dùng");
                    }
                    return subject;
                }
                throw new IllegalArgumentException("Token đã hết hạn!");
            }
            throw new IllegalArgumentException("Token không hợp lệ");
        } catch (JOSEException | ParseException exception) {
            throw new IllegalArgumentException("Token không đúng định dạng", exception);
        }
    }
}
