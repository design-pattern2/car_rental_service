package domain.rental.strategy;

import domain.car.Car;

import java.math.BigDecimal;

/**
 * 기본 대여료에 기간별 요금 정책을 적용하는 전략 인터페이스.
 */
public interface FeeStrategy {

    /**
     * 차량의 기본 요금과 기간을 고려하여 정책이 적용된 요금을 계산합니다.
     *
     * @param car        대여 차량 객체
     * @param rentalDays 대여 기간 (일)
     * @return 정책이 적용된 총 대여료
     */
    BigDecimal calculateTotalFee(Car car, int rentalDays);
}

