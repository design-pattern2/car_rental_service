package main.command.concretecommand.prelogin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Optional;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 로그인 명령
 */
public class LoginCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public LoginCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[3. 로그인]");
        System.out.print("ID: ");
        String id = scanner.nextLine();
        System.out.print("Password: ");
        String pw = scanner.nextLine();
        
        Optional<domain.user.User> userOpt = context.getUserService().login(id, pw);
        if (userOpt.isPresent()) {
            domain.user.User user = userOpt.get();
            context.setLoggedInUser(user);
            
            if (context.isAdmin()) {
                System.out.println("✅ 관리자로 로그인되었습니다! 환영합니다, " + user.getName() + "님.");
            } else {
                System.out.println("✅ 로그인 성공! 환영합니다, " + user.getName() + "님.");
            }
        } else {
            System.err.println("❌ 로그인 실패! ID 또는 비밀번호가 올바르지 않습니다.");
        }
    }
}

