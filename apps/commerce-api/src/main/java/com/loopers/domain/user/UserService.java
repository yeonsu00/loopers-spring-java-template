package com.loopers.domain.user;

import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User signup(SignupCommand signupCommand) {
        if (userRepository.existsByLoginId(signupCommand.loginId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 ID입니다.");
        }

        if (userRepository.existsByEmail(signupCommand.email())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 이메일입니다.");
        }

        User user = User.createUser(signupCommand);
        return userRepository.save(user);
    }

}
