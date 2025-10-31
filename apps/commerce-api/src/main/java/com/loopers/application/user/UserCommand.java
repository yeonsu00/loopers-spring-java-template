package com.loopers.application.user;

import com.loopers.interfaces.api.user.UserV1Dto.SignupRequest;

public class UserCommand {

    public record SignupCommand(
            String loginId,
            String email,
            String birthDate,
            String gender
    ) {
        public static SignupCommand from(SignupRequest request) {
            return new SignupCommand(
                    request.loginId(),
                    request.email(),
                    request.birthDate(),
                    request.gender()
            );
        }
    }
}
