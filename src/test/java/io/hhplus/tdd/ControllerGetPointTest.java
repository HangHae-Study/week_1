package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;



@SpringBootTest
@AutoConfigureMockMvc
public class ControllerGetPointTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PointService pointService;

    @Autowired
    UserPointTable userPointTable;

    @Autowired
    PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(pointHistoryTable, userPointTable);
    }

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

    @Test
    void T2_등록되지_않은_유저의_포인트를_조회한다() throws Exception{
        Long userId = 1L;

        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.point").value(0));
    }

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

}
