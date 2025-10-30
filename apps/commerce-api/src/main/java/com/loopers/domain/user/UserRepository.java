package com.loopers.domain.user;

public interface UserRepository {
    User save(User user);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);
}
