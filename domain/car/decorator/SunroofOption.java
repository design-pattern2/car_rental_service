package domain.car.decorator;

import java.math.BigDecimal;

public class SunroofOption extends CarOptionDecorator {

    public SunroofOption(CarPricer delegate) {
        super(delegate);
    }

    @Override
    protected BigDecimal optionCost() {
        return new BigDecimal("15000"); // 하루 기준 15,000원
    }

    @Override
    protected String optionName() {
        return "Sunroof";
    }
}
