package io.hhplus.tdd.point2;

import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPointRepository {
    public UserPoint selectById(long id);

    public UserPoint insertOrUpdate(long id, long amount);
}
