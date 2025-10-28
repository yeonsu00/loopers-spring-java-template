package com.loopers.domain.user;

import com.loopers.application.user.UserCommand.SignupCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User signup(SignupCommand signupCommand) {
        User user = User.createUser(signupCommand);
        return userRepository.save(user);
    }

}
