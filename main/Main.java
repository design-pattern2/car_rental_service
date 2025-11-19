package main;

import db.DBConnection;
import db.EnvLoader;
import domain.user.User;
import domain.user.UserRepository;
import domain.user.UserService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

public class Main1 {
    private static User loggedInUser = null;

    public static void main(String[] args) {
        EnvLoader.load();

        System.out.println(" 차량 렌트 시스템 시뮬레이션을 시작합니다. ");

        try (Connection conn = DBConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {
            System.out.println(" DB 연결에 성공했습니다! 스키마: " + conn.getCatalog());
            UserService us = new UserService(new UserRepository(new DBConnection()));
            startSimulation(us, scanner);

        } catch (SQLException e) {
            System.err.println(" DB 연결에 실패했습니다! 오류: " + e.getMessage());
        }

    }
    private static void startSimulation(UserService us, Scanner scanner) {
        while (true) {
            // ⭐️ 로그인 상태에 따라 다른 메뉴를 보여줍니다.
            if (loggedInUser == null) {
                displayPreLoginMenu();
            } else {
                displayPostLoginMenu();
            }

            try {
                System.out.print("\n> 메뉴 선택: ");
                String input = scanner.nextLine();
                int menu = Integer.parseInt(input);

                if (menu == 0) {
                    System.out.println("\n👋 렌터카 시스템 시뮬레이션을 종료합니다.");
                    break;
                }

                // ⭐️ 로그인 상태에 따라 다른 실행 로직을 호출합니다.
                if (loggedInUser == null) {
                    executePreLoginMenu(menu, us, scanner);
                } else {
                    executePostLoginMenu(menu, us, scanner);
                }

            } catch (NumberFormatException e) {
                System.err.println("\n🚨 [오류] 숫자를 입력해주세요.");
            } catch (IllegalArgumentException e) {
                System.err.println("\n🚨 [오류] " + e.getMessage());
            } catch (Exception e) {
                System.err.println("\n🚨 [오류] 예상치 못한 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    private static void displayPreLoginMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("        🚗 [비회원] 회원 관리 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 회원가입 ");
        System.out.println(" 2. 로그인 (login)");
        System.out.println(" 3. 비밀번호 찾기");
        System.out.println(" 0. 종료");
        System.out.println("-".repeat(40));
    }

    private static void displayPostLoginMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("   👤 [" + loggedInUser.getUserId() + "님] 회원 관리 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 정보 조회 ");
        System.out.println(" 2. 정보 수정 ");
        System.out.println(" 3. 카드 등록 ");
        System.out.println(" 4. 회원 탈퇴 ");
        System.out.println(" 9. 로그아웃 "); // ⭐️ 로그아웃 추가
        System.out.println(" 0. 종료");
        System.out.println("-".repeat(40));
    }

    private static void executePreLoginMenu(int menu, UserService us, Scanner scanner) {
        String id, pw;

        switch (menu) {
            case 1: // 회원가입
                System.out.println("\n[1. 회원가입]");
                System.out.print("ID: "); id = scanner.nextLine();
                System.out.print("Password: "); pw = scanner.nextLine();
                System.out.print("이름: "); String name = scanner.nextLine();
                System.out.print("전화번호 (010...): "); String phone = scanner.nextLine();
                us.signUp(id, pw, name, phone);
                System.out.println("✅ 회원가입이 완료되었습니다! (" + id + ")");
                break;

            case 2: // 로그인 ⭐️
                System.out.println("\n[2. 로그인]");
                System.out.print("ID: "); id = scanner.nextLine();
                System.out.print("Password: "); pw = scanner.nextLine();

                Optional<User> userOpt = us.login(id, pw);
                if (userOpt.isPresent()) {
                    loggedInUser = userOpt.get(); // ⭐️ 로그인 성공 시 User 객체 저장
                    System.out.println("✅ 로그인 성공! 환영합니다, " + loggedInUser.getName() + "님.");
                } else {
                    System.err.println("❌ 로그인 실패! ID 또는 비밀번호가 올바르지 않습니다.");
                }
                break;

            case 3: // 비밀번호 찾기 (-> 새 비밀번호로 즉시 변경)
                System.out.println("\n[3. 비밀번호 재설정]");
                System.out.print("재설정할 ID: "); id = scanner.nextLine();
                System.out.print("새 Password: "); pw = scanner.nextLine();

                // 💡 UserService에  resetPassword(id, newRawPassword) 메서드 사용
                User resetUser = us.resetPassword(id, pw);
                System.out.println("✅ 비밀번호 재설정이 완료되었습니다! (" + resetUser.getUserId() + "님)");
                System.out.println("새 비밀번호로 다시 로그인해주세요.");
                break;

            default:
                System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                break;
        }
    }
    private static void executePostLoginMenu(int menu, UserService us, Scanner scanner) {
        String currentId = loggedInUser.getUserId(); // ⭐️ 로그인된 ID 사용

        switch (menu) {
            case 1: // 정보 조회
                System.out.println("\n[1. 정보 조회]");
                Optional<User> userOpt = us.getUserInfo(currentId);
                userOpt.ifPresentOrElse(
                        user -> {
                            // ⭐️ 업데이트된 정보를 반영하기 위해 loggedInUser 갱신
                            loggedInUser = user;
                            System.out.println("✅ 사용자 정보 조회 성공:");
                            System.out.println(user);
                        },
                        () -> System.err.println("❌ 사용자 정보를 찾을 수 없습니다. (내부 오류)")
                );
                break;

            case 2: // 정보 수정
                System.out.println("\n[2. 정보 수정]");
                System.out.println("⚠️ 변경하지 않을 항목은 Enter만 누르세요.");
                System.out.print("새 이름: "); String name = scanner.nextLine();
                System.out.print("새 Password: "); String pw = scanner.nextLine();
                System.out.print("새 전화번호 (010...): "); String phone = scanner.nextLine();

                name = name.trim().isEmpty() ? null : name.trim();
                pw = pw.trim().isEmpty() ? null : pw.trim();
                phone = phone.trim().isEmpty() ? null : phone.trim();

                User updatedUser = us.updateUserInfo(currentId, name, pw, phone);
                loggedInUser = updatedUser; // ⭐️ 수정된 객체로 갱신
                System.out.println("✅ 사용자 정보 수정 완료!");
                break;

            case 3: // 카드 등록
                System.out.println("\n[3. 카드 등록]");
                System.out.print("카드 번호: "); String cardNum = scanner.nextLine();
                User userWithCard = us.registerCard(currentId, cardNum);
                loggedInUser = userWithCard; // ⭐️ 갱신
                System.out.println("✅ 카드 등록이 완료되었습니다.");
                break;

            case 4: // 회원 탈퇴
                System.out.println("\n[4. 회원 탈퇴]");
                us.withdraw(currentId);
                System.out.println("✅ 회원 탈퇴가 완료되었습니다. (" + currentId + ")");
                loggedInUser = null; // ⭐️ 탈퇴 후 로그아웃 처리
                break;

            case 9: // 로그아웃 ⭐️
                System.out.println("\n🚪 로그아웃 되었습니다.");
                loggedInUser = null; // ⭐️ loggedInUser를 null로 설정하여 로그인 전 상태로 돌아감
                break;

            default:
                System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                break;
        }
    }
}