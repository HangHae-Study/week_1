package io.hhplus.tdd.point2;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PointTableInMemImpl implements PointTableInterface {
    private final UserPointTable userPointTable;

    @Override
    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }
}
