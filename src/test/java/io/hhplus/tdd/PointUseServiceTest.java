package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class PointUseServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    @Test
    void T1_유저의_포인트가_0일때_사용한다(){
        //given
        Long userId = 1L;
        long usePoint = 10L;
        UserPoint currUserPoint = UserPoint.empty(userId);

        given(userPointTable.selectById(userId)).willReturn(currUserPoint);

    }
}
