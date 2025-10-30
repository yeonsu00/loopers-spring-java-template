package com.loopers.application.user;

import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserFacade {

    private final UserService userService;

    public UserInfo signup(SignupCommand signupCommand) {
        User savedUser = userService.signup(signupCommand);
        return UserInfo.from(savedUser);
    }

}
