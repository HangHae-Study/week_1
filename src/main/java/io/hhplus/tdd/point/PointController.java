package io.hhplus.tdd.point;

import io.hhplus.tdd.dto.PointAmountDto;
import io.hhplus.tdd.dto.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    @Autowired
    PointService pointService;

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public ResponseEntity<ResponseDto<?>> point(@PathVariable long id) {
        UserPoint point = pointService.getPoint(id);
        return ResponseEntity.ok(ResponseDto.success(point));
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public ResponseEntity<ResponseDto<?>> history(@PathVariable long id) {
        List<PointHistory> histories = pointService.getHistories(id);
        return ResponseEntity.ok(ResponseDto.success(histories));
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public ResponseEntity<ResponseDto<?>>  charge( @PathVariable long id, @RequestBody PointAmountDto rq) {
        UserPoint charged = pointService.charge(id, rq.amount());
        return ResponseEntity.ok(ResponseDto.success(charged));
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public ResponseEntity<ResponseDto<?>> use(@PathVariable long id, @RequestBody PointAmountDto rq) {
        UserPoint curPoint = pointService.getPoint(id);
        try{
            UserPoint usePoint = pointService.use(id, rq.amount());
            return ResponseEntity.ok(ResponseDto.success(usePoint));
        }catch (Exception e){
            return ResponseEntity.badRequest().body(ResponseDto.error("잔여 포인트가 부족합니다"));
        }
    }
}
