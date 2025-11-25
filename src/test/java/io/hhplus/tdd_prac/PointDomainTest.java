package io.hhplus.tdd_prac;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class PointDomainTest {

    @Test
    public void TEST01_유저_포인트를_정상_충전한다(){
        UserPoint before = UserPoint.empty(1L);

        long amt = 100L;
        long afterPoint = before.charge(amt);

        assertThat(before.point() + amt).isEqualTo(afterPoint);
    }

    @Test
    public void TEST02_유저_포인트를_음수_충전하여_실패한다(){
        UserPoint before = UserPoint.empty(1L);

        long amt = -100L;

        assertThatThrownBy(() -> before.charge(amt))
                .hasMessageContaining("Charge Should Be Positive");
    }

    @Test
    public void TEST03_유저_포인트를_한계치_이상_충전한다(){
        UserPoint before = UserPoint.empty(1L);

        long amt = 1111111L;
        assertThatThrownBy(() -> before.charge(amt))
                .hasMessageContaining("Charge Should Be Less Then a Million");
    }
}
