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
 * 차량 반납 명령
 */
public class ReturnCarCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public ReturnCarCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[8. 차량 반납]");
        String currentId = context.getLoggedInUser().getUserId();
        
        try {
            // 1) 현재 사용자의 렌트 중인 차량 목록 조회
            domain.user.User currentUser = context.getUserService().getUserInfo(currentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            int userPk = currentUser.getId();
            
            List<domain.rental.RentalRecord> activeRentals = context.getRentalRepository().findActiveByUserId(userPk);
            
            if (activeRentals.isEmpty()) {
                System.out.println("❌ 현재 대여 중인 차량이 없습니다.");
                return;
            }
            
            // 2) 렌트 중인 차량 목록 표시
            System.out.println("\n현재 대여 중인 차량 목록:");
            System.out.println("-".repeat(60));
            List<domain.rental.RentalRecord> validRecords = new ArrayList<>();
            for (int i = 0; i < activeRentals.size(); i++) {
                domain.rental.RentalRecord record = activeRentals.get(i);
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
                validRecords.add(record);
            }
            System.out.println("-".repeat(60));
            
            if (validRecords.isEmpty()) {
                System.err.println("❌ 반납 가능한 차량이 없습니다.");
                return;
            }
            
            // 3) 차량 이름으로 반납할 차량 선택
            System.out.print("\n반납할 차량 이름 입력: ");
            String returnCarName = scanner.nextLine().trim();
            
            domain.rental.RentalRecord selectedRecord = null;
            domain.car.Car returnCar = null;
            for (domain.rental.RentalRecord record : validRecords) {
                String carIdStr = record.getCarId();
                domain.car.Car car = context.getCarRepository().findById(carIdStr);
                if (car != null && car.getName().equals(returnCarName)) {
                    domain.rental.RentalRecord cachedRecord = context.getRentalRecordCache().get(record.getId());
                    if (cachedRecord != null) {
                        selectedRecord = cachedRecord;
                    } else {
                        selectedRecord = record;
                    }
                    returnCar = car;
                    break;
                }
            }
            
            if (selectedRecord == null || returnCar == null) {
                System.err.println("❌ 해당 이름의 대여 중인 차량을 찾을 수 없습니다.");
                return;
            }
            
            // baseFee와 optionFee가 0이면 캐시에서 다시 확인
            if ((selectedRecord.getBaseFee() == null || selectedRecord.getBaseFee().compareTo(BigDecimal.ZERO) == 0) &&
                (selectedRecord.getOptionFee() == null || selectedRecord.getOptionFee().compareTo(BigDecimal.ZERO) == 0)) {
                domain.rental.RentalRecord cachedRecordForReturn = context.getRentalRecordCache().get(selectedRecord.getId());
                if (cachedRecordForReturn != null) {
                    selectedRecord = cachedRecordForReturn;
                }
            }
            
            long rentalId = selectedRecord.getId();
            
            // 반납 전 사용자 등급 저장
            domain.user.User userBeforeReturn = context.getUserService().getUserInfo(currentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            String membershipBefore = userBeforeReturn.getUserMembershipStrategy().getClass().getSimpleName();
            
            // 반납 실행
            context.getRentalService().returnCar(rentalId, returnCar, selectedRecord);
            
            // 차량 상태를 DB에 업데이트
            returnCar.release();
            context.getCarRepository().update(returnCar);
            
            System.out.println("\n✅ 반납이 성공적으로 완료되었습니다!");
            System.out.println("\n반납 요금은 다음과 같습니다:\n");
            
            // 요금 명세서 출력
            BigDecimal dailyFee = returnCar.getDailyRentalFee() != null ?
                returnCar.getDailyRentalFee() : returnCar.type().baseRate();
            int returnRentalDays = selectedRecord.getRentalDays();
            
            // 요금 정책 재구성
            FeeStrategy returnFeeStrategy;
            String feeStrategyType = selectedRecord.getFeeStrategyType();
            if (feeStrategyType == null || feeStrategyType.isEmpty()) {
                returnFeeStrategy = new BaseFeeStrategy();
            } else if ("PeakSeasonFeeStrategy".equals(feeStrategyType)) {
                returnFeeStrategy = new PeakSeasonFeeStrategy();
            } else if ("OffSeasonFeeStrategy".equals(feeStrategyType)) {
                returnFeeStrategy = new OffSeasonFeeStrategy();
            } else {
                returnFeeStrategy = new BaseFeeStrategy();
            }
            
            BigDecimal baseFee = returnFeeStrategy.calculateTotalFee(returnCar, returnRentalDays);
            
            String policyDescription = "";
            if (returnFeeStrategy instanceof PeakSeasonFeeStrategy) {
                policyDescription = " (20% 할증)";
            } else if (returnFeeStrategy instanceof OffSeasonFeeStrategy) {
                policyDescription = " (10% 할인)";
            }
            
            // 옵션 비용 계산
            Map<String, BigDecimal> optionCosts = new HashMap<>();
            List<String> returnOptions = selectedRecord.getOptions();
            if (returnOptions != null && !returnOptions.isEmpty()) {
                for (String option : returnOptions) {
                    BigDecimal optionDailyCost = switch (option) {
                        case "Blackbox" -> new BigDecimal("5000");
                        case "Navigation" -> new BigDecimal("7000");
                        case "Sunroof" -> new BigDecimal("15000");
                        default -> BigDecimal.ZERO;
                    };
                    BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(returnRentalDays));
                    optionCosts.put(option, optionTotal);
                }
            }
            
            BigDecimal penalty = selectedRecord.getPenalty() != null ? selectedRecord.getPenalty() : BigDecimal.ZERO;
            
            // 요금 계산 과정 출력
            System.out.println("--- [반납 요금 계산 내역] ---");
            System.out.printf("차량: %s (%s)%n", returnCar.getName(), returnCar.type());
            System.out.printf("차량 일일 요금: %s원%n", context.formatMoney(dailyFee));
            System.out.printf("대여 일수: %d일%n", returnRentalDays);
            System.out.printf("요금 정책: %s%s%n", returnFeeStrategy.getClass().getSimpleName(), policyDescription);
            
            // 옵션 표시
            if (!optionCosts.isEmpty()) {
                System.out.print("옵션: ");
                List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                for (int i = 0; i < optionNames.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    String optionName = optionNames.get(i);
                    BigDecimal optionTotal = optionCosts.get(optionName);
                    BigDecimal optionDaily = optionTotal.divide(new BigDecimal(returnRentalDays));
                    System.out.printf("%s (%s원/일)", optionName, context.formatMoney(optionDaily));
                }
                System.out.println();
            } else {
                System.out.println("옵션: 없음");
            }
            System.out.println();
            
            // 간단한 계산식 출력
            String policyPercent = "";
            if (returnFeeStrategy instanceof PeakSeasonFeeStrategy) {
                policyPercent = " × 120%";
            } else if (returnFeeStrategy instanceof OffSeasonFeeStrategy) {
                policyPercent = " × 90%";
            }
            
            BigDecimal totalOptionCost = BigDecimal.ZERO;
            if (!optionCosts.isEmpty()) {
                for (BigDecimal cost : optionCosts.values()) {
                    totalOptionCost = totalOptionCost.add(cost);
                }
            }
            
            BigDecimal calculatedTotal = baseFee.add(totalOptionCost);
            System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                context.formatMoney(dailyFee), returnRentalDays, policyPercent,
                context.formatMoney(totalOptionCost), context.formatMoney(calculatedTotal));
            System.out.println();
            
            BigDecimal rentalFee = calculatedTotal;
            System.out.printf("대여 시 요금: %s원%n", context.formatMoney(rentalFee));
            
            BigDecimal discountedAmount = userBeforeReturn.applyDiscount(rentalFee);
            BigDecimal discount = rentalFee.subtract(discountedAmount);
            BigDecimal totalFee = discountedAmount.add(penalty);
            
            String membershipName = userBeforeReturn.getUserMembershipStrategy().getClass().getSimpleName();
            String membershipDisplay = membershipName.replace("Strategy", "").toUpperCase();
            
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                System.out.printf("회원 등급 할인(%s): -%s원%n", membershipDisplay, context.formatMoney(discount));
            } else {
                System.out.printf("회원 등급 할인(%s): 없음%n", membershipDisplay);
            }
            
            System.out.println();
            System.out.printf("총 결제 금액: %s원%n", context.formatMoney(totalFee));
            System.out.println("-------------------\n");
            
            // 등급 승급 확인
            domain.user.User userAfterReturn = context.getUserService().getUserInfo(currentId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            context.setLoggedInUser(userAfterReturn);
            String membershipAfter = userAfterReturn.getUserMembershipStrategy().getClass().getSimpleName();
            
            if (!membershipBefore.equals(membershipAfter)) {
                String beforeGrade = membershipBefore.replace("Strategy", "");
                String afterGrade = membershipAfter.replace("Strategy", "");
                System.out.printf("🎉 회원 등급이 %s에서 %s로 올랐습니다!%n", beforeGrade, afterGrade);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 반납 실패: " + e.getMessage());
        }
    }
}

