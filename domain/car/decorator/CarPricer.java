package domain.car.decorator;

import java.math.BigDecimal;

import domain.car.Car;

public interface CarPricer {
    BigDecimal quote(Car car);   // 최종 차량 요금 계산
    String description();        // 옵션 설명
}