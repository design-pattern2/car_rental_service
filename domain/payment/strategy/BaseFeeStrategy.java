package domain.payment.strategy;

import domain.car.Car;

import java.math.BigDecimal;

public class BaseFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateTotalFee(Car car, int rentalDays) {
        // 기본 요금: 일일 대여료 * 대여 기간
        return car.getDailyRentalFee().multiply(new BigDecimal(rentalDays));
    }
}