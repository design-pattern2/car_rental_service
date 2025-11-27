package main.command.concretecommand.user;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.util.Optional;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 정보 조회 명령 (일반 사용자/관리자 공통)
 */
public class ViewInfoCommand implements Command {
    private final ApplicationContext context;
    private final boolean isAdmin;
    
    public ViewInfoCommand(ApplicationContext context, Scanner scanner, boolean isAdmin) {
        this.context = context;
        this.isAdmin = isAdmin;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[1. 정보 조회]");
        String currentId = context.getLoggedInUser().getUserId();
        Optional<domain.user.User> userOpt = context.getUserService().getUserInfo(currentId);
        userOpt.ifPresentOrElse(
            user -> {
                context.setLoggedInUser(user);
                System.out.println("✅ " + user.getName() + "님의 정보는 다음과 같습니다.");
                System.out.println("ID: " + user.getUserId());
                System.out.println("이름: " + user.getName());
                System.out.println("전화번호: " + user.getPhoneNumber());
                String membership = user.getMembership();
                String gradeDisplay = (membership != null && membership.equals("ADMIN"))
                    ? "ADMIN"
                    : user.getUserMembershipStrategy().name().replace("Strategy", "").toUpperCase();
                System.out.println("등급: " + gradeDisplay);
            },
            () -> System.err.println("❌ 사용자 정보를 찾을 수 없습니다. (내부 오류)")
        );
    }
}

