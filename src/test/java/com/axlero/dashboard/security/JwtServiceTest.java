package com.axlero.dashboard.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void rejectsAnUnconfiguredSecretInsteadOfUsingAFallback() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(" ", 86_400_000));
    }
}
