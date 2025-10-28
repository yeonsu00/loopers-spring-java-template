package com.loopers.application.user;

import com.loopers.interfaces.api.user.UserV1Dto.SignupRequest;
import java.time.LocalDate;

public class UserCommand {

    public record SignupCommand(
            String loginId,
            String email,
            LocalDate birthDate,
            String gender
    ) {
        public static SignupCommand from(SignupRequest request) {
            return new SignupCommand(
                    request.loginId(),
                    request.email(),
                    LocalDate.parse(request.birthDate()),
                    request.gender()
            );
        }
    }
}
