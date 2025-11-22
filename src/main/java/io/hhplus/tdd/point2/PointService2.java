package io.hhplus.tdd.point2;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService2 {
    private final PointTableInterface userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public UserPoint getUserPoint(Long id){
        return userPointTable.selectById(id);
    }
}
