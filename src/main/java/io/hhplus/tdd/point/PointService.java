package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable  userPointTable;

    public UserPoint getPoint(Long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint charge(Long userId, long chargeAmount) {
        UserPoint curUserPoint = userPointTable.selectById(userId);

        // 포인트가 0L 일 경우, 그냥 0원인 유저일 수도 있고 신규 유저일 수도 있음.
        long newPoint = curUserPoint.point() + chargeAmount;
        UserPoint updUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        return updUserPoint;
    }
}
