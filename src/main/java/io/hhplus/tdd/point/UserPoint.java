package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    public long charge(long amount){
        if(amount <= 0){
            throw new RuntimeException("Charge Should Be Positive");
        }

        if(amount >= 1000000L){
            throw new RuntimeException("Charge Should Be Less Then a Million");
        }

        return this.point() + amount;
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
