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
        Long currentTimeMills =  System.currentTimeMillis();
        UserPoint userPoint = userPointTable.selectById(id);

        if(currentTimeMills <= userPoint.updateMillis()){
            userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        }else{

        }

        return userPoint;
    }

    public UserPoint charge(Long userId, long chargeAmount) {

        // 1- 포인트가 0L 일 경우, 그냥 0원인 유저일 수도 있고 신규 유저일 수도 있음.
        // 2- updateMillies를 기반으로 진행을 한다면?
        // 3- getPoint로 통합
        UserPoint curUserPoint = getPoint(userId);
        long newPoint = curUserPoint.point() + chargeAmount;

        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, newPoint);
    }
}
