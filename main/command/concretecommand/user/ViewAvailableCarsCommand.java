package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Command Pattern: ConcreteCommand
 * 빌릴 수 있는 차량 조회 명령
 */
public class ViewAvailableCarsCommand implements Command {
    private final ApplicationContext context;
    
    public ViewAvailableCarsCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[6. 빌릴 수 있는 차량 조회]");
        List<domain.car.Car> allCars = context.getCarRepository().findAllCars();
        List<domain.car.Car> availableCars = allCars.stream()
            .filter(car -> car.status() == domain.car.carFactory.CarStatus.AVAILABLE)
            .collect(Collectors.toList());
        
        if (availableCars.isEmpty()) {
            System.out.println("❌ 현재 대여 가능한 차량이 없습니다.");
        } else {
            System.out.println("✅ 대여 가능한 차량 목록:");
            System.out.println("-".repeat(60));
            for (int i = 0; i < availableCars.size(); i++) {
                domain.car.Car car = availableCars.get(i);
                java.math.BigDecimal fee = car.getDailyRentalFee() != null ? car.getDailyRentalFee() : car.type().baseRate();
                System.out.printf("%d. [%s] %s | 일일 요금: %s원%n",
                    i + 1, car.type(), car.getName(), context.formatMoney(fee));
            }
            System.out.println("-".repeat(60));
        }
    }
}

