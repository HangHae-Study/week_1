### Mock으로 먼저 구현해보기?

```
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

}
```

> Service 클래스 내부에 수행 로직을 추가하면서, 진행하기 위해 간단하게 Red 코드 작성해보기...


---

### 1. 조회 기능 (TDD 작성 시작?)

강결합을 지향하는 테스트 코드를 작성하라고 하셨지만, 기존에 테이블 클래스가 어떤 형태를 가지고 있는지 먼저 확인한 후에 서비스 클래스와 테스트 코드를 작성하였음.

1. selectById에 대한 코드는 우선 구현되어 있는 상태.
2. 서비스 클래스 생성과 동시에 내부 테이블 코드를 알고 있는 상태로 시작하기에, 이를 결합이 생기는 테스트 코드라 할 수 있을지 잘 모르겠음..
3. 기능 완성을 목표로 리팩터링을 동시에 하는 것으로 간주해야하나?
4. (과제 요구사항에는 Table 클래스를 고치지 않고 진행하라고 하였으니,, 결합도가 깨지는 현상은 발생하지 않는다고 생각하기?)

#### RED
 `error: cannot find symbol private PointService pointService;`

```
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.UserPoint;
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
}
```

#### GREEN
```
@Service
@RequiredArgsConstructor
public class PointService {
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable  userPointTable;

    public UserPoint getPoint(Long id) {
        return userPointTable.selectById(id);
    }
}
```

---

### 추가 요구사항 확인 해보기

1. 여기서 seelctById를 할 때, 만일에 존재하지 않는 userId가 있다면 포인트가 0L인 UserPoint 클래스를 뱉어준다는 것.
2. 그렇다면, 포인트 충전/사용 시에 현재 유저가 테이블에 존재하는지 안 하는지를 미리 확인한 후에 다음 로직을 추가로 작성하는 것이 좋을 것 같다.


#### 다음 로직 작성 준비?
- 포인트 충전하는 테스트 코드작성
- 충전하기 전, 충전을 할 유저(ID)가 존재하는지 확인해야함.
  - 유저가 존재한다면, 현재 유저의 Point 를 최신화하는 동작 수행
  - 유저가 존재하지 않는다면, empty 유저를 뱉고, 새로 유저를 등록해야 한다는 것.
- 그 다음, PointHistoryTable에 해당 유저의 충전 기록을 추가한다.
- 그 과정에서 UserPointTable과 PointHistoryTable 간의 Atomic한 작업을 보장할 필요는 있는데,, 이건 나중에

---

### 2. 충전 기능 테스트 코드 작성

#### RED
- 한 번도 충전한적 없는(존재하지 않는 유저?)일 때, 충전 한다면?
- UserPointTable에 데이터가 없으면 새로운 UserPoint를 가져온다.
- 새로 만든 UserPoint의 포인트가 충전 금액과 같아야 한다.
`error: cannot find symbol UserPoint result = pointService.charge(userId, chargeAmount);`
```
@Test
    void 유저가_존재하지_않으면_새로운_UserPoint를_생성하고_포인트를_충전한다(){
        // given
        Long userId = 1L;
        long chargeAmount = 100L;
        UserPoint emptyUser = UserPoint.empty(userId);

        given(userPointTable.selectById(userId)).willReturn(emptyUser);

        /* 질문할 부분*/
        // 여기서 새로 만든 UserPoint가 UserPointTable에서 getOrDefault를 수행한 다는 것을 안다는 가정하에 테스트 코드를 진행한다면,,,
        // 이건 결합도가 발생하게 테스트 코드를 짜는건가???
        // 그래서 일단, 새로운 UserPoint객체를 생성하고, 뱉어주는 것을 전재로 Service의 selectById에 의존적이지 않게 코드를 짠다면?
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

```
#### 생각 정리하기

- UserPointTable 안의 selectById에서 맵 필드에 GetOrDefault가 당연히, 유저가 존재하지 않을 때 포인트가 0 인 UserPoint를 아는 상태로 테스트 코드를 처음에 작성하려했음..


- 하지만, 해당 방식은 테이블 클래스의 내부 코드를 아는 상태로 테스트 코드를 작성하는게 되는데,, 이건 결합력을 높이게 되는 방향인가?? 판별하기가 좀 어려움,,


- (왜냐하면, 과제 요구사항에서는 Table 클래스의 내부 코드를 수정하지 않는 것을 지시하여서..)

> - 새로운 유저를 생성해준다는 것이 이미 Table 의 전제 로직에서 제공을 해줌으로써 "필요한 결합" 인가?
> - 테스트 코드에서 selectById에서 수정이 일어난다면? 후에 테스트 코드에서도 영향이 끼칠 수 있기 때문에,, 동작이 보장되도록 코드를 구현하는 것이 맞다고 생각함.


`given(userPointTable.selectById(id)).willReturn(null);`

만일에 Table 코드가 없었다면 조회 시 Null을 기반으로 처리 할지말지를 고민하고 Service 단에서 로직 수행 판별여부를 진행하도록 보장할 수 있었을 것 같다.


#### GREEN
```
@Service
@RequiredArgsConstructor
public class PointService {
    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable  userPointTable;

    public UserPoint charge(Long userId, long chargeAmount) {
        UserPoint curUserPoint = userPointTable.selectById(userId);

        // 포인트가 0L 일 경우, 그냥 0원인 유저일 수도 있고 신규 유저일 수도 있음.
        long newPoint = curUserPoint.point() + chargeAmount;
        UserPoint updUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        return updUserPoint;
    }
}    
```

### 추가 요구사항 확인해보기
1. 기존 코드에서는 신규 유저인지, 포인트가 다 소진된(충전하지 않은) 유저인지 판별할 수가 없다..
2. 나중에 이 둘을 기반으로, 생성과 동시에 충전이 발생한다면??
3. throttle 코드에 따른 유저 생성 이전에 충전이 되어버리는 원자성이 깨지는 코드가 발생할 수도 있을 것 같다..
   - 이를 해결하려면,,, Record 클래스를 수정하는 (신규/기존 유저 판별 필드)
   - 추가로 고려할 부분이 많아보이지만, TDD를 작성하면서 추가로 좀 더 고민 해봐야 할 것 같음.

#### 다음 로직 작성 준비?
- 포인트 사용 기능 추가하기
- 포인트 사용 기능 추가 전, 유저 관리를 어떻게할 지도 ?
  - 먼저 구현 후에 해보는 것이 추후에 다시 돌아와서 코드를 수정하는 일이 적을 것같다.