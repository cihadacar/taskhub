package io.github.cihadacar.taskhub.security;

import io.github.cihadacar.taskhub.user.UserAccount;
import io.github.cihadacar.taskhub.user.UserRepository;
import io.github.cihadacar.taskhub.user.UserResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public UserResponse register(RegisterRequest request) {
        UserAccount user = userRepository.save(request.email().strip(), request.username().strip(),
                passwordEncoder.encode(request.password()));
        return UserResponse.from(user);
    }

    public TokenResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(request.email().strip())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        TokenService.IssuedToken token = tokenService.issue(user);
        return new TokenResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}
