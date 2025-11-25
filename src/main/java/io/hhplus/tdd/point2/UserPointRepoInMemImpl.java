package io.hhplus.tdd.point2;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserPointRepoInMemImpl implements UserPointRepository {
    private final UserPointTable userPointTable;

    @Override
    public UserPoint selectById(long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint insertOrUpdate(long id, long amount){
        return userPointTable.insertOrUpdate(id, amount);
    }

}
