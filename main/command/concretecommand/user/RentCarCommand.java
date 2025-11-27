package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;
import domain.car.carFactory.CarType;
import domain.payment.strategy.FeeStrategy;
import domain.payment.strategy.PeakSeasonFeeStrategy;
import domain.payment.strategy.OffSeasonFeeStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Command Pattern: ConcreteCommand
 * 차량 대여 명령
 */
public class RentCarCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public RentCarCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[7. 차량 대여]");
        String currentId = context.getLoggedInUser().getUserId();
        
        try {
            // 1) 차량 타입 선택
            System.out.println("차량 타입 선택:");
            System.out.println("  1. SEDAN");
            System.out.println("  2. SUV");
            System.out.println("  3. BIKE");
            System.out.print("선택: ");
            int rentTypeChoice = Integer.parseInt(scanner.nextLine());
            CarType rentType = switch (rentTypeChoice) {
                case 1 -> CarType.SEDAN;
                case 2 -> CarType.SUV;
                case 3 -> CarType.BIKE;
                default -> throw new IllegalArgumentException("잘못된 타입 선택");
            };
            
            // 2) 해당 타입의 사용 가능한 차량 목록 표시
            List<domain.car.Car> allCars = context.getCarRepository().findAllCars();
            List<domain.car.Car> cars = allCars.stream()
                .filter(car -> car.status() == domain.car.carFactory.CarStatus.AVAILABLE && car.type() == rentType)
                .collect(Collectors.toList());
            if (cars.isEmpty()) {
                System.out.println("❌ 현재 대여 가능한 " + rentType + " 차량이 없습니다.");
                return;
            }
            System.out.println("\n대여 가능한 " + rentType + " 차량 목록:");
            for (int i = 0; i < cars.size(); i++) {
                domain.car.Car car = cars.get(i);
                BigDecimal fee = car.getDailyRentalFee() != null ? car.getDailyRentalFee() : car.type().baseRate();
                System.out.printf("%d. %s | 일일 요금: %s원%n",
                    i + 1, car.getName(), context.formatMoney(fee));
            }
            
            // 3) 차량 이름으로 선택
            System.out.print("\n대여할 차량 이름 입력: ");
            String carName = scanner.nextLine().trim();
            Optional<domain.car.Car> carOpt = cars.stream()
                .filter(car -> car.getName().equals(carName))
                .findFirst();
            if (carOpt.isEmpty()) {
                System.err.println("❌ 해당 이름의 차량을 찾을 수 없습니다.");
                return;
            }
            domain.car.Car selectedCar = carOpt.get();
            
            if (selectedCar.status() != domain.car.carFactory.CarStatus.AVAILABLE) {
                System.err.println("❌ 해당 차량은 현재 대여 불가능합니다.");
                return;
            }
            
            // 4) 대여 일수 입력
            System.out.print("대여 일수: ");
            int rentalDays = Integer.parseInt(scanner.nextLine());
            if (rentalDays <= 0) {
                System.err.println("❌ 대여 일수는 1일 이상이어야 합니다.");
                return;
            }
            
            // 5) 옵션 선택
            System.out.println("추가 옵션 선택 (여러 개 선택 가능, 엔터로 종료):");
            System.out.println("  - Blackbox (블랙박스) - 5,000원/일");
            System.out.println("  - Navigation (네비게이션) - 7,000원/일");
            System.out.println("  - Sunroof (선루프) - 15,000원/일");
            List<String> options = new ArrayList<>();
            while (true) {
                System.out.print("옵션 이름 (엔터로 종료): ");
                String option = scanner.nextLine().trim();
                if (option.isEmpty()) break;
                if (option.equals("Blackbox") || option.equals("Navigation") || option.equals("Sunroof")) {
                    options.add(option);
                    System.out.println("✅ " + option + " 옵션이 추가되었습니다.");
                } else {
                    System.out.println("⚠️ 알 수 없는 옵션입니다. 무시됩니다.");
                }
            }
            
            // 6) 요금 정책: 현재 설정된 시즌 사용
            FeeStrategy feeStrategy = context.getCurrentSeason();
            
            // 7) 대여 실행 및 요금 계산 과정 출력
            BigDecimal dailyFee = selectedCar.getDailyRentalFee() != null ?
                selectedCar.getDailyRentalFee() : selectedCar.type().baseRate();
            
            BigDecimal baseFee = feeStrategy.calculateTotalFee(selectedCar, rentalDays);
            
            String policyDescription = "";
            if (feeStrategy instanceof PeakSeasonFeeStrategy) {
                policyDescription = " (20% 할증)";
            } else if (feeStrategy instanceof OffSeasonFeeStrategy) {
                policyDescription = " (10% 할인)";
            }
            
            // 옵션 비용 계산
            BigDecimal totalOptionCost = BigDecimal.ZERO;
            Map<String, BigDecimal> optionCosts = new HashMap<>();
            if (options != null && !options.isEmpty()) {
                for (String option : options) {
                    BigDecimal optionDailyCost = switch (option) {
                        case "Blackbox" -> new BigDecimal("5000");
                        case "Navigation" -> new BigDecimal("7000");
                        case "Sunroof" -> new BigDecimal("15000");
                        default -> BigDecimal.ZERO;
                    };
                    BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(rentalDays));
                    optionCosts.put(option, optionTotal);
                    totalOptionCost = totalOptionCost.add(optionTotal);
                }
            }
            
            BigDecimal totalFee = baseFee.add(totalOptionCost);
            
            // 요금 계산 과정 출력
            System.out.println("\n--- [요금 계산 내역] ---");
            System.out.printf("차량: %s (%s)%n", selectedCar.id(), selectedCar.type());
            System.out.printf("차량 일일 요금: %s원%n", context.formatMoney(dailyFee));
            System.out.printf("대여 일수: %d일%n", rentalDays);
            System.out.printf("요금 정책: %s%s%n", feeStrategy.getClass().getSimpleName(), policyDescription);
            
            // 옵션 표시
            if (!optionCosts.isEmpty()) {
                System.out.print("옵션: ");
                List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                for (int i = 0; i < optionNames.size(); i++) {
                    if (i > 0) System.out.print(", ");
                    String optionName = optionNames.get(i);
                    BigDecimal optionTotal = optionCosts.get(optionName);
                    BigDecimal optionDaily = optionTotal.divide(new BigDecimal(rentalDays));
                    System.out.printf("%s (%s원/일)", optionName, context.formatMoney(optionDaily));
                }
                System.out.println();
            } else {
                System.out.println("옵션: 없음");
            }
            System.out.println();
            
            // 간단한 계산식 출력
            String policyPercent = "";
            if (feeStrategy instanceof PeakSeasonFeeStrategy) {
                policyPercent = " × 120%";
            } else if (feeStrategy instanceof OffSeasonFeeStrategy) {
                policyPercent = " × 90%";
            }
            
            System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                context.formatMoney(dailyFee), rentalDays, policyPercent,
                context.formatMoney(totalOptionCost), context.formatMoney(totalFee));
            System.out.println("-------------------\n");
            
            domain.rental.RentalRecord record = context.getRentalService().rent(currentId, selectedCar, rentalDays, options, feeStrategy);
            
            // 대여 시 생성된 RentalRecord를 메모리에 저장 (반납 시 정보 유지)
            domain.rental.RentalRecord recordCopy = new domain.rental.RentalRecord();
            recordCopy.setId(record.getId());
            recordCopy.setUserId(record.getUserId());
            recordCopy.setCarId(record.getCarId());
            recordCopy.setRentalDays(record.getRentalDays());
            recordCopy.setStartAt(record.getStartAt());
            recordCopy.setEndAt(record.getEndAt());
            recordCopy.setStatus(record.getStatus());
            recordCopy.setFeeStrategyType(record.getFeeStrategyType());
            recordCopy.setMembershipStrategyType(record.getMembershipStrategyType());
            recordCopy.setOptions(new ArrayList<>(record.getOptions()));
            recordCopy.setBaseFee(record.getBaseFee());
            recordCopy.setOptionFee(record.getOptionFee());
            recordCopy.setDiscount(record.getDiscount());
            recordCopy.setPenalty(record.getPenalty());
            recordCopy.setTotalFee(record.getTotalFee());
            
            context.getRentalRecordCache().put(record.getId(), recordCopy);
            
            // 차량 상태를 DB에 업데이트
            selectedCar.occupy();
            context.getCarRepository().update(selectedCar);
            System.out.println("✅ 차량 대여가 완료되었습니다!");
            System.out.println("대여 ID: " + record.getId());
        } catch (NumberFormatException e) {
            System.err.println("❌ 올바른 숫자를 입력해주세요.");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ 대여 실패: " + e.getMessage());
        }
    }
}

