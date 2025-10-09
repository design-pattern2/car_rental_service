package domain.payment.strategy;

import domain.car.Car;

import java.math.BigDecimal;

public class PeakSeasonFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateTotalFee(Car car, int rentalDays) {
        // 성수기 요금: 기본 요금에 20% 할증
        BigDecimal baseFee = car.getDailyRentalFee().multiply(new BigDecimal(rentalDays));
        return baseFee.multiply(new BigDecimal("1.2"));
    }
}