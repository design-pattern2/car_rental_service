package car_Factory;

import java.math.BigDecimal;

class SEDANPolicy implements CarTypePolicy {
	@Override
	public BigDecimal applyFee(BigDecimal price) {
		return price;
	}
}