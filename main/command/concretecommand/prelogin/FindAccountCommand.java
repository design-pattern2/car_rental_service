package main.command.concretecommand.prelogin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;
import domain.user.User;

import java.util.Optional;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 회원정보 찾기 명령 (ID 찾기, 비밀번호 찾기)
 */
public class FindAccountCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public FindAccountCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[4. 회원정보 찾기]");
        System.out.println(" 1. ID 찾기");
        System.out.println(" 2. 비밀번호 찾기");
        System.out.print("선택: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                findUserId();
                break;
            case "2":
                findPassword();
                break;
            default:
                System.err.println("❌ 유효하지 않은 선택입니다.");
        }
    }
    
    /**
     * ID 찾기: 이름을 입력받아 해당 이름의 ID를 찾아 표시
     */
    private void findUserId() {
        System.out.println("\n[ID 찾기]");
        System.out.print("이름: ");
        String name = scanner.nextLine();
        
        if (name.trim().isEmpty()) {
            System.err.println("❌ 이름을 입력해주세요.");
            return;
        }
        
        // UserService를 통해 이름으로 사용자 찾기
        Optional<User> userOpt = context.getUserService().findUserByName(name);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            System.out.println("✅ " + user.getName() + "님의 ID는 다음과 같습니다: " + user.getUserId());
        } else {
            System.out.println("❌ 없는 정보입니다.");
        }
    }
    
    /**
     * 비밀번호 찾기: ID를 입력받아 해당 ID가 있으면 비밀번호 재설정
     */
    private void findPassword() {
        System.out.println("\n[비밀번호 찾기]");
        System.out.print("ID: ");
        String userId = scanner.nextLine();
        
        if (userId.trim().isEmpty()) {
            System.err.println("❌ ID를 입력해주세요.");
            return;
        }
        
        // UserService를 통해 ID로 사용자 찾기
        Optional<User> userOpt = context.getUserService().getUserInfo(userId);
        
        if (userOpt.isEmpty()) {
            System.out.println("❌ 없는 정보입니다.");
            return;
        }
        
        // ID가 존재하면 비밀번호 재설정
        System.out.print("새 비밀번호: ");
        String newPassword = scanner.nextLine();
        
        if (newPassword.trim().isEmpty()) {
            System.err.println("❌ 비밀번호는 비워둘 수 없습니다.");
            return;
        }
        
        try {
            context.getUserService().resetPassword(userId, newPassword);
            System.out.println("✅ 비밀번호 재설정이 완료되었습니다!");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ " + e.getMessage());
        }
    }
}

