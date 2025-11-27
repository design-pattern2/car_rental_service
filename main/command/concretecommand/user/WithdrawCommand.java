package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 회원 탈퇴 명령
 */
public class WithdrawCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public WithdrawCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[5. 회원 탈퇴]");
        System.out.println("⚠️  정말 회원 탈퇴를 하시겠습니까? (yes/no)");
        System.out.print("선택: ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if ("yes".equals(confirm)) {
            String currentId = context.getLoggedInUser().getUserId();
            String userName = context.getLoggedInUser() != null ? context.getLoggedInUser().getName() : currentId;
            context.getUserService().withdraw(currentId);
            System.out.println("✅ " + userName + "님 회원 탈퇴가 완료되었습니다.");
            context.setLoggedInUser(null);
            context.setAdmin(false);
        } else if ("no".equals(confirm)) {
            System.out.println("❌ 회원 탈퇴가 취소되었습니다.");
        } else {
            System.err.println("❌ 'yes' 또는 'no'를 입력해주세요.");
        }
    }
}

