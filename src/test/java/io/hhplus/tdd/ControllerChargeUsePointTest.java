package io.hhplus.tdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hhplus.PointAction;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
public class ControllerChargeUsePointTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    PointService pointService;

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

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

        // 충전
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "amount": 500
                        }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.point").value(500));

        // 재조회
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.point").value(beforeP + 500));
    }


    // --- 반복 코드 추출 이후 작성

    private long getPoint(Long userId) throws Exception {
        MvcResult result = mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);
        return root.path("data").path("point").asLong();
    }

    private void performPointAction(
            Long userId,
            long amount,
            PointAction action,
            String successFail,
            ResultMatcher expectedStatus
    ) throws Exception {
        mockMvc.perform(
                        patch(action.path(), userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "amount": %d
                        }
                    """.formatted(amount)))
                .andExpect(jsonPath("$.code").value(successFail))
                .andExpect(expectedStatus);
    }

    private long sumHistories(Long userId) throws Exception {
        MvcResult result = mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(content);

        JsonNode dataNode = root.path("data");
        long sum = 0;
        for (JsonNode node : dataNode) {
            long amount = node.path("amount").asLong();
            String type = node.path("type").asText();

            if (type.equals("CHARGE")) {
                sum += amount;
            } else if (type.equals("USE")) {
                sum -= amount;
            }
        }
        return sum;
    }

    @Test
    void 유저가_조회하고_충전하고_조회하고_기록검증한다() throws Exception {
        // 시나리오 2 : 유저 충전 이후, 충전된 기록을 조회하는 기본 시나리오
        Long userId = 1L;

        // 최초 조회
        long before = getPoint(userId);
        assertThat(before).isEqualTo(0);

        // 여러 번 충전
        performPointAction(userId, 200L, PointAction.CHARGE, "SUCCESS", status().isOk());
        performPointAction(userId, 300L, PointAction.CHARGE, "SUCCESS", status().isOk());

        // 재조회
        long after = getPoint(userId);
        assertThat(after).isEqualTo(before + 500L);

        // 기록 검증
        long historySum = sumHistories(userId);
        assertThat(historySum).isEqualTo(after);
    }

    @Test
    void 유저_포인트_보다_많은_사용량_요청() throws Exception{
        // 시나리오 2 : 유저 충전 이후, 충전된 기록을 조회하는 기본 시나리오

        Long userId = 1L;
        Long advancePoint = 500L;
        // 미리 충전
        performPointAction(userId, advancePoint, PointAction.CHARGE, "SUCCESS", status().isOk());

        // 최초 조회
        long before = getPoint(userId);
        assertThat(before).isEqualTo(advancePoint);

        // 더 많은 금액 사용해서 실패
        performPointAction(userId, 1000L, PointAction.USE, "ERROR", status().isBadRequest());

        // 재조회
        long after = getPoint(userId);
        assertThat(after).isEqualTo(before);
    }


    // 유저 포인트 보다 많은 사용량 및 적은 사용량 요청에 대한 기본 중복 코드가 많이 작성되어 묶어서 테스트해보기?
    private static Stream<Arguments> pointUseScenarios() {
        return Stream.of(
                // 성공 케이스
                Arguments.of(1L, 500L, 200L, true, 300L, null),
                Arguments.of(2L, 500L, 500L, true, 0L, null),

                // 실패 케이스 (잔액 0)
                Arguments.of(3L, 0L, 100L, false, 0L, "잔여 포인트가 부족합니다"),

                // 실패 케이스 (잔액 부족)
                Arguments.of(4L, 100L, 200L, false, 100L, "잔여 포인트가 부족합니다")
        );
    }
    @ParameterizedTest
    @MethodSource("pointUseScenarios")
    void 포인트_사용_성공_또는_실패_통합테스트(
            Long userId,
            long initialAmount,
            long newAmount,
            boolean expectSuccess,
            long expectedRemaining,
            String expectedErrorMsg
    ) throws Exception {


        // Given - 초기 충전
        if (initialAmount > 0) {
            performPointAction(userId, initialAmount, PointAction.CHARGE, "SUCCESS", status().isOk());
        }

        // When & Then
        if (expectSuccess) {
            performPointAction(userId, newAmount, PointAction.USE, "SUCCESS", status().isOk());
        } else {
            performPointAction(userId, newAmount, PointAction.USE, "ERROR", status().isBadRequest());
        }

        // 실패 시 잔액이 유지되었는지 확인
        Long after = getPoint(userId);
        assertThat(after).isEqualTo(expectedRemaining);
    }




}
