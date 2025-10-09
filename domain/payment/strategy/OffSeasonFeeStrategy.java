package domain.payment.strategy;

import domain.car.Car;

import java.math.BigDecimal;

public class OffSeasonFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateTotalFee(Car car, int rentalDays) {
        // 비성수기 요금: 기본 요금에 10% 할인 (요금 정책에 포함)
        BigDecimal baseFee = car.getDailyRentalFee().multiply(new BigDecimal(rentalDays));
        return baseFee.multiply(new BigDecimal("0.9"));
    }
}