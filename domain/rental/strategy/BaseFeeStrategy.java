package domain.rental.strategy;

import domain.car.Car;

import java.math.BigDecimal;

public class BaseFeeStrategy implements FeeStrategy {
    @Override
    public BigDecimal calculateTotalFee(Car car, int rentalDays) {
        // 기본 요금: 일일 대여료 * 대여 기간
        BigDecimal dailyFee = car.getDailyRentalFee();
        if (dailyFee == null) {
            // dailyRentalFee가 null이면 타입의 기본 요금 사용
            dailyFee = car.type().baseRate();
        }
        return dailyFee.multiply(new BigDecimal(rentalDays));
    }
}

