package main;

import db.DBConnection;
import db.EnvLoader;
import domain.admin.AdminService;
import domain.car.CarRepository;
import domain.rental.RentalService;
import domain.user.UserRepository;
import domain.user.UserService;
import main.command.command.Command;
import main.command.invoker.Invoker;
import main.command.receiver.ApplicationContext;
import main.command.concretecommand.prelogin.SignUpCommand;
import main.command.concretecommand.prelogin.AdminSignUpCommand;
import main.command.concretecommand.prelogin.LoginCommand;
import main.command.concretecommand.prelogin.FindAccountCommand;
import main.command.concretecommand.user.*;
import main.command.concretecommand.admin.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Command Pattern: Client
 * 애플리케이션의 진입점이며, Command 객체들을 생성하고 Invoker를 통해 실행합니다.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("\n⏳ 차량 렌트 시스템 시뮬레이션 시작 중...");
        
        EnvLoader.load();

        try (Connection conn = DBConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("✅ DB 연결 완료");
            
            DBConnection db = DBConnection.getInstance();
            UserService userService = new UserService(new UserRepository(db));
            AdminService adminService = new AdminService(db, userService);
            CarRepository carRepository = new CarRepository(db);
            RentalService rentalService = new RentalService(new domain.rental.RentalRepository(db), userService);
            
            // ApplicationContext 생성 (Receiver)
            ApplicationContext context = new ApplicationContext(
                userService, adminService, carRepository, rentalService
            );
            
            System.out.println("✅ 시뮬레이션 시작 완료");
            
            // 움직이는 모션 효과
            animateWelcomeMessage();
            
            startSimulation(context, scanner);

        } catch (SQLException e) {
            System.err.println("\n❌ DB 연결에 실패했습니다! 오류: " + e.getMessage());
        }
    }
    
    /**
     * 환영 메시지 애니메이션 효과
     */
    private static void animateWelcomeMessage() {
        try {
            System.out.println();
            String[] dots = {".", "..", "..."};
            
            // 점이 깜빡이는 효과
            for (int i = 0; i < 2; i++) {
                for (String dot : dots) {
                    System.out.print("\r   로딩 중" + dot + "   ");
                    Thread.sleep(200);
                }
            }
            
            System.out.println("\r" + " ".repeat(20)); // 이전 텍스트 지우기
            
            // 환영 메시지 타이핑 효과
            System.out.println("=".repeat(50));
            
            String message1 = "   어서오세요 차량 렌트 시스템입니다.";
            typeWriter(message1, 100);
            System.out.println();
            
            Thread.sleep(300);
            
            String message2 = "   먼저 회원 인증을 해주세요!";
            typeWriter(message2, 100);
            System.out.println();
            
            System.out.println("=".repeat(50));
            
        } catch (InterruptedException e) {
            // 인터럽트 발생 시 그냥 메시지만 표시
            System.out.println("\n" + "=".repeat(50));
            System.out.println("   어서오세요 차량 렌트 시스템입니다.");
            System.out.println("   먼저 회원 인증을 해주세요!");
            System.out.println("=".repeat(50));
        }
    }
    
    /**
     * 타이핑 효과로 텍스트를 한 글자씩 출력
     */
    private static void typeWriter(String text, int delay) {
        try {
            for (char c : text.toCharArray()) {
                System.out.print(c);
                // 개행 문자는 딜레이를 더 줄여서 빠르게 표시
                if (c == '\n') {
                    Thread.sleep(delay / 3);
                } else {
                    Thread.sleep(delay);
                }
            }
        } catch (InterruptedException e) {
            // 인터럽트 발생 시 전체 텍스트 출력
            System.out.print(text);
        }
    }
    
    /**
     * 시뮬레이션 메인 루프
     */
    private static void startSimulation(ApplicationContext context, Scanner scanner) {
        Invoker invoker = new Invoker();
        
        while (true) {
            // 로그인 상태에 따라 다른 메뉴를 보여줍니다.
            if (context.getLoggedInUser() == null) {
                displayPreLoginMenu();
            } else {
                displayPostLoginMenu(context);
            }

            try {
                System.out.print("\n> 메뉴 선택: ");
                String input = scanner.nextLine();
                int menu = Integer.parseInt(input);

                if (menu == 0) {
                    System.out.println("\n👋 렌터카 시스템 시뮬레이션을 종료합니다.");
                    break;
                }

                // Command 생성 및 실행
                Command command = createCommand(menu, context, scanner);
                if (command != null) {
                    invoker.setCommand(command);
                    invoker.executeCommand();
                } else {
                    System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                }

            } catch (NumberFormatException e) {
                System.err.println("\n🚨 [오류] 숫자를 입력해주세요.");
            } catch (IllegalArgumentException e) {
                System.err.println("\n🚨 [오류] " + e.getMessage());
            } catch (Exception e) {
                System.err.println("\n🚨 [오류] 예상치 못한 오류가 발생했습니다: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 메뉴 번호에 따라 적절한 Command를 생성합니다.
     */
    private static Command createCommand(int menu, ApplicationContext context, Scanner scanner) {
        if (context.getLoggedInUser() == null) {
            // Pre-login 메뉴
            return switch (menu) {
                case 1 -> new SignUpCommand(context, scanner);
                case 2 -> new AdminSignUpCommand(context, scanner);
                case 3 -> new LoginCommand(context, scanner);
                case 4 -> new FindAccountCommand(context, scanner);
                default -> null;
            };
        } else {
            // Post-login 메뉴
            boolean isAdmin = context.isAdmin();
            if (isAdmin) {
                // 관리자 메뉴
                return switch (menu) {
                    case 1 -> new ViewInfoCommand(context, scanner, true);
                    case 2 -> new UpdateInfoCommand(context, scanner);
                    case 3 -> new RegisterCarCommand(context, scanner);
                    case 4 -> new DeleteCarCommand(context, scanner);
                    case 5 -> new ViewRentalRecordsCommand(context, scanner);
                    case 6 -> new ChangeSeasonCommand(context, scanner);
                    case 7 -> new LogoutCommand(context, scanner);
                    default -> null;
                };
            } else {
                // 일반 사용자 메뉴
                return switch (menu) {
                    case 1 -> new ViewInfoCommand(context, scanner, false);
                    case 2 -> new UpdateInfoCommand(context, scanner);
                    case 3 -> new RegisterCardCommand(context, scanner);
                    case 4 -> new WithdrawCommand(context, scanner);
                    case 5 -> new ViewAvailableCarsCommand(context, scanner);
                    case 6 -> new RentCarCommand(context, scanner);
                    case 7 -> new ReturnCarCommand(context, scanner);
                    case 8 -> new ViewPaymentCommand(context, scanner);
                    case 9 -> new LogoutCommand(context, scanner);
                    default -> null;
                };
            }
        }
    }

    private static void displayPreLoginMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("🚗 [비회원] 회원 관리 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 회원가입 ");
        System.out.println(" 2. 관리자 회원가입 ");
        System.out.println(" 3. 로그인 (login)");
        System.out.println(" 4. 회원정보 찾기");
        System.out.println(" 0. 종료");
        System.out.println("-".repeat(40));
    }
    
    private static void displayPostLoginMenu(ApplicationContext context) {
        System.out.println("\n" + "-".repeat(40));
        String role = context.isAdmin() ? "관리자" : "회원";
        System.out.println("👤 [" + context.getLoggedInUser().getName() + "님] 회원 관리 시스템 메뉴 [" + role + "]");
        System.out.println("-".repeat(40));
        
        if (context.isAdmin()) {
            // 관리자 메뉴
            System.out.println(" 1. 정보 조회 ");
            System.out.println(" 2. 정보 수정 ");
            System.out.println(" 3. 차량 등록 ");
            System.out.println(" 4. 차량 삭제 ");
            System.out.println(" 5. 대여 기록 조회 ");
            System.out.println(" 6. 시즌 변경 ");
            System.out.println(" 7. 로그아웃 ");
            System.out.println(" 0. 종료");
        } else {
            // 일반 사용자 메뉴
            System.out.println(" 1. 정보 조회 ");
            System.out.println(" 2. 정보 수정 ");
            System.out.println(" 3. 카드 등록 ");
            System.out.println(" 4. 회원 탈퇴 ");
            System.out.println(" 5. 빌릴 수 있는 차량 조회 ");
            System.out.println(" 6. 차량 대여 ");
            System.out.println(" 7. 차량 반납 ");
            System.out.println(" 8. 결제 금액 확인 ");
            System.out.println(" 9. 로그아웃 ");
            System.out.println(" 0. 종료");
        }
        System.out.println("-".repeat(40));
    }
}
