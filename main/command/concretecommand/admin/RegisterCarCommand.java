package main.command.concretecommand.admin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;
import domain.car.carFactory.CarType;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 차량 등록 명령 (관리자 전용)
 */
public class RegisterCarCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public RegisterCarCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[4. 차량 등록]");
        try {
            // 1) 차량 타입 선택
            System.out.println("차량 타입 선택:");
            System.out.println("  1. SEDAN");
            System.out.println("  2. SUV");
            System.out.println("  3. BIKE");
            System.out.print("선택: ");
            String typeInput = scanner.nextLine().trim();
            int typeChoice;
            try {
                typeChoice = Integer.parseInt(typeInput);
            } catch (NumberFormatException e) {
                System.err.println("❌ 숫자를 입력해주세요.");
                return;
            }
            
            CarType type = switch (typeChoice) {
                case 1 -> CarType.SEDAN;
                case 2 -> CarType.SUV;
                case 3 -> CarType.BIKE;
                default -> {
                    System.err.println("❌ 잘못된 타입 선택입니다. (1-3 중 선택)");
                    yield null;
                }
            };
            
            if (type == null) {
                return;
            }
            
            // 2) 차량 이름 입력
            System.out.print("차량 이름: ");
            String carName = scanner.nextLine().trim();
            if (carName.isEmpty()) {
                System.err.println("❌ 차량 이름은 필수입니다.");
                return;
            }
            
            // 3) 일일 대여료 입력 (필수)
            System.out.print("일일 대여료: ");
            String feeInput = scanner.nextLine().trim();
            if (feeInput.isEmpty()) {
                System.err.println("❌ 일일 대여료는 필수입니다.");
                return;
            }
            
            BigDecimal dailyRentalFee;
            try {
                dailyRentalFee = new BigDecimal(feeInput);
                if (dailyRentalFee.compareTo(BigDecimal.ZERO) <= 0) {
                    System.err.println("❌ 일일 대여료는 0보다 커야 합니다.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("❌ 올바른 숫자를 입력해주세요.");
                return;
            }
            
            // 4) 차량 등록
            context.getAdminService().addCar(type, dailyRentalFee, carName);
            System.out.println("✅ 차량 등록이 완료되었습니다.");
            
        } catch (Exception e) {
            System.err.println("❌ 차량 등록 실패: " + e.getMessage());
        }
    }
}

