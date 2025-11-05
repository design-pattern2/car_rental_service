package car_Factory;

import java.math.BigDecimal;

public enum CarType {
	SEDAN(new BigDecimal("90000")),
    SUV  (new BigDecimal("140000")),
    BIKE   (new BigDecimal("230000"));

    private final BigDecimal baseRate;
    CarType(BigDecimal baseRate) { this.baseRate = baseRate; }
    public BigDecimal baseRate() { return baseRate; }
}