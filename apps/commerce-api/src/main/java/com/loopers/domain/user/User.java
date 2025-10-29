package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Builder
    private User(String loginId, String email, LocalDate birthDate, Gender gender) {
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
                .gender(Gender.fromCode(signupCommand.gender()))
                .build();
    }
}
