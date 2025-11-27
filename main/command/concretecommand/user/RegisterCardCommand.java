package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 카드 등록 명령
 */
public class RegisterCardCommand implements Command {
    private final ApplicationContext context;
    private final Scanner scanner;
    
    public RegisterCardCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
        this.scanner = scanner;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[4. 카드 등록]");
        System.out.print("카드 번호: ");
        String cardNum = scanner.nextLine();
        String currentId = context.getLoggedInUser().getUserId();
        domain.user.User userWithCard = context.getUserService().registerCard(currentId, cardNum);
        context.setLoggedInUser(userWithCard);
        System.out.println("✅ 카드 등록이 완료되었습니다.");
    }
}

