package main.command.concretecommand.admin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 차량 삭제 명령 (관리자 전용)
 */
public class DeleteCarCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public DeleteCarCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[5. 차량 삭제]");
        try {
            // 1) 모든 차량 목록 조회 및 표시
            List<Map<String, Object>> allCars = context.getAdminService().getAllCarsWithStatus();
            
            if (allCars.isEmpty()) {
                System.out.println("❌ 등록된 차량이 없습니다.");
                return;
            }
            
            System.out.println("\n현재 등록된 차량 목록:");
            System.out.println("-".repeat(60));
            for (int i = 0; i < allCars.size(); i++) {
                Map<String, Object> car = allCars.get(i);
                String carName = Objects.toString(car.get("name"), "");
                String carType = Objects.toString(car.get("type"), "");
                String carStatusDisplay = Objects.toString(car.get("status"), "");
                
                System.out.printf("%d. 이름: %s | 타입: %s | 상태: %s%n",
                    i + 1, carName, carType, carStatusDisplay);
            }
            System.out.println("-".repeat(60));
            
            // 2) 삭제할 차량 선택 (차량 이름 입력)
            System.out.print("삭제할 차량 이름 입력: ");
            String carNameToDelete = scanner.nextLine().trim();
            
            if (carNameToDelete.isEmpty()) {
                System.err.println("❌ 차량 이름을 입력해주세요.");
                return;
            }
            
            // 3) 차량 조회
            Optional<Map<String, Object>> carOpt = context.getAdminService().findCarByName(carNameToDelete);
            
            if (carOpt.isEmpty()) {
                System.err.println("❌ 해당 이름의 차량을 찾을 수 없습니다.");
                return;
            }
            
            Map<String, Object> carToDelete = carOpt.get();
            String carStatus = Objects.toString(carToDelete.get("status"), "");
            Object idObj = carToDelete.get("id");
            int carId = (idObj instanceof Number) ? ((Number) idObj).intValue() : 0;
            
            // 4) 상태 확인
            if ("UNAVAILABLE".equalsIgnoreCase(carStatus)) {
                System.err.println("❌ 현재 렌트중인 차는 삭제할 수 없습니다.");
                return;
            }
            
            // 5) 삭제 확인
            System.out.println("⚠️  정말 삭제하시겠습니까? (yes/no)");
            System.out.print("선택: ");
            String confirm = scanner.nextLine().trim().toLowerCase();
            
            if ("yes".equals(confirm)) {
                try {
                    boolean deleted = context.getAdminService().deleteCarById(carId);
                    if (deleted) {
                        System.out.println("✅ 차량 삭제가 완료되었습니다.");
                    } else {
                        System.err.println("❌ 차량 삭제에 실패했습니다.");
                    }
                } catch (IllegalStateException e) {
                    System.err.println("❌ " + e.getMessage());
                } catch (Exception deleteException) {
                    System.err.println("❌ 차량 삭제 실패: " + deleteException.getMessage());
                }
            } else if ("no".equals(confirm)) {
                System.out.println("❌ 차량 삭제가 취소되었습니다.");
            } else {
                System.err.println("❌ 'yes' 또는 'no'를 입력해주세요.");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 차량 삭제 실패: " + e.getMessage());
        }
    }
}

