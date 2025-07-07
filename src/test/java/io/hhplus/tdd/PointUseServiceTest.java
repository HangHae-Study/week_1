package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class PointUseServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    @Test
    void T1_유저의_포인트가_0일때_사용할수없다(){
        //given
        Long userId = 1L;
        long useAmount = 10L;
        UserPoint currUserPoint = UserPoint.empty(userId);

        given(userPointTable.selectById(userId)).willReturn(currUserPoint);

        // when&then
        assertThatThrownBy(() ->
                pointService.use(userId, useAmount)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 포인트가 부족합니다");
    }

    @Test
    void T2_포인트가_있고_충분하면_사용할수있다(){
        //given
        Long userId = 1L;
        long curAmount = 20L;
        long useAmount = 10L;

        // 의존 테이블 클래스에서 값을 가져올 때, 처음부터 유저를 생성하는 단계를 거치지 않도록, 미리 UserPoint를 stub으로 설정
        UserPoint currUserPoint = new UserPoint(userId, curAmount, System.currentTimeMillis());
        given(userPointTable.selectById(userId))
                .willReturn(currUserPoint);

        UserPoint updatedUserPoint = new UserPoint(userId, curAmount - useAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(eq(userId), eq(curAmount - useAmount)))
                .willReturn(updatedUserPoint);

        given(pointHistoryTable.insert(
                eq(userId),
                eq(useAmount),
                eq(TransactionType.USE),
                anyLong()
        )).willReturn(new PointHistory(
                1L,
                userId,
                useAmount,
                TransactionType.USE,
                System.currentTimeMillis()
        ));

        //when
        UserPoint result = pointService.use(userId, useAmount);

        //then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(curAmount - useAmount);

        // verify 추가
        verify(userPointTable).selectById(userId);
        verify(userPointTable).insertOrUpdate(eq(userId), eq(curAmount - useAmount));
        verify(pointHistoryTable).insert(
                eq(userId),
                eq(useAmount),
                eq(TransactionType.USE),
                anyLong()
        );
    }

    @Test
    void T3_포인트가_있고_충분하지않다면_사용할수없다(){
        Long userId = 1L;
        long useAmount = 20L;
        long curAmount = 10L;

        UserPoint currUserPoint = new UserPoint(userId, curAmount, System.currentTimeMillis());
        given(userPointTable.selectById(userId))
                .willReturn(currUserPoint);

        // when & then
        assertThatThrownBy(() ->
                pointService.use(userId, useAmount)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 포인트가 부족합니다");
    }
}
