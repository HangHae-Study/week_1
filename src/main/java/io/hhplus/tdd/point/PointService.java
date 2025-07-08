package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        return updatedUserPoint;
    }

    public UserPoint use(Long userId, long useAmount) {
        UserPoint curUserPoint = getPoint(userId);
        long newPoint = curUserPoint.point() - useAmount;

        if(curUserPoint.point() == 0 || newPoint < 0){
            throw new IllegalStateException("잔여 포인트가 부족합니다");
        }

        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.USE, System.currentTimeMillis());

        return  updatedUserPoint;
    }

    public List<PointHistory> getHistories(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
        /*
        return List.of(
                new PointHistory[]{
                        new PointHistory(1L, 1L, 500L, TransactionType.CHARGE, System.currentTimeMillis()),
                        new PointHistory(2L, 1L, 200L, TransactionType.USE, System.currentTimeMillis()),
        });
        */

    }
}
