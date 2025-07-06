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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserPointTable userPointTable;

    @Test
    void 포인트_충전내역_기본조회(){
        // given
        Long id = 1L;
        UserPoint mockUser = new UserPoint(id, 0L, 0L);
        given(userPointTable.selectById(id)).willReturn(mockUser);

        UserPoint result = pointService.getPoint(id);
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(id);
        assertThat(result.point()).isEqualTo(mockUser.point());
    }

    @Test
    /*
    - 한 번도 충전한적 없는(존재하지 않는 유저?)일 때, 충전 한다면?
    - UserPointTable에 데이터가 없으면 새로운 UserPoint를 가져온다.
    - 새로 만든 UserPoint의 포인트가 충전 금액과 같아야 한다.
     */
    void 유저가_존재하지_않으면_새로운_UserPoint를_생성하고_포인트를_충전한다(){
        // given
        Long userId = 1L;
        long chargeAmount = 100L;
        UserPoint emptyUser = UserPoint.empty(userId);

        given(userPointTable.selectById(userId)).willReturn(emptyUser);

        // 여기서 새로 만든 UserPoint가 UserPointTable에서 getOrDefault를 수행한 다는 것을 안다는 가정하에 테스트 코드를 진행한다면,,, 이건 결합도가 발생하게 테스트 코드를 짜는건가???
        // 그래서 일단,, 새로운 UserPoint를 뱉어준 다는 생각으로 Service의 selectById에 의존적이지 않게 코드를 짠다면?
        // 질문 해보기..
        // Mock Respository 에서 뱉어줄 객체 값들 설정
        UserPoint newUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(userId, chargeAmount))
                .willReturn(newUserPoint);

        given(pointHistoryTable.insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        )).willReturn(new PointHistory(
                1L,
                userId,
                chargeAmount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
        ));

        // when
        UserPoint result = pointService.charge(userId, chargeAmount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(chargeAmount);

    }
}
