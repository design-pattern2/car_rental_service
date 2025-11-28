package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;
import domain.rental.strategy.BaseFeeStrategy;
import domain.rental.strategy.FeeStrategy;
import domain.rental.strategy.PeakSeasonFeeStrategy;
import domain.rental.strategy.OffSeasonFeeStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 결제 금액 확인 명령
 */
public class ViewPaymentCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public ViewPaymentCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[9. 결제 금액 확인]");
        String currentId = context.getLoggedInUser().getUserId();
        
        try {
            // 1) 현재 사용자의 렌트 중인 차량 목록 조회
            domain.user.User currentUserForPayment = context.getUserService().getUserInfo(currentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            int userPkForPayment = currentUserForPayment.getId();
            
            List<domain.rental.RentalRecord> activeRentalsForPayment = context.getRentalRepository().findActiveByUserId(userPkForPayment);
            
            if (activeRentalsForPayment.isEmpty()) {
                System.out.println("❌ 현재 대여 중인 차량이 없습니다.");
                return;
            }
            
            // 2) 렌트 중인 차량 목록 표시
            System.out.println("\n현재 대여 중인 차량 목록:");
            System.out.println("-".repeat(60));
            List<domain.rental.RentalRecord> validRecordsForPayment = new ArrayList<>();
            for (int i = 0; i < activeRentalsForPayment.size(); i++) {
                domain.rental.RentalRecord record = activeRentalsForPayment.get(i);
                domain.rental.RentalRecord cachedRecord = context.getRentalRecordCache().get(record.getId());
                if (cachedRecord != null) {
                    record = cachedRecord;
                }
                
                String carIdStr = record.getCarId();
                domain.car.Car car = context.getCarRepository().findById(carIdStr);
                if (car == null) {
                    continue;
                }
                
                String displayCarName = car.getName();
                String startDate = record.getStartAt() != null ?
                    record.getStartAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
                    "알 수 없음";
                
                System.out.printf("%d. %s | 대여일: %s%n", i + 1, displayCarName, startDate);
                validRecordsForPayment.add(record);
            }
            System.out.println("-".repeat(60));
            
            if (validRecordsForPayment.isEmpty()) {
                System.err.println("❌ 확인 가능한 차량이 없습니다.");
                return;
            }
            
            // 3) 차량 이름으로 확인할 차량 선택
            System.out.print("\n결제 금액을 확인할 차량 이름 입력: ");
            String paymentCarName = scanner.nextLine().trim();
            
            domain.rental.RentalRecord selectedRecordForPayment = null;
            domain.car.Car paymentCar = null;
            for (domain.rental.RentalRecord record : validRecordsForPayment) {
                String carIdStr = record.getCarId();
                domain.car.Car car = context.getCarRepository().findById(carIdStr);
                if (car != null && car.getName().equals(paymentCarName)) {
                    domain.rental.RentalRecord cachedRecord = context.getRentalRecordCache().get(record.getId());
                    if (cachedRecord != null) {
                        selectedRecordForPayment = cachedRecord;
                    } else {
                        selectedRecordForPayment = record;
                    }
                    paymentCar = car;
                    break;
                }
            }
            
            if (selectedRecordForPayment == null || paymentCar == null) {
                System.err.println("❌ 해당 이름의 대여 중인 차량을 찾을 수 없습니다.");
                return;
            }
            
            // baseFee와 optionFee가 0이면 캐시에서 다시 확인
            if ((selectedRecordForPayment.getBaseFee() == null || selectedRecordForPayment.getBaseFee().compareTo(BigDecimal.ZERO) == 0) &&
                (selectedRecordForPayment.getOptionFee() == null || selectedRecordForPayment.getOptionFee().compareTo(BigDecimal.ZERO) == 0)) {
                domain.rental.RentalRecord cachedRecordForPayment = context.getRentalRecordCache().get(selectedRecordForPayment.getId());
                if (cachedRecordForPayment != null) {
                    selectedRecordForPayment = cachedRecordForPayment;
                }
            }
            
            // 대여 시 청구한 금액 표시
            BigDecimal dailyFee = paymentCar.getDailyRentalFee() != null ?
                paymentCar.getDailyRentalFee() : paymentCar.type().baseRate();
            int paymentRentalDays = selectedRecordForPayment.getRentalDays();
            
            // 요금 정책 재구성
            FeeStrategy paymentFeeStrategy;
            String feeStrategyType = selectedRecordForPayment.getFeeStrategyType();
            if (feeStrategyType == null || feeStrategyType.isEmpty()) {
                paymentFeeStrategy = new BaseFeeStrategy();
            } else if ("PeakSeasonFeeStrategy".equals(feeStrategyType)) {
                paymentFeeStrategy = new PeakSeasonFeeStrategy();
            } else if ("OffSeasonFeeStrategy".equals(feeStrategyType)) {
                paymentFeeStrategy = new OffSeasonFeeStrategy();
            } else {
                paymentFeeStrategy = new BaseFeeStrategy();
            }
            
            BigDecimal baseFee = paymentFeeStrategy.calculateTotalFee(paymentCar, paymentRentalDays);
            
            String policyDescription = "";
            if (paymentFeeStrategy instanceof PeakSeasonFeeStrategy) {
                policyDescription = " (20% 할증)";
            } else if (paymentFeeStrategy instanceof OffSeasonFeeStrategy) {
                policyDescription = " (10% 할인)";
            }
            
            // 옵션 비용 계산
            Map<String, BigDecimal> optionCosts = new HashMap<>();
            List<String> paymentOptions = selectedRecordForPayment.getOptions();
            if (paymentOptions != null && !paymentOptions.isEmpty()) {
                for (String option : paymentOptions) {
                    BigDecimal optionDailyCost = switch (option) {
                        case "Blackbox" -> new BigDecimal("5000");
                        case "Navigation" -> new BigDecimal("7000");
                        case "Sunroof" -> new BigDecimal("15000");
                        default -> BigDecimal.ZERO;
                    };
                    BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(paymentRentalDays));
                    optionCosts.put(option, optionTotal);
                }
            }
            
            BigDecimal totalOptionCost = BigDecimal.ZERO;
            if (!optionCosts.isEmpty()) {
                for (BigDecimal cost : optionCosts.values()) {
                    totalOptionCost = totalOptionCost.add(cost);
                }
            }
            
            BigDecimal totalFee = baseFee.add(totalOptionCost);
            
            // 요금 계산 과정 출력
            System.out.println("\n--- [요금 계산 내역] ---");
            System.out.printf("차량: %s (%s)%n", paymentCar.getName(), paymentCar.type());
            System.out.printf("차량 일일 요금: %s원%n", context.formatMoney(dailyFee));
            System.out.printf("대여 일수: %d일%n", paymentRentalDays);
            System.out.printf("요금 정책: %s%s%n", paymentFeeStrategy.getClass().getSimpleName(), policyDescription);
            
            // 옵션 표시
            if (!optionCosts.isEmpty()) {
                System.out.print("옵션: ");
                List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                for (int i = 0; i < optionNames.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    String optionName = optionNames.get(i);
                    BigDecimal optionTotal = optionCosts.get(optionName);
                    BigDecimal optionDaily = optionTotal.divide(new BigDecimal(paymentRentalDays));
                    System.out.printf("%s (%s원/일)", optionName, context.formatMoney(optionDaily));
                }
                System.out.println();
            } else {
                System.out.println("옵션: 없음");
            }
            System.out.println();
            
            // 간단한 계산식 출력
            String policyPercent = "";
            if (paymentFeeStrategy instanceof PeakSeasonFeeStrategy) {
                policyPercent = " × 120%";
            } else if (paymentFeeStrategy instanceof OffSeasonFeeStrategy) {
                policyPercent = " × 90%";
            }
            
            System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                context.formatMoney(dailyFee), paymentRentalDays, policyPercent,
                context.formatMoney(totalOptionCost), context.formatMoney(totalFee));
            System.out.println("-------------------\n");
            
        } catch (Exception e) {
            System.err.println("❌ 결제 금액 확인 실패: " + e.getMessage());
        }
    }
}

