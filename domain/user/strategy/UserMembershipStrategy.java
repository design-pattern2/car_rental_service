package domain.user.strategy;
import java.math.BigDecimal;

public interface UserMembershipStrategy {
    BigDecimal calculateDiscount(BigDecimal decimal);
    default String name() { return getClass().getSimpleName(); }
}
