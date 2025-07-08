package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PointClassicTest {

    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;
    private PointService pointService;

    @BeforeEach
    void setUp(){
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

    @Test
    void T1_WHITE_포인트_충전내역_기본조회(){
        // given
        Long id = 1L;
        Long currentTimeMillis =  System.currentTimeMillis();
        UserPoint newUser = pointService.getPoint(id);

        assertThat(newUser).isNotNull();
        assertThat(newUser.point()).isEqualTo(0L);
        assertThat(newUser.updateMillis()).isGreaterThanOrEqualTo(currentTimeMillis);

        currentTimeMillis = System.currentTimeMillis();
        UserPoint oldUser = pointService.getPoint(id);

        assertThat(oldUser).isNotNull();
        assertThat(oldUser.updateMillis()).isLessThan(currentTimeMillis);
    }

    @Test
    void T1_BLACK_포인트_충전내역_기본조회(){
        Long id = 1L;
        long chargeAmount = 10L;

        // 신규 유저 조회
        UserPoint newUser = userPointTable.selectById(id);
        assertThat(newUser.point()).isEqualTo(0L);

        // 신규 유저 충전 -> 기존 유저로 전환
        UserPoint chargeUser = pointService.charge(id, chargeAmount);

        // 신규, 기존 유저 조회 기준이 updateTimeMillis 인지 모르고 진행.
        // 포인트 충전도 동시에 테스트로 검증
        UserPoint userPoint = pointService.getPoint(id);
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(id);
        assertThat(userPoint.point()).isEqualTo(chargeAmount);
    }


    @Test
    void T2_포인트가_충분하면_사용한다(){
        Long userId = 1L;
        long curAmount = 20L;
        long useAmount = 10L;

        // 사전 준비,,
        userPointTable.insertOrUpdate(1L, curAmount);
        pointHistoryTable.insert(userId, curAmount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        UserPoint userPoint = pointService.use(userId, useAmount);
        assertThat(userPoint).isNotNull();
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(curAmount - useAmount);

        UserPoint stored = userPointTable.selectById(userId);
        assertThat(stored.point()).isEqualTo(curAmount - useAmount);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).asList().hasSize(2); // 첫 충전 내역 + 사용 내역
        assertThat(histories.get(1).amount()).isEqualTo(useAmount);
        assertThat(histories.get(1).type()).isEqualTo(TransactionType.USE);
    }

    @Test
    void T3_포인트가_부족하다면_사용할_수_없다() {
        Long userId = 1L;
        long curAmount = 10L;
        long useAmount = 20L;

        // 사전 준비,,
        userPointTable.insertOrUpdate(1L, curAmount);
        pointHistoryTable.insert(userId, curAmount, TransactionType.CHARGE, System.currentTimeMillis());

        // when
        assertThatThrownBy(() -> pointService.use(userId, useAmount)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔여 포인트가 부족합니다");

        UserPoint stored = userPointTable.selectById(userId);
        assertThat(stored.point()).isEqualTo(curAmount);

        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);
        assertThat(histories).asList().hasSize(1); // 첫 충전 내역 + 사용 내역
        assertThat(histories.get(0).amount()).isEqualTo(curAmount);
        assertThat(histories.get(0).type()).isEqualTo(TransactionType.CHARGE);
    }


    @Test
    void T4_포인트_충전_사용_후_포인트_내역을_조회한다(){
        Long userId = 1L;
        long chargeAmount = 500L;
        long useAmount = 200L;

        UserPoint chargeUser = pointService.charge(userId, chargeAmount);

        Assertions.assertThat(chargeUser).isNotNull();
        Assertions.assertThat(chargeUser.point()).isEqualTo(chargeAmount);

        UserPoint useUser = pointService.use(userId, useAmount);

        Assertions.assertThat(useUser).isNotNull();
        Assertions.assertThat(useUser.point()).isEqualTo(chargeAmount - useAmount);

        List<PointHistory> histories = pointService.getHistories(userId);

        long sum = histories.stream()
                .mapToLong(h-> {
                    if (h.type() == TransactionType.CHARGE) {
                        return h.amount();
                    } else {
                        return -h.amount();
                    }
                }).sum();

        Assertions.assertThat(sum).isEqualTo(chargeAmount - useAmount);
    }

}
