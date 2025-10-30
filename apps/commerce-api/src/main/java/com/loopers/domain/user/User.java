package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Builder
    private User(String loginId, String email, String birthDate, String gender) {
        validateLoginId(loginId);
        validateEmail(email);
        validateBirthDate(birthDate);

        this.loginId = loginId;
        this.email = email;
        this.birthDate = LocalDate.parse(birthDate);
        this.gender = Gender.fromCode(gender);
    }

    private static void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 필수입니다.");
        }
        if (!loginId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자로만 구성되어야 하며, 10자 이내여야 합니다.");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 필수입니다.");
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.");
        }
    }

    private static void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 필수입니다.");
        }
        try {
            LocalDate.parse(birthDate);
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "올바른 날짜 형식이 아닙니다. (yyyy-MM-dd)");
        }
    }

    public User() {
    }

    public static User createUser(UserCommand.SignupCommand signupCommand) {
        return User.builder()
                .loginId(signupCommand.loginId())
                .email(signupCommand.email())
                .birthDate(signupCommand.birthDate())
                .gender(signupCommand.gender())
                .build();
    }
}
