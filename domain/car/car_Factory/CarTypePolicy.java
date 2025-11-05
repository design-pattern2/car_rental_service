package car_Factory;

import java.math.BigDecimal;

interface CarTypePolicy {
	BigDecimal applyFee(BigDecimal price);
}