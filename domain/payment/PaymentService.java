package domain.payment;

import domain.payment.strategy.FeeStrategy;
import domain.rental.option.RentalComponent; // 데코레이터 패턴 인터페이스
import domain.user.User;
import domain.user.UserService;
import domain.car.Car;

import java.math.BigDecimal;

public class PaymentService {

    private final UserService userService;

    public PaymentService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 최종 결제 금액을 계산하고 등록된 카드로 결제를 시도합니다.
     * @param userId 결제 사용자 ID
     * @param car 대여 차량 객체
     * @param rentalDays 대여 기간 (일)
     * @param rentalOptionComponent 옵션 데코레이터가 적용된 렌탈 객체 (총 옵션 비용 포함)
     * @param feeStrategy 적용할 요금 전략
     * @return 최종 결제 결과 메시지
     */
    public String processPayment(
            String userId,
            Car car,
            int rentalDays,
            RentalComponent rentalOptionComponent,
            FeeStrategy feeStrategy)
    {
        // 1. 사용자 정보 조회 및 카드 확인
        User user = userService.getUserInfo(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String cardNumber = user.getCardNumber();

        // 카드 등록 여부 확인 (회원가입 후 등록하는 흐름 반영)
        if (cardNumber == null || cardNumber.isEmpty()) {
            return "오류: 결제를 시도했으나 **등록된 카드 정보가 없습니다**. 카드 등록 후 다시 시도해주세요.";
        }

        // 2. 기본 요금 계산 (FeeStrategy 적용)
        BigDecimal totalBaseFee = feeStrategy.calculateTotalFee(car, rentalDays);

        // 3. 옵션 추가 요금 계산 (RentalComponent/Decorator 적용)
        // 가정: RentalComponent.getCost()는 기본 요금과 모든 옵션 비용을 합친 총액을 반환
        BigDecimal feeAfterPolicyAndOption = rentalOptionComponent.getCost();

        // 4. 순수 옵션 비용 계산 (출력용)
        // 데코레이터가 적용된 최종 비용에서 기본 정책 비용을 제외하여 순수 옵션 비용을 추정
        BigDecimal totalOptionCost = feeAfterPolicyAndOption.subtract(totalBaseFee);

        // 5. 회원 할인 적용 (UserService의 User 도메인 전략 패턴)
        // 회원 할인은 옵션 비용까지 포함된 최종 금액에 적용됩니다.discountAmount
        BigDecimal finalPaymentAmount = user.applyDiscount(feeAfterPolicyAndOption);
        BigDecimal discountAmount = feeAfterPolicyAndOption.subtract(finalPaymentAmount);

        // 6. 결제 실행 시뮬레이션
        String maskedCardNumber = "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);

        System.out.println("--- [최종 결제 정보] ---");
        System.out.printf("  적용된 요금 정책: %s\n", feeStrategy.getClass().getSimpleName());
        System.out.printf("  정책 적용 기본 요금: %.0f원\n", totalBaseFee);
        System.out.printf("  추가 옵션 비용 (데코레이터): %.0f원\n", totalOptionCost);
        System.out.printf("  회원 할인 금액: %.0f원\n", discountAmount);
        System.out.printf("  최종 결제 금액: **%.0f원**\n", finalPaymentAmount);
        System.out.println("------------------------");

        // *******************************************
        //내부적으로 반납처리 구현
        // *******************************************

        return String.format("🎉 결제가 성공적으로 완료되었습니다! 등록된 카드 (%s)로 %.0f원이 결제되었습니다.",
                maskedCardNumber, finalPaymentAmount);
    }
}