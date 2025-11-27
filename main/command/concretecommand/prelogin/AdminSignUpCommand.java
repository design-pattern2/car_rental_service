package main.command.concretecommand.prelogin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 관리자 회원가입 명령
 */
public class AdminSignUpCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public AdminSignUpCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[2. 관리자 회원가입]");
        System.out.print("ID: ");
        String id = scanner.nextLine();
        System.out.print("Password: ");
        String pw = scanner.nextLine();
        System.out.print("이름: ");
        String name = scanner.nextLine();
        System.out.print("전화번호 (010...): ");
        String phone = scanner.nextLine();
        System.out.print("관리자 암호: ");
        String adminPassword = scanner.nextLine();
        
        try {
            String envAdminPassword = System.getProperty("ADMIN_PASSWORD");
            if (envAdminPassword == null || envAdminPassword.trim().isEmpty()) {
                throw new IllegalArgumentException("환경 변수 ADMIN_PASSWORD가 설정되지 않았습니다.");
            }
            if (adminPassword == null || !adminPassword.equals(envAdminPassword)) {
                throw new IllegalArgumentException("관리자 암호가 올바르지 않습니다.");
            }
            
            domain.user.User adminUser = context.getUserService().signUpAdmin(id, pw, name, phone);
            System.out.println("✅ " + adminUser.getName() + "님 관리자 회원가입이 완료되었습니다!");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ " + e.getMessage());
        } catch (IllegalStateException e) {
            System.err.println("❌ " + e.getMessage());
        }
    }
}

