package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    private String loginId;

    private String email;

    private LocalDate birthDate;

    private String gender;

    @Builder
    private User(String loginId, String email, LocalDate birthDate, String gender) {
        this.loginId = loginId;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public User() {
    }

    public static User createUser(UserCommand.SignupCommand signupCommand) {
        return User.builder()
                .loginId(signupCommand.loginId())
                .email(signupCommand.email())
                .birthDate(LocalDate.parse(signupCommand.birthDate()))
                .gender(signupCommand.gender())
                .build();
    }
}
