package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.LoginRequest;
import com.axlero.dashboard.dto.RegisterRequest;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void existingUserCanLogInWithStoredPasswordHash() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setPassword("existing-password-hash");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "existing-password-hash"))
                .thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("signed-token");

        var response = authService.login(new LoginRequest("alice", "correct-password"));

        assertEquals("alice", response.username());
        assertEquals("signed-token", response.token());
        verify(userRepository).findByUsername("alice");
        verify(passwordEncoder).matches("correct-password", "existing-password-hash");
    }

    @Test
    void existingUserCanLogInWithEmail() {
        User user = new User();
        user.setUsername("alice");
        user.setEmail("alice@example.test");
        user.setPassword("existing-password-hash");
        when(userRepository.findByUsername("alice@example.test"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("alice@example.test"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "existing-password-hash"))
                .thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("signed-token");

        assertEquals(
                "alice",
                authService.login(new LoginRequest(
                        "alice@example.test", "correct-password")).username());
        verify(userRepository).findByEmail("alice@example.test");
    }

    @Test
    void invalidCredentialsRaiseAuthenticationFailure() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequest("alice", "wrong-password")));
    }

    @Test
    void registrationPersistsNewAccountThroughCanonicalRepository() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.test")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken("alice")).thenReturn("signed-token");

        var response = authService.register(
                new RegisterRequest("alice", "alice@example.test", "new-password"));

        assertEquals("alice", response.username());
        verify(userRepository).save(any(User.class));
    }
}
