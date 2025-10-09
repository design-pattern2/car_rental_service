package domain.user.strategy;

import java.math.BigDecimal;

public class VIPStrategy implements UserMembershipStrategy{
    @Override
    public BigDecimal calculateDiscount(BigDecimal decimal) {
        return decimal.multiply(new BigDecimal("0.80"));
    }
}
