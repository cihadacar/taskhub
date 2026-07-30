package io.github.cihadacar.taskhub.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import io.github.cihadacar.taskhub.user.Role;
import io.github.cihadacar.taskhub.user.UserAccount;
import io.github.cihadacar.taskhub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesFieldsAndStoresOnlyThePasswordHash() {
        RegisterRequest request = new RegisterRequest(
                " member@example.com ", " member ", "Str0ngPass!");
        UserAccount saved = responseUser();
        when(passwordEncoder.encode("Str0ngPass!")).thenReturn("encoded-password");
        when(userRepository.save("member@example.com", "member", "encoded-password")).thenReturn(saved);

        var response = authService.register(request);

        assertThat(response.email()).isEqualTo("member@example.com");
        assertThat(response.username()).isEqualTo("member");
        verify(userRepository).save("member@example.com", "member", "encoded-password");
    }

    @Test
    void loginReturnsTheIssuedBearerToken() {
        UserAccount user = credentialUser();
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Str0ngPass!", "encoded-password")).thenReturn(true);
        when(tokenService.issue(user)).thenReturn(new TokenService.IssuedToken("signed-token", 3600));

        var response = authService.login(new LoginRequest(" member@example.com ", "Str0ngPass!"));

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
    }

    @Test
    void loginRejectsAnUnknownEmailWithoutIssuingAToken() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("missing@example.com", "Str0ngPass!")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void loginRejectsAnIncorrectPassword() {
        UserAccount user = credentialUser();
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("member@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private UserAccount responseUser() {
        UserAccount user = mock(UserAccount.class);
        when(user.id()).thenReturn(7L);
        when(user.email()).thenReturn("member@example.com");
        when(user.username()).thenReturn("member");
        when(user.roles()).thenReturn(Set.of(Role.USER));
        when(user.createdAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    private UserAccount credentialUser() {
        UserAccount user = mock(UserAccount.class);
        when(user.passwordHash()).thenReturn("encoded-password");
        return user;
    }
}
