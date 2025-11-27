package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 로그아웃 명령
 */
public class LogoutCommand implements Command {
    private final ApplicationContext context;
    
    public LogoutCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
    }
    
    @Override
    public void execute() {
        System.out.println("\n🚪 로그아웃 되었습니다.");
        context.setLoggedInUser(null);
        context.setAdmin(false);
    }
}

