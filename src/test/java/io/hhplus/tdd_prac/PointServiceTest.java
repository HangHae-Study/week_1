package io.hhplus.tdd_prac;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point2.PointService2;
import io.hhplus.tdd.point2.UserPointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class PointServiceTest {
    @InjectMocks
    PointService2 pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointRepository userPointTable;

    @Test
    public void TEST_01_유저를_조회한다(){
        // given
        Long id = 1L;
        given(userPointTable.selectById(id)).willReturn(UserPoint.empty(id));

        UserPoint result = pointService.getUserPoint(id);

        // then
        assertThat(result.id()).isEqualTo(UserPoint.empty(id).id());
        assertThat(result.point()).isZero();
    }

    @Test
    public void TEST_02_유저_포인트를_충전한다(){
        // given
        long id = 1L;
        long amount = 100L;
        UserPoint before = new UserPoint(id, 50L, 0L);
        UserPoint after  = new UserPoint(id, 150L, System.currentTimeMillis());

        given(userPointTable.selectById(id)).willReturn(before);
        given(userPointTable.insertOrUpdate(id, 150L)).willReturn(after);

        // when
        UserPoint result = pointService.chargeUserPoint(id, amount);

        // then
        assertThat(result.point()).isEqualTo(150L);
        assertThat(result.id()).isEqualTo(id);

        // 상호작용 검증
        verify(userPointTable).selectById(id);
        verify(userPointTable).insertOrUpdate(id, 150L);
    }



}
