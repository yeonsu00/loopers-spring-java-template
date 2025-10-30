package com.loopers.domain.point;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PointService {

    private final PointRepository pointRepository;

    @Transactional
    public Point chargePoint(Long id, Integer chargePoint) {
        Optional<Point> point = pointRepository.findByUserId(id);

        if (point.isPresent()) {
            Point existingPoint = point.get();
            existingPoint.charge(chargePoint);
            return existingPoint;
        } else {
            Point newPoint = Point.createPoint(id);
            newPoint.charge(chargePoint);
            return pointRepository.save(newPoint);
        }
    }
}
