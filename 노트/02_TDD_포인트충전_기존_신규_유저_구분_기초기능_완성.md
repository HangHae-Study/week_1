### 1. 포인트 충전 신규 유저
```
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
```
> selectById 하기 전, 현재 시간을 기준으로
> - 현재 시간보다 UserPoint의 updateTimeMillies가 늦으면 (cur <= upd): 신규 유저
> - 현재 시간보다 UserPoint의 updateTimeMillies가 빠르면 (cur >  upd): 기존 유저

---
### 2. 포인트 충전 기존 유저

`UserPoint emptyUser = new UserPoint(userId, 0L, System.currentTimeMillis() - 10);`

> updateTimeMillis를 의도적으로 늦게 주어서, Test 통과를 시킴..
> - 근데, 이러면 애초에 UserPoint를 조회할 때부터 user가 map 필드에 존재 하는지 안하는지 확인을 하면 안될까??
> - getPoint가 존재하는데,,

---

### 3. PointService 리팩터링 해보기

- 아래 코드에서는 위 과정에서 수정한 서비스 코드인데,, 두 개 의 함수에서 selectById를 호출하고 있었고, 공통으로 신규/기존 등록 유저를 묶는게 테스트에서도 훨씬 나을 것 같음..


- 유저에 대한 getPoint 함수에서, 없다면 신규로 유저 테이블에 값을 넣어주고, 있다면 안 넣어주면 되니 다른 함수에서 계속해서 유저 구분을 할 필요가 없을테니,,
- Before
```
    public UserPoint getPoint(Long id) {
        return userPointTable.selectById(id);
    }

    public UserPoint charge(Long userId, long chargeAmount) {
        Long currentTimeMillis = System.currentTimeMillis();
        UserPoint curUserPoint = userPointTable.selectById(userId);

        // 포인트가 0L 일 경우, 그냥 0원인 유저일 수도 있고 신규 유저일 수도 있음.
        // updateMillies를 기반으로 진행을 한다면?
        long newPoint = curUserPoint.point() + chargeAmount;
        UserPoint updUserPoint;
        PointHistory newPointHistory;
        if(currentTimeMillis <= curUserPoint.updateMillis()){
            updUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
            newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());
        }else{
            updUserPoint = userPointTable.insertOrUpdate(userId, newPoint);
            newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());
        }

        return updUserPoint;
    }
```
- After
```
    public UserPoint getPoint(Long id) {
        Long currentTimeMills =  System.currentTimeMillis();
        UserPoint userPoint = userPointTable.selectById(id);

        if(currentTimeMills <= userPoint.updateMillis()){
            userPointTable.insertOrUpdate(userPoint.id(), userPoint.point());
        }else{

        }

        return userPoint;
    }

    public UserPoint charge(Long userId, long chargeAmount) {

        // 1- 포인트가 0L 일 경우, 그냥 0원인 유저일 수도 있고 신규 유저일 수도 있음.
        // 2- updateMillies를 기반으로 진행을 한다면?
        // 3- getPoint로 통합
        UserPoint curUserPoint = getPoint(userId);
        long newPoint = curUserPoint.point() + chargeAmount;

        PointHistory newPointHistory = pointHistoryTable.insert(userId, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(userId, newPoint);
    }
```

---
### 4. 테스트 코드 수정

- `after`
```
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
            userPoint = new UserPoint (userId, chargeAmount, now - 10);
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
```

- `before`
```
    @Test
    void T4_유저_포인트_조회시_기존_신규_유저_구분(){
        // given
        Long newUserId = 1L;
        Long oldUserId = 2L;
        long chargeAmount = 100L;
        Long currentTime = System.currentTimeMillis();
        UserPoint newUser = UserPoint.empty(newUserId);
        UserPoint oldUser = new UserPoint(oldUserId, 0L, System.currentTimeMillis() - 10);

        given(userPointTable.selectById(newUserId)).willReturn(newUser);
        given(userPointTable.selectById(oldUserId)).willReturn(oldUser);

        UserPoint newUserPoint = new UserPoint(newUserId, chargeAmount, System.currentTimeMillis());
        UserPoint oldUserPoint = new UserPoint(oldUserId, chargeAmount, System.currentTimeMillis());
        given(userPointTable.insertOrUpdate(newUserId, chargeAmount))
                .willReturn(newUserPoint);
        given(userPointTable.insertOrUpdate(oldUserId, chargeAmount))
                .willReturn(oldUserPoint);

        given(pointHistoryTable.insert(
                eq(newUserId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        )).willReturn(new PointHistory(
                1L,
                newUserId,
                chargeAmount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
        ));

        given(pointHistoryTable.insert(
                eq(oldUserId),
                eq(chargeAmount),
                eq(TransactionType.CHARGE),
                anyLong()
        )).willReturn(new PointHistory(
                1L,
                oldUserId,
                chargeAmount,
                TransactionType.CHARGE,
                System.currentTimeMillis()
        ));

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
```

---

### 5. 트러블 슈팅,,

```
Strict stubbing argument mismatch. Please check:
 - this invocation of 'insert' method:
    pointHistoryTable.insert(
    2L,
    200L,
    CHARGE,
    1751810041937L
);
    -> at io.hhplus.tdd.point.PointService.charge(PointService.java:35)
 - has following stubbing(s) with different arguments:|
```
> 어디서 에러 났나 했더니,,, 
> 
>`if(old){ userPoint = new UserPoint (userId, chargeAmount, now - 10); }`
> chargeAmount를 oldUser의 파라미터로 넣어버려서,,, 0L로 박음..


---

### 6. 조회/충전 일단 끝!!