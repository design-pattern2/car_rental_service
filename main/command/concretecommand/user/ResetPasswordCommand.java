package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 비밀번호 재설정 명령
 */
public class ResetPasswordCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public ResetPasswordCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[3. 비밀번호 재설정]");
        System.out.print("새 비밀번호: ");
        String newPassword = scanner.nextLine();
        if (newPassword.trim().isEmpty()) {
            System.err.println("❌ 비밀번호는 비워둘 수 없습니다.");
            return;
        }
        
        String currentId = context.getLoggedInUser().getUserId();
        domain.user.User resetUser = context.getUserService().resetPassword(currentId, newPassword);
        context.setLoggedInUser(resetUser);
        System.out.println("✅ 비밀번호 재설정이 완료되었습니다!");
    }
}

