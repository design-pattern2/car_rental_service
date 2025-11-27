package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 정보 수정 명령
 */
public class UpdateInfoCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public UpdateInfoCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[2. 정보 수정]");
        System.out.println("⚠️ 변경하지 않을 항목은 Enter만 누르세요.");
        System.out.print("새 이름: ");
        String name = scanner.nextLine();
        System.out.print("새 Password: ");
        String pw = scanner.nextLine();
        System.out.print("새 전화번호 (010...): ");
        String phone = scanner.nextLine();
        
        name = name.trim().isEmpty() ? null : name.trim();
        pw = pw.trim().isEmpty() ? null : pw.trim();
        phone = phone.trim().isEmpty() ? null : phone.trim();
        
        String currentId = context.getLoggedInUser().getUserId();
        domain.user.User updatedUser = context.getUserService().updateUserInfo(currentId, name, pw, phone);
        context.setLoggedInUser(updatedUser);
        System.out.println("✅ 사용자 정보 수정 완료!");
    }
}

