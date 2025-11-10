package domain.car.decorator;

import java.math.BigDecimal;

public class NavigationOption extends CarOptionDecorator {

    public NavigationOption(CarPricer delegate) {
        super(delegate);
    }

    @Override
    protected BigDecimal optionCost() {
        return new BigDecimal("7000");
    }

    @Override
    protected String optionName() {
        return "Navigation";
    }
}
