package domain.car.decorator;

import java.math.BigDecimal;

public class BlackboxOption extends CarOptionDecorator {

    public BlackboxOption(CarPricer delegate) {
        super(delegate);
    }

    @Override
    protected BigDecimal optionCost() {
        return new BigDecimal("5000"); // 하루 5천원
    }

    @Override
    protected String optionName() {
        return "Blackbox";
    }
}
