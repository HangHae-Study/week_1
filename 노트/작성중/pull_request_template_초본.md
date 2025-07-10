### **커밋 링크**

#### 단위 테스트

1) Mock 테스트 
   - 포인트 조회/충전 기능 초기 완성 : [[72eec80](https://github.com/HangHae-Study/week_1/tree/72eec80e2084b8c313cd6abe8c2784bb60355d3e)]
   - 유저 구분 별 포인트 조회/충전 테스트 : [[dc42650](https://github.com/HangHae-Study/week_1/tree/dc42650967ac6703bdd549505f4f5f52c35f752c)]
   - 포인트 사용 기능 완성 :[[ff7966a](https://github.com/HangHae-Study/week_1/tree/ff7966ab310876fa25df8e493d04a9c4e02b3e92)]


2. 고전파 테스트 
   - 실제 객체 사용 테스트 : [[5bbb55c](https://github.com/HangHae-Study/week_1/tree/5bbb55cbd2f057ece037cec692738f242e2b7e4a)]
   - 포인트 기록(history) 테스트 : [[15f2e98](https://github.com/HangHae-Study/week_1/tree/15f2e98c2ad62ecdd4974338ea7c74b7653d999d)]

#### 통합 테스트

1. 조회 컨트롤러 테스트
   - 초본 : [[40b2f5e](https://github.com/HangHae-Study/week_1/tree/40b2f5e920c5f63331bd9134d697ea6af913edcf)]
   - 완성 : [[9946b0c](https://github.com/HangHae-Study/week_1/tree/9946b0ce7dcb024a033fd5f358c9a5970675e2d1)]


2. 사용/충전 컨트롤러 테스트
   - [[93d5e7b](https://github.com/HangHae-Study/week_1/tree/93d5e7b021f820f9071088c490f77c3154c76100)]
---

### **리뷰 포인트(질문)**

- 리뷰 포인트 1
    - Mock 객체를 사용하여, 조회 `selectById` 를 기준으로 (내부에서는 map.getOrDefault 를 하는 행위) 에서 뱉어는 Stub의 형태를 어떻게 설정하여야 했을지 테스트 코드를 봐주셨으면 합니다.
    - ```
    //given
    Long id = 1L;
    UserPoint mockUser = new UserPoint(id, 0L, 0L);
    given(userPointTable.selectById(id)).willReturn(mockUser);
    
    Long userId = 1L;
    UserPoint emptyUser = UserPoint.empty(userId);
    given(userPointTable.selectById(userId)).willReturn(emptyUser);
    
    - UserPoint에 대한 도메인 요구사항이 변경된다면, 위 두 Stub은 테스트가 깨질 수도 있는 가능성이 있다고 생각합니다.
        - 파라미터가 추가되거나, empty를 뱉지 않게 된다면???
    - (근데 이 방법 말고는 도저히 생각이 안나요..)
    ```
    - 왜냐하면 테이블 클래스가 수정되지 않는다는 전재가 있고 블랙박스 테스트를 한다면, 어느 범위까지 블랙박스의 범주에 포함 되어야할지 궁금합니다.
      <br/><br/>
    - pointService에 대해서만 내부 동작을 모른채(?)로 코드를 작성할지, table 클래스 내부 동작까지 모른채(?)로 진행할 지의 차이는 코드를 작성할 때 꽤 크게 범위가 달라진다고 느꼈기 때문입니다.
      <br/><br/>
    - 저는, 포인트 테이블에 대한 동작까지 이미 숙지한 채로 테스트 코드를 작성했습니다. -> Point 내부에 대한동작이 곧 서비스에서 포인트 테이블을 호출하는 행위 대부분이 귀결되는 행위일텐데,,,
        - 이 범위에서 블랙 박스라 함은 발생할 수 있는 "예외" 혹은 비즈니스/도메인에 대한 제약 사항을 미리 설정하는 부분으로 바라봐도 될까요?
        - ex)
            - 음수 단위의 포인트를 충전할 수 없다
            - 현재 포인트보다 더 많은 포인트를 사용할 수 없다
            - 포인트 기록의 응답 데이터 단위는 배열이다 (배열 안의 값은 모름)


- 리뷰 포인트2
```
    @Test
    void T1_유저의_포인트_조회를_요청한다() throws Exception{

        Long userId = 1L;
        pointService.charge(userId, 400L);

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.point").value(400));
    }
```

- 해당 테스트에서는 응답 데이터에 대한 필드를 미리 숙지하고 있습니다.
- 블랙 박스 테스트는 내부 동작에 대한 숙지는 정확히 하지 않지만, 응답에 대해서는 요청하는 입장에서 이렇게 필드를 적나라하게 알아도 될까요?


- 만일에 내부 필드가 바뀐다면,, 그건 명세 자체가 바뀌는 것이기에 테스트 코드도 적절히 바뀌는 것이 당연한건지 궁금합니다. (저는 맞다고 생각해요.)
```
@Test
void T3_등록되지_않은_유저의_포인트_기록을_조회한다() throws Exception{
Long userId = 1L;

        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.length()").value(0));

        // 길이가 0 이다는 뭔가 내부에 결과가 나올 수 있다는 것을 암시하는 것 같음..
        // 0으로 만들지 말고,, data가 배열로 떨어진다는 사실만 안 채로, 집계한 값이 0인지를 판별한다면?
        // 근데 집계한 값이 0일 경우 포인트를 다 쓴 경우도 있음..
        // 그럼 배열이 떨어진 다는 사실을 알았을 때, 길이를 0으로 판별하는 것이 어찌하였건 테스트를 유효하게 만들 수 있는 조건 같음..

        // 배열 안에 뭐가 들어올진 모르고, 배열이라서 길이를 가진다는 것만 아는 것은 ? 화이트 or 블랙인가?
        // 배열 안에 데이터가 바뀌더라도, 배열이란 성질은 histories라는 공통의 기록 목록을 도메인으로 가져간다면, 길
        // 이를 바라보는 것은 비즈니스에 영향을 받지 않고, 도메인에 종속적이므로 블랙박스로 봐도 될듯한데??
    }
```
해당 코드 안의 주석 내용이 기록이 배열로 들어온다는 것만 인지하고, 내부객체가 어떤 형태를 띌지는 모른다는 전제를 잡았는데,,, 이또한 어디까지 블랙박스의 범위로 잡아야할지.. 알려주시면 감사하겠습니다.

- ControllerChargeUsePointTest 같은 경우, 코드를 모두 완성하고 통합 테스트를 작성했다고 생각하는데, TDD 관점에서 필요한 테스트 클래스인지 의문이 듭니다..



---
### **이번주 KPT 회고**

### Keep
<!-- 유지해야 할 좋은 점 -->

### Problem
- 과제 제출을 하기 위해 짰던 코드들을 다시 확인해보고, 정리했던 글을 다시 읽어보니,,
- `TDD를 연습해야한다, 테스트 코드를 짜자!` 라는 생각으로 급한 마음에 조금 더 코드를 들여다 보지 않고, 무작정 달려들었던 것 같습니다.


- 프론트에서 당연히 유저의 현재 포인트가 조회될 것이고, 그에 따른 후속 동작으로 충전/조회를 고민하였습니다.
- 그렇다보니 포인트가 0 점 일 때, Table 클래스에 존재하는 Map 필드의 getOrDefault 라는 메서드에 꽂혀, 
  - 포인트가 0일 경우, `기존에 존재하지 않던 UserPoint`인지, `포인트를 다 사용한 UserPoint`인지를 꼭 판별 해야겠다는 생각으로 코드를 작성하게 되었던 것 같습니다.


- 이 사실을 안 채로 테스트 코드를 작성하다보니, 과제에서 요구하는 블랙박스 테스트 보다는 화이트박스 테스트의 방향으로 작성할 수 밖에 없었던 것 같습니다..
- 이후, 멘토링을 통해서 코치님이 알려주신 여러 접근 방식과 예시를 보고, `selectById`가 뭘 하든, 테이블 함수를 호출하는 `poitnService`의 동작에 상관없이 테스트 코드가 통과될 수 있도록 작성하는 것이 좀 더 중요하겠다는 생각이 들었습니다.


- 단위 테스트 때에는 @Mock 객체와 Stub을 통해서 내부 동작을 이미 아는채로, 결합력이 높은 테스트를 작성하며 단위 테스트와 기능을 만드는데 초점을 두었지만..


- 조회/사용/충전의 모든 행위가 연결되어있는 `/point/{id}/histories` 기능을 구현할때는 Classic(고전파) 테스트를 지향하는 방향으로, 실제 객체를 사용하되 내부 객체의 비즈니스/도메인 자체의 기능이 바뀌지 않는다는 가정하에 통합테스트를 작성하려 노력 했습니다.

  (틀린 방법일 수 있지만...)

### Try
<!-- 새롭게 시도할 점 -->