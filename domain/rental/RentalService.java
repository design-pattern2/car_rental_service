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
import domain.user.User;
import domain.user.UserService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 대여/반납 비즈니스 로직을 담당하는 서비스.
 */
public class RentalService {

    // 연체 패널티: 초과 1일당 기본 일일요금의 30%
    private static final BigDecimal OVERDUE_RATE = new BigDecimal("0.30");

    private final RentalRepository rentalRepository;
    private final UserService userService;

    public RentalService(RentalRepository rentalRepository, UserService userService) {
        this.rentalRepository = Objects.requireNonNull(rentalRepository, "rentalRepository");
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /**
     * 대여:
     *  1) 차량 가용성 검사
     *  2) 로그인 ID → User / user.id(PK) 조회
     *  3) 옵션 + 요금제(FeeStrategy)로 총액 계산
     *  4) rental 테이블에 INSERT
     *  5) 차량 상태를 UNAVAILABLE로 변경
     *
     * @param userId      로그인 아이디 (user.userId)
     * @param car         대여할 차량
     * @param rentalDays  대여 일수
     * @param optionNames 선택 옵션 목록 (예: ["Blackbox", "Navigation"])
     * @param feeStrategy 요금 정책 전략
     */
    public RentalRecord rent(String userId,
                             Car car,
                             int rentalDays,
                             List<String> optionNames,
                             FeeStrategy feeStrategy) {

        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(car, "car");
        Objects.requireNonNull(feeStrategy, "feeStrategy");
        if (rentalDays <= 0) {
            throw new IllegalArgumentException("rentalDays must be > 0");
        }

        // 1) 로그인 ID로 User 조회 (user.userId)
        User user = userService.getUserInfo(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));

        // 2) Car.id(String) → DB car.id(INT)로 변환 (현재 설계에서 id가 숫자 문자열이라고 가정)
        final int carPk;
        try {
            carPk = Integer.parseInt(car.id());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("DB carId로 사용할 수 없는 차량 id입니다: " + car.id(), e);
        }

        int userPk = user.getId();

        // 3) 가용성 / 상태 검사
        rentalRepository.findActiveByCarId(carPk).ifPresent(r -> {
            throw new IllegalStateException("이미 대여 중인 차량입니다: carId=" + carPk);
        });
        if (car.status() != CarStatus.AVAILABLE) {
            throw new IllegalStateException("차량 상태가 AVAILABLE이 아닙니다. 현재 상태: " + car.status());
        }

        // 4) 옵션 데코레이터 체인 구성
        CarPricer pricer = buildPricer(car, optionNames);

        // 5) 요금 계산 (정책 + 옵션)
        RentalComponent adapter = new OptionPricerAdapter(pricer, car, rentalDays, feeStrategy);
        BigDecimal combined = adapter.getCost();                       // 정책 기본료 + 옵션 총액
        BigDecimal base = feeStrategy.calculateTotalFee(car, rentalDays);
        BigDecimal option = combined.subtract(base);

        // 6) RentalRecord(도메인용) 구성
        LocalDateTime now = LocalDateTime.now();

        RentalRecord rec = new RentalRecord();
        rec.setUserId(userId);             // 로그인 아이디 (문자열)
        rec.setCarId(car.id());            // UI에서 쓰는 차량 id 문자열
        rec.setRentalDays(rentalDays);
        rec.setStartAt(now);
        rec.setEndAt(now.plusDays(rentalDays)); // 예정 종료 시각
        rec.setStatus(RentalRecord.Status.RENTED);
        rec.setFeeStrategyType(feeStrategy.getClass().getSimpleName());
        rec.setMembershipStrategyType(
                user.getUserMembershipStrategy() != null
                        ? user.getUserMembershipStrategy().name()
                        : ""
        );
        rec.setOptions(new ArrayList<>(optionNames == null ? List.of() : optionNames));
        rec.setBaseFee(base);
        rec.setOptionFee(option.max(BigDecimal.ZERO));
        rec.setDiscount(BigDecimal.ZERO);
        rec.setPenalty(BigDecimal.ZERO);
        rec.setTotalFee(combined);

        // 7) DB에 저장 (userId, carId는 정수 PK 기준)
        long rentalId = rentalRepository.save(userPk, carPk, rec);
        rec.setId(rentalId);

        // 8) 차량 상태 점유
        car.occupy();

        return rec;
    }

    /**
     * 반납:
     *  1) rental 레코드 조회
     *  2) 연체 패널티 계산
     *  3) rental.status = 'RETURNED', endTime = now
     *  4) 회원 등급 자동 승급
     *  5) 차량 상태 AVAILABLE로 변경
     */
    public void returnCar(long rentalId, Car car) {
        Objects.requireNonNull(car, "car");

        // 1) DB에서 대여 레코드 조회
        RentalRecord rec = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("대여 레코드를 찾을 수 없습니다: id=" + rentalId));

        // 2) now 기준 연체 패널티 계산
        BigDecimal penalty = calculatePenaltyNow(rec, car);
        BigDecimal discount = BigDecimal.ZERO; // 결제 모듈에서 할인 확정 시 이 값 반영
        BigDecimal total = rec.getBaseFee()
                .add(rec.getOptionFee())
                .add(penalty)
                .subtract(discount);

        // 3) DB 상태 업데이트 (status='RETURNED', endTime=현재시각)
        boolean updated = rentalRepository.markReturnedIfRented(rentalId);
        if (!updated) {
            throw new IllegalStateException("반납 처리에 실패했습니다. 이미 반납되었거나 상태가 RENTED가 아닙니다: id=" + rentalId);
        }

        // 4) 회원 등급 자동 승급
        // RentalRecord.userId 는 로그인 아이디(user.userId)라고 가정
        String loginUserId = rec.getUserId();
        try {
            userService.upgradeGrade(loginUserId);
        } catch (IllegalArgumentException e) {
            // 이미 VIP이거나 사용자가 없을 때 등: 반납은 성공시키되, 승급 실패는 로그만
            System.err.println("등급 승급 중 오류: " + e.getMessage());
        }

        // 5) 메모리 상에서도 요약 정보 업데이트
        rec.setEndAt(LocalDateTime.now());
        rec.setStatus(RentalRecord.Status.RETURNED);
        rec.setPenalty(penalty);
        rec.setDiscount(discount);
        rec.setTotalFee(total);

        // 6) 차량 상태 해제
        car.release();
    }

    // ====== 내부 헬퍼 메서드 ======

    /** 옵션 이름 리스트를 기반으로 데코레이터 체인을 생성 */
    private CarPricer buildPricer(Car car, List<String> optionNames) {
        CarPricer pricer = new BaseCarPricer(car);
        if (optionNames == null) return pricer;

        for (String opt : optionNames) {
            if (opt == null) continue;
            switch (opt.trim()) {
                case "Blackbox"   -> pricer = new BlackboxOption(pricer);
                case "Navigation" -> pricer = new NavigationOption(pricer);
                case "Sunroof"    -> pricer = new SunroofOption(pricer);
                default -> {
                    // 알 수 없는 옵션은 무시
                }
            }
        }
        return pricer;
    }

    /**
     * 현재 시각(now) 기준 연체 패널티 계산.
     * - 기준: 예정 종료 시각 = startAt + rentalDays
     * - 초과 1일당 기본 일일요금의 OVERDUE_RATE 비율만큼 부과
     */
    private BigDecimal calculatePenaltyNow(RentalRecord rec, Car car) {
        LocalDateTime shouldEnd = rec.getStartAt().plusDays(rec.getRentalDays());
        LocalDateTime now = LocalDateTime.now();
        if (!now.isAfter(shouldEnd)) {
            return BigDecimal.ZERO;
        }

        long extraDays = Duration.between(shouldEnd, now).toDays();
        if (extraDays <= 0) {
            // 하루 미만 지연도 1일로 처리
            extraDays = 1;
        }

        BigDecimal daily =
                (car.getDailyRentalFee() != null)
                        ? car.getDailyRentalFee()
                        : car.type().baseRate();

        return daily
                .multiply(new BigDecimal(extraDays))
                .multiply(OVERDUE_RATE);
    }
}
