### 하나의 시나리오로 테스트를 구상해보기?

- 단위 테스트는 하나의 비즈니스 로직 혹은 도메인에 대한 정상 동작을 검증하는 것.. 으로 받아들이고
- 통합 테스트는 해당 단위 테스트를 합쳐서 하나의 흐름이 정상적으로 동작하는지 확인하는 것으로 받아들임..

---

## 시나리오 작성해보기,,

### 1. 유저 충전에 대한 기본 시나리오 
- 유저가 조회한 다음
   - 충전 이후에는 기존 포인트에서 충전한 만큼의 금액이 올라가야한다.
   - 내가 전제로 깔았떤 비즈니스 로직은, 유저가 이미 존재하든/존재하지 않든, ID를 요청 시에 특정 ID 에 대한 포인트가 반드시 Response 된다는 것.
   - (Table의 getOrDefault라는 강결합이 전제가 되지만,,)


- 이렇게 되었을 때, 
  - 신규 유저의 포인트가 0 점이든
  - 기존 유저의 포인트가 0 점 이상이든, (충전했다가 다 사용함 ㅠ)
  - ID 에 대한 충전 행위는 반드시 이뤄지게 보장 한다는 것.
    - (충전의 제약조건에 대해서는 고려하지 않았을 때, -> 추가 요구사항으로 넘기기)
  - 외부에서는 유저의 존재여부에 대해서 알 필요 없이 `충전한다` 라는 하나의 행위 만을 보장할 수 있도록 테스트 코드 작성해보기

> 유저 조회 -> 충전 -> 유저 조회 시, 기존 포인트에서 충전한 만큼의 포인트가 올라가서 조회가 된다면, 충전 행위를 성공한 것으로 간주한다.
>

### 2. 유저 충전 이후, 충전된 기록을 조회하는 기본 시나리오
- `1. 유저 충전에 대한 기본 시나리오` 이 후, 
    - 기록에서 조회되는 포인트 충전내역 = 현재 유저의 포인트 라면
    - 충전이 정상적으로 완료되었고, 그에 대한 기록까지 정상 등록 된것.


- `유저 충전에 대한 기본 시나리오` 와 겹치는 부분이 많으므로, 이걸 합쳐야하나??
> 유저 조회 -> 충전 -> 유저 조회 -> 유저 기록 조회 -> 유저포인트=유저기록집계 라면, 충전 행위를 성공한 것으로 간주한다.

### 3. 유저 포인트 사용에 대한 실패 시나리오
- 유저가 포인트 사용을 하지 못하는 경우가 존재한다.
  - 현재 유저의 포인트가 0원일 때,
  - 현재 유저의 포인트가 0원 이상이고, 사용하려는 금액이 현재 유저의 포인트보다 클 때


- 요청하는 입장에서는, 현재 유저가 얼마나 금액을 갖고 있는지 (?) 유저 당사자가 아니면 모름(개발관점에서)


- 또는, 유저가 금액을 얼마나 갖고 있든 가지고 있지 않든, 현재 가진 포인트보다 큰 금액을 사용하려면 당연히 사용 불가하도록 실패 응답을 반환해 줄 것.

> 기존 유저가 이미 충전한 것은, 앞선 1, 2번 테스트 에서 완료 하였으니 service 객체에 직접 접근하여서, 값을 미리 초기화한 후 API로 요청한 결과값을 비교한다.
> 
> `미리 작성된 유저의 포인트 및 기록 생성 -> 사용 불가능한 포인트 요청 -> 미리 작성된 유저의 포인트 및 기록`이 똑같이 조회 되면, 정상적으로 실패한 것으로 간주한다.


### 4. 유저 포인트 사용에 대한 성공 시나리오
- 유저가 포인트  사용을 하는 것은?
  - 현재 유저의 포인트가 0원보다 많을 때,
  - 사용하련느 금액이 현재 유저의 포인트보다 같거나 작을 때


### 5. 유저 포인트 사용에 대한 성공/실패 시나리오 통합?
- 3, 4 번 테스트가 동일한 행위에 대해, 나오는 결과를 검증해야함으로
- `포인트 사용` 이라는 행위는 중복적이게 된다.
- `ParameterizedTest`를 통해서, 행위에 대한 공통을 제거하고, 다를 수 있는 내역에 대한 분기처리를 성공/처리로 테스트해보기

---

### 1. 기본 충전 시나리오에서의 고민
```
  private Long parsingPoint(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        long pointBefore = root.path("data").path("point").asLong();

        return pointBefore;
    }

    @Test
    void 유저가_조회하고_충전하고_조회한다() throws Exception{
        // 시나리오 1 : 유저 충전에 대한 기본 시나리오
        Long userId = 1L;

        // 최초 조회 (충전전 비교하기 위함)
        MvcResult getResult = mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andReturn();

        Long beforeP = parsingPoint(getResult);
```
해당 코드에서, 처음에는 userId를 통해 포인트 get요청을 보냇을 때, `.andExpect(jsonPath("$.data.point").value(0))` 로 검증하였다.

-> 잘못된 점, 현재 유저의 등록 상태를 모르고 포인트 상태도 모른다고 했을 때, 해당 코드는 실패함.

-> 그래서 이전 테스트에서 getPoint를 통해 유저가 반드시 조회가 된다는 것은 알고 있으니, `.andExpect(jsonPath("$.data").exists())` 로 변경

`JsonParsing을 통해 조회된 기본 값을 추후에 더할 값과 비교하여 최종적으로 테스트 비교를 하였음.`

### 2. 계속된 테스트 코드 작성의 반복
- 포인트 충전 테스트
  - 현재 금액 조회
  - 충전
  - 행위 후 금액 조회

- 포인트 사용 테스트
  - 현재 금액 조회
  - 사용
  - 행위 후 금액 조회

- 포인트 기록 테스트
  - 현재 금액 조회
  - 충전/사용
  - 행위 후 금액 조회
  - 행위 후 금액 기록 집계

> 조회 -> 행위 -> 조회 가 반복되어, 테스트 헬퍼 함수를 통해서 추출하고 수정해보기.

또한, 포인트에 대한 행위가 늘어날 수록, 테스트 코드 또한 결합/종속 적인 특징이 늘어날 것같아서.. 테스트 코드내에서 요청하는 방식을 분기할 수 있는 방법을 고려해보기
- Before
```
  private void chargeOrUse(Long userId, long amount, boolean charge) throws Exception {
        // 포인트에 대한 행위가 더 늘어난다면,,, 결합력 깨지는 코드가 될 수,,
        // 메서드가 행위에 종속되어 버린다..
        String path = "/point/{id}/" + (charge ? "charge" : "use");
        mockMvc.perform(patch(path, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "amount": %d
                        }
                    """.formatted(amount)))
                .andExpect(status().isOk());
    }
```
- After
```
    public enum PointAction {
        CHARGE("/point/{id}/charge"),
        USE("/point/{id}/use"),
        DELIVER("/point/{id}/deliver");
    
        private final String path;
    
        PointAction(String path) {
            this.path = path;
        }
    
        public String path() {
            return path;
        }
    }
    
    private void performPointAction(Long userId, long amount, PointAction action) throws Exception {
        mockMvc.perform(
                        patch(action.path(), userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "amount": %d
                        }
                    """.formatted(amount)))
                .andExpect(status().isOk());
    }
```

이를 통해 좀더 간단한 형태의 테스트 코드 작성 해보기
```
    @Test
    void 유저가_조회하고_충전하고_조회하고_기록검증한다() throws Exception {
        Long userId = 1L;

        // 최초 조회
        long before = getPoint(userId);
        assertThat(before).isEqualTo(0);

        // 여러 번 충전
        performPointAction(userId, 200L, PointAction.CHARGE);
        performPointAction(userId, 300L, PointAction.CHARGE);

        // 재조회
        long after = getPoint(userId);
        assertThat(after).isEqualTo(before + 500L);

        // 기록 검증
        long historySum = sumHistories(userId);
        assertThat(historySum).isEqualTo(after);
    }

```
