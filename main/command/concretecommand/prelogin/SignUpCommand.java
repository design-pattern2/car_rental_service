package main.command.concretecommand.prelogin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 회원가입 명령
 */
public class SignUpCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public SignUpCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[1. 회원가입]");
        System.out.print("ID: ");
        String id = scanner.nextLine();
        System.out.print("Password: ");
        String pw = scanner.nextLine();
        System.out.print("이름: ");
        String name = scanner.nextLine();
        System.out.print("전화번호 (010...): ");
        String phone = scanner.nextLine();
        
        context.getUserService().signUp(id, pw, name, phone);
        System.out.println("✅ " + name + "님 회원가입이 완료되었습니다!");
    }
}

