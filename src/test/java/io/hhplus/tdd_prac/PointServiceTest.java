package io.hhplus.tdd_prac;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point2.PointService2;
import io.hhplus.tdd.point2.PointTableInterface;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class PointServiceTest {
    @InjectMocks
    PointService2 pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private PointTableInterface userPointTable;

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
}
