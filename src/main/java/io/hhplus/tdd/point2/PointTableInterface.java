package io.hhplus.tdd.point2;

import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Repository;

@Repository
public interface PointTableInterface {
    public UserPoint selectById(long id);
}
