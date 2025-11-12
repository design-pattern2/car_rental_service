package domain.rental.option;

import domain.car.Car;
import domain.car.decorator.CarPricer;
import domain.payment.strategy.FeeStrategy;

import java.math.BigDecimal;

/**
 * 데코레이터(옵션) 요금 + 정책(FeeStrategy) + 대여 일수 → 총액을 산출하는 어댑터.
 * - pricer.quote(car): 1일 기준 "기본+옵션" 요금이라고 가정
 * - car.getDailyRentalFee(): 1일 기본요금
 */
public class OptionPricerAdapter implements RentalComponent {
    private final CarPricer pricer;
    private final Car car;
    private final int rentalDays;
    private final FeeStrategy feeStrategy;

    public OptionPricerAdapter(CarPricer pricer, Car car, int rentalDays, FeeStrategy feeStrategy) {
        this.pricer = pricer;
        this.car = car;
        this.rentalDays = rentalDays;
        this.feeStrategy = feeStrategy;
    }

    @Override
    public BigDecimal getCost() {
        // 정책 적용 기본료(일수 포함)
        BigDecimal base = feeStrategy.calculateTotalFee(car, rentalDays);

        // 옵션 총액 = (옵션포함 1일가 - 기본 1일가) * 일수
        BigDecimal perDayWithOptions = pricer.quote(car);
        BigDecimal perDayBase = car.getDailyRentalFee();
        BigDecimal optionPerDay = perDayWithOptions.subtract(perDayBase);
        if (optionPerDay.signum() < 0) optionPerDay = BigDecimal.ZERO;

        BigDecimal optionTotal = optionPerDay.multiply(new BigDecimal(rentalDays));
        return base.add(optionTotal);
    }
}
