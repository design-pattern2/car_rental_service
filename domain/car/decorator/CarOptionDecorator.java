package domain.car.decorator;

import java.math.BigDecimal;

import domain.car.Car;

public abstract class CarOptionDecorator implements CarPricer {

    protected final CarPricer delegate;

    protected CarOptionDecorator(CarPricer delegate) {
        this.delegate = delegate;
    }

    @Override
    public BigDecimal quote(Car car) {
        return delegate.quote(car).add(optionCost());
    }

    @Override
    public String description() {
        return delegate.description() + " + " + optionName();
    }

    protected abstract BigDecimal optionCost();
    protected abstract String optionName();
}
