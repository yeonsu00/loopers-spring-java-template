package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointInfo;
import jakarta.validation.constraints.NotNull;

public class PointV1Dto {

    public record PointRequest(
            @NotNull(message = "충전 포인트는 필수입니다.")
            Integer chargePoint
    ) {
        public static PointCommand.ChargeCommand toCommand(String loginId, PointV1Dto.PointRequest request) {
            return new PointCommand.ChargeCommand(loginId, request.chargePoint());
        }
    }

    public record PointResponse(Integer totalPoint) {
        public static PointV1Dto.PointResponse from(PointInfo info) {
            return new PointV1Dto.PointResponse(
                    info.totalPoint()
            );
        }
    }

}
