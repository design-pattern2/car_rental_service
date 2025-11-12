package domain.rental;

import domain.car.Car;
import domain.car.car_Factory.CarStatus;
import domain.car.decorator.BaseCarPricer;
import domain.car.decorator.BlackboxOption;
import domain.car.decorator.CarPricer;
import domain.car.decorator.NavigationOption;
import domain.car.decorator.SunroofOption;
import domain.payment.strategy.FeeStrategy;
import domain.rental.option.OptionPricerAdapter;
import domain.rental.option.RentalComponent;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RentalService {
    private final RentalRepository rentalRepository;

    // 연체 패널티: 초과 1일당 기본 일일요금의 30% (요구사항에 맞게 조정 가능)
    private static final BigDecimal OVERDUE_RATE = new BigDecimal("0.30");

    public RentalService(RentalRepository rentalRepository) {
        this.rentalRepository = rentalRepository;
    }

    /** 대여: 가용성 검사 → 금액계산(정책+옵션) → 저장 → 차량 점유 */
    public RentalRecord rent(String userId, Car car, int rentalDays, List<String> optionNames, FeeStrategy feeStrategy) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(car, "car");
        Objects.requireNonNull(feeStrategy, "feeStrategy");
        if (rentalDays <= 0) throw new IllegalArgumentException("rentalDays must be > 0");

        // 가용성/상태 체크
        rentalRepository.findActiveByCarId(car.id()).ifPresent(r -> {
            throw new IllegalStateException("이미 대여 중인 차량입니다: " + car.id());
        });
        if (car.status() != CarStatus.AVAILABLE) {
            throw new IllegalStateException("차량 상태가 AVAILABLE이 아닙니다.");
        }

        // 옵션 데코레이터 체인 구성
        CarPricer pricer = buildPricer(car, optionNames);

        // 금액 계산
        RentalComponent adapter = new OptionPricerAdapter(pricer, car, rentalDays, feeStrategy);
        BigDecimal combined = adapter.getCost();                       // 정책 기본료 + 옵션 총액
        BigDecimal base = feeStrategy.calculateTotalFee(car, rentalDays);
        BigDecimal option = combined.subtract(base);

        // 레코드 저장
        RentalRecord rec = new RentalRecord();
        rec.setUserId(userId);
        rec.setCarId(car.id());
        rec.setRentalDays(rentalDays);
        rec.setStartAt(LocalDateTime.now());
        rec.setStatus(RentalRecord.Status.RENTED);
        rec.setFeeStrategyType(feeStrategy.getClass().getSimpleName());
        rec.setMembershipStrategyType(""); // 결제 시 UserStrategy가 적용된다면 빈칸 유지 가능
        rec.setOptions(new ArrayList<>(optionNames == null ? List.of() : optionNames));
        rec.setBaseFee(base);
        rec.setOptionFee(option.max(BigDecimal.ZERO));
        rec.setDiscount(BigDecimal.ZERO);
        rec.setPenalty(BigDecimal.ZERO);
        rec.setTotalFee(combined);

        rentalRepository.save(rec);

        // 차량 점유
        car.occupy();

        return rec;
    }

    /** 반납: 연체 패널티 계산 → 레코드 마킹(RETURNED) → 차량 해제 */
    public void returnCar(long rentalId, Car car) {
        RentalRecord rec = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("대여 레코드를 찾을 수 없습니다: id=" + rentalId));

        // now 기준 연체 계산
        BigDecimal penalty = calculatePenaltyNow(rec, car);
        BigDecimal discount = BigDecimal.ZERO; // 결제 모듈에서 할인 확정 시 이 값 반영
        BigDecimal total = rec.getBaseFee().add(rec.getOptionFee()).add(penalty).subtract(discount);

        rentalRepository.markReturned(rentalId, penalty, discount, total);

        // 차량 상태 해제
        car.release();
    }

    // ===== helpers =====
    private CarPricer buildPricer(Car car, List<String> optionNames) {
        CarPricer pricer = new BaseCarPricer(car);
        if (optionNames == null) return pricer;
        for (String opt : optionNames) {
            if (opt == null) continue;
            switch (opt.trim()) {
                case "Blackbox"   -> pricer = new BlackboxOption(pricer);
                case "Navigation" -> pricer = new NavigationOption(pricer);
                case "Sunroof"    -> pricer = new SunroofOption(pricer);
                default -> { /* 알 수 없는 옵션은 무시 */ }
            }
        }
        return pricer;
    }

    private BigDecimal calculatePenaltyNow(RentalRecord rec, Car car) {
        LocalDateTime shouldEnd = rec.getStartAt().plusDays(rec.getRentalDays());
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(shouldEnd)) return BigDecimal.ZERO;

        long extraDays = Duration.between(shouldEnd, now).toDays();
        if (extraDays <= 0) extraDays = 1; // 정책상 하루 미만 지연도 1일로 처리 시
        return car.getDailyRentalFee()
                .multiply(new BigDecimal(extraDays))
                .multiply(OVERDUE_RATE);
    }
}
