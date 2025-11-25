package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public UserPoint pointCharge(long point){

        return new UserPoint(
                this.id,
                this.point + point,
                System.currentTimeMillis());
    }

    public UserPoint pointPay(long point){
        if(this.point == 0 || this.point < point){
            throw new RuntimeException("Pay Point Failed");
        }

        return new UserPoint(
                this.id,
                this.point - point,
                System.currentTimeMillis());
    }
}
