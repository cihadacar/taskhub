package io.github.cihadacar.taskhub.user;

import io.github.cihadacar.taskhub.common.PageResponse;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResponse<UserResponse> list(int page, int size) {
        return PageResponse.from(userRepository.findAll().stream().map(UserResponse::from).toList(), page, size);
    }
}
