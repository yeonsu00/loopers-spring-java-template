package com.loopers.application.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PointFacade {

    private final PointService pointService;
    private final UserService userService;

    public PointInfo chargePoint(PointCommand.ChargeCommand chargeCommand) {
        User user = userService.getUserByLoginId(chargeCommand.loginId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, chargeCommand.loginId() + " 사용자를 찾을 수 없습니다."));

        Point point = pointService.chargePoint(user.getId(), chargeCommand.chargePoint());
        return PointInfo.from(point);
    }
}
