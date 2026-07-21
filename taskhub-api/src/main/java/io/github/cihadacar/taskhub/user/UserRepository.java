package io.github.cihadacar.taskhub.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    UserAccount save(String email, String username, String passwordHash);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(Long id);

    List<UserAccount> findAll();
}
