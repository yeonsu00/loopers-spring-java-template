package com.loopers.interfaces.api.user;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User V1 API", description = "User V1 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
            summary = "회원가입",
            description = "ID, 이메일, 생년월일, 성별로 회원가입을 합니다."
    )
    ApiResponse<UserV1Dto.UserResponse> signup(
            @Schema(name = "회원가입 정보", description = "회원가입할 사용자 정보")
            UserV1Dto.SignupRequest signupRequest
    );

}
