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
public class PointSelectChargeServiceTest {

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
    void T1_UserPoint를_생성하고_포인트를_충전한다(){
        // 1*** 유저가 존재하지 않으면을 가정했을 때 !!!!!!
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

    @Test
    void T2_updateMillies를_통해_신규_유저를_구분하여_포인트를_충전한다(){
        // T1 에서 해결 못하였던 신규/기존 유저를 구분하기 위해 record에 존재하는 updateMillies를 사용해본다면?
        // given
        Long userId = 1L;
        long chargeAmount = 100L;
        Long currentTime = System.currentTimeMillis();

        // 유저 조회
        UserPoint emptyUser = UserPoint.empty(userId);
        given(userPointTable.selectById(userId)).willReturn(emptyUser);

        // 유저 포인트 충전
        UserPoint newUserPoint = new UserPoint(userId,  chargeAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(userId, chargeAmount))
                .willReturn(newUserPoint);

        // 포인트 충전 내역 업데이트
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
        UserPoint chargeUser = pointService.charge(userId, chargeAmount);

        //then
        assertThat(chargeUser).isNotNull();
        assertThat(chargeUser.id()).isEqualTo(userId);
        assertThat(chargeUser.point()).isEqualTo(chargeAmount);

        // 신규 유저
        assertThat(emptyUser.updateMillis()).isGreaterThanOrEqualTo(currentTime);
    }

    @Test
    void T3_updateMillies를_통해_기존_유저를_구분하여_포인트를_충전한다(){
        //given
        Long userId = 1L;
        long chargeAmount = 100L;
        UserPoint emptyUser = new UserPoint(userId, 0L, System.currentTimeMillis() - 10);
        Long currentTime = System.currentTimeMillis();

        given(userPointTable.selectById(userId)).willReturn(emptyUser);

        UserPoint newUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(userId, chargeAmount))
                .willReturn(newUserPoint);

        // 포인트 충전 내역 업데이트
        given(pointHistoryTable.insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        )).willReturn(new PointHistory(
                1L, // 포인트 충전/사용 내역 식별자
                userId,
                chargeAmount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
        ));

        // when
        UserPoint chargeUser = pointService.charge(userId, chargeAmount);

        // then
        assertThat(chargeUser).isNotNull();
        assertThat(chargeUser.id()).isEqualTo(userId);
        assertThat(chargeUser.point()).isEqualTo(chargeAmount);

        // 기존 유저
        assertThat(emptyUser.updateMillis()).isLessThan(currentTime);
    }

    /*
    T4 테스트는 Service 먼저 구현한 후에, 테스트 코드를 작성하였음.
     */
    @Test
    void T4_유저_포인트_조회시_기존_신규_유저_구분(){
        // given
        Long newUserId = 1L;
        Long oldUserId = 2L;
        long chargeAmount = 100L;
        Long currentTime = System.currentTimeMillis();
        UserPoint newUser = UserPoint.empty(newUserId);
        UserPoint oldUser = new UserPoint(oldUserId, 0L, System.currentTimeMillis() - 10);

        mockUserPoint(newUserId, chargeAmount, false, 1L);
        mockUserPoint(oldUserId, chargeAmount, true, 2L);

        // when
        UserPoint newResult = pointService.charge(newUserId, chargeAmount);
        UserPoint oldResult = pointService.charge(oldUserId, chargeAmount);

        // then
        assertThat(newResult).isNotNull();
        assertThat(newResult.id()).isEqualTo(newUserId);
        assertThat(newResult.point()).isEqualTo(chargeAmount);
        assertThat(newUser.updateMillis()).isGreaterThanOrEqualTo(currentTime);

        assertThat(oldResult).isNotNull();
        assertThat(oldResult.id()).isEqualTo(oldUserId);
        assertThat(oldResult.point()).isEqualTo(chargeAmount);
        assertThat(oldUser.updateMillis()).isLessThan(currentTime);
    }

    private void mockUserPoint(Long userId, long chargeAmount, boolean old, long cursor){
        Long now = System.currentTimeMillis();

        UserPoint userPoint;
        if(old){
            userPoint = new UserPoint (userId, 0L, now - 10);
        }else{
            userPoint = UserPoint.empty(userId);
        }
        given(userPointTable.selectById(userId)).willReturn(userPoint);

        UserPoint newUserPoint = new UserPoint(userId, chargeAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(userId, chargeAmount)).willReturn(newUserPoint);

        given(pointHistoryTable.insert(
                eq(userId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        )).willReturn(new PointHistory(
                cursor,
                userId,
                chargeAmount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
        ));
    }
}
