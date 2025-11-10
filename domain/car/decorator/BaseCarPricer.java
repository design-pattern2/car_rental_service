package domain.car.decorator;

import java.math.BigDecimal;

import domain.car.Car;

public class BaseCarPricer implements CarPricer {

    private final Car car;

    public BaseCarPricer(Car car) {
        this.car = car;
    }

    @Override
    public BigDecimal quote(Car car) {
        return car.type().baseRate();
    }

    @Override
    public String description() {
        return car.type().name();
    }
}