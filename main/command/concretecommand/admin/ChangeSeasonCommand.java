package main.command.concretecommand.admin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;
import domain.payment.strategy.BaseFeeStrategy;
import domain.payment.strategy.FeeStrategy;
import domain.payment.strategy.PeakSeasonFeeStrategy;
import domain.payment.strategy.OffSeasonFeeStrategy;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 시즌 변경 명령 (관리자 전용)
 */
public class ChangeSeasonCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public ChangeSeasonCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[7. 시즌 변경]");
        try {
            // 현재 시즌 표시
            FeeStrategy currentSeason = context.getCurrentSeason();
            String currentSeasonName = "";
            if (currentSeason instanceof PeakSeasonFeeStrategy) {
                currentSeasonName = "성수기 (20% 할증)";
            } else if (currentSeason instanceof OffSeasonFeeStrategy) {
                currentSeasonName = "비수기 (10% 할인)";
            } else {
                currentSeasonName = "기본";
            }
            
            System.out.println("현재 시즌: " + currentSeasonName);
            System.out.println("\n변경할 시즌 선택:");
            System.out.println("  1. 기본");
            System.out.println("  2. 성수기 (20% 할증)");
            System.out.println("  3. 비수기 (10% 할인)");
            System.out.print("선택: ");
            String seasonChoice = scanner.nextLine().trim();
            
            FeeStrategy newSeason = null;
            String newSeasonName = "";
            switch (seasonChoice) {
                case "1":
                    newSeason = new BaseFeeStrategy();
                    newSeasonName = "기본";
                    break;
                case "2":
                    newSeason = new PeakSeasonFeeStrategy();
                    newSeasonName = "성수기 (20% 할증)";
                    break;
                case "3":
                    newSeason = new OffSeasonFeeStrategy();
                    newSeasonName = "비수기 (10% 할인)";
                    break;
                default:
                    System.err.println("❌ 잘못된 선택입니다. (1-3 중 선택)");
                    return;
            }
            
            if (newSeason != null) {
                context.setCurrentSeason(newSeason);
                System.out.println("✅ 시즌이 '" + newSeasonName + "'로 변경되었습니다.");
                System.out.println("   (이후 모든 차량 대여에 적용됩니다)");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 시즌 변경 실패: " + e.getMessage());
        }
    }
}

