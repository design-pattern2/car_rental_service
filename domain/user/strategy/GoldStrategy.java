package domain.user.strategy;

import java.math.BigDecimal;

public class GoldStrategy implements UserMembershipStrategy{
    @Override
    public BigDecimal calculateDiscount(BigDecimal decimal) {
        return decimal.multiply(new BigDecimal("0.9"));
    }
}
