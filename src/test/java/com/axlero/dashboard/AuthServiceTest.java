package com.axlero.dashboard;

import com.axlero.dashboard.auth.AuthService;
import com.axlero.dashboard.auth.RegisterRequest;
import com.axlero.dashboard.auth.UserResponse;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersNormalizedEmailAndStoresOnlyEncodedPassword() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        UserResponse response = authService.register(
                new RegisterRequest(" Ada Lovelace ", " ADA@EXAMPLE.COM ", "strong-password"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(userCaptor.getValue().getName()).isEqualTo("Ada Lovelace");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$encoded");
        assertThat(response).isEqualTo(new UserResponse(42L, "Ada Lovelace", "ada@example.com"));
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada Lovelace", "ada@example.com", "strong-password")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void rejectsPasswordsThatExceedBcryptUtf8LengthLimit() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada Lovelace", "ada@example.com", "😀".repeat(19))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("72 UTF-8 bytes");

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
