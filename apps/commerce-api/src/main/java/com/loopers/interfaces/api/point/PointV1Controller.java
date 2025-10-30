package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/point")
public class PointV1Controller implements PointV1ApiSpec {

    private final PointFacade pointFacade;

    @PostMapping
    @Override
    public ApiResponse<PointV1Dto.PointResponse> chargePoint(
            @RequestHeader("X-USER-ID") String loginId,
            @Valid @RequestBody PointV1Dto.PointRequest pointRequest
    ) {
        PointCommand.ChargeCommand chargeCommand = PointV1Dto.PointRequest.toCommand(loginId, pointRequest);

        PointInfo pointInfo = pointFacade.chargePoint(chargeCommand);
        PointV1Dto.PointResponse response = PointV1Dto.PointResponse.from(pointInfo);

        return ApiResponse.success(response);
    }

}
