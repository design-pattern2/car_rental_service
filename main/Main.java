package main;

import db.DBConnection;
import db.EnvLoader;
import domain.admin.AdminService;
import domain.car.Car;
import domain.car.CarRepository;
import domain.car.carFactory.CarType;
import domain.payment.PaymentService;
import domain.payment.strategy.BaseFeeStrategy;
import domain.payment.strategy.FeeStrategy;
import domain.payment.strategy.PeakSeasonFeeStrategy;
import domain.payment.strategy.OffSeasonFeeStrategy;
import domain.rental.RentalRecord;
import domain.rental.RentalRepository;
import domain.rental.RentalService;
import domain.user.User;
import domain.user.UserRepository;
import domain.user.UserService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static User loggedInUser = null;
    private static boolean isAdmin = false;
    // 대여 시 생성된 RentalRecord를 메모리에 저장 (반납 시 정보 유지)
    private static Map<Long, RentalRecord> rentalRecordCache = new HashMap<>();
    // 현재 시즌 (관리자가 변경 가능, 기본값: BaseFeeStrategy)
    private static FeeStrategy currentSeason = new BaseFeeStrategy();
    
    /**
     * BigDecimal을 정수 문자열로 변환 (소수점 제거)
     */
    private static String formatMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.valueOf(amount.setScale(0, java.math.RoundingMode.HALF_UP).intValue());
    }

    public static void main(String[] args) {
        System.out.println("\n⏳ 차량 렌트 시스템 시뮬레이션 시작 중...");
        
        EnvLoader.load();

        try (Connection conn = DBConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("✅ DB 연결 완료");
            
            DBConnection db = new DBConnection();
            UserService us = new UserService(new UserRepository(db));
            AdminService adminService = new AdminService(db, us);
            CarRepository carRepository = new CarRepository(db);
            RentalService rentalService = new RentalService(new RentalRepository(db), us);
            PaymentService paymentService = new PaymentService(us);
            
            System.out.println("✅ 시뮬레이션 시작 완료");
            
            // 움직이는 모션 효과
            animateWelcomeMessage();
            
            startSimulation(us, adminService, carRepository, rentalService, paymentService, scanner);

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
     * 여러 줄을 타이핑 효과로 출력
     */
    private static void typeWriterLines(String[] lines, int delay) {
        try {
            for (String line : lines) {
                typeWriter(line, delay);
                System.out.println();
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            for (String line : lines) {
                System.out.println(line);
            }
        }
    }
    
    private static void startSimulation(UserService us, AdminService adminService, 
                                       CarRepository carRepository, RentalService rentalService,
                                       PaymentService paymentService, Scanner scanner) {
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
                    executePreLoginMenu(menu, us, adminService, scanner);
                } else {
                    executePostLoginMenu(menu, us, adminService, carRepository, rentalService, paymentService, scanner);
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

    private static void displayPreLoginMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("🚗 [비회원] 회원 관리 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 회원가입 ");
        System.out.println(" 2. 관리자 회원가입 ");
        System.out.println(" 3. 로그인 (login)");
        System.out.println(" 0. 종료");
        System.out.println("-".repeat(40));
    }
    
    private static void displayAdminMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("👤 [관리자] 관리자 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 차량 등록 ");
        System.out.println(" 2. 차량 삭제 ");
        System.out.println(" 3. 전체 차량 조회 ");
        System.out.println(" 4. 전체 대여 이력 조회 ");
        System.out.println(" 9. 로그아웃 ");
        System.out.println(" 0. 종료");
        System.out.println("-".repeat(40));
    }

    private static void displayPostLoginMenu() {
        System.out.println("\n" + "-".repeat(40));
        String role = isAdmin ? "관리자" : "회원";
        System.out.println("👤 [" + loggedInUser.getName() + "님] 회원 관리 시스템 메뉴 [" + role + "]");
        System.out.println("-".repeat(40));
        
        if (isAdmin) {
            // 관리자 메뉴
            System.out.println(" 1. 정보 조회 ");
            System.out.println(" 2. 정보 수정 ");
            System.out.println(" 3. 비밀번호 재설정 ");
            System.out.println(" 4. 차량 등록 ");
            System.out.println(" 5. 차량 삭제 ");
            System.out.println(" 6. 대여 기록 조회 ");
            System.out.println(" 7. 시즌 변경 ");
            System.out.println(" 8. 로그아웃 ");
            System.out.println(" 0. 종료");
        } else {
            // 일반 사용자 메뉴
            System.out.println(" 1. 정보 조회 ");
            System.out.println(" 2. 정보 수정 ");
            System.out.println(" 3. 비밀번호 재설정 ");
            System.out.println(" 4. 카드 등록 ");
            System.out.println(" 5. 회원 탈퇴 ");
            System.out.println(" 6. 빌릴 수 있는 차량 조회 ");
            System.out.println(" 7. 차량 대여 ");
            System.out.println(" 8. 차량 반납 ");
            System.out.println(" 9. 결제 금액 확인 ");
            System.out.println(" 10. 로그아웃 ");
            System.out.println(" 0. 종료");
        }
        System.out.println("-".repeat(40));
    }

    private static void executePreLoginMenu(int menu, UserService us, AdminService adminService, Scanner scanner) {
        String id, pw;

        switch (menu) {
            case 1: // 회원가입
                System.out.println("\n[1. 회원가입]");
                System.out.print("ID: "); id = scanner.nextLine();
                System.out.print("Password: "); pw = scanner.nextLine();
                System.out.print("이름: "); String name = scanner.nextLine();
                System.out.print("전화번호 (010...): "); String phone = scanner.nextLine();
                us.signUp(id, pw, name, phone);
                System.out.println("✅ " + name + "님 회원가입이 완료되었습니다!");
                break;

            case 2: // 관리자 회원가입
                System.out.println("\n[2. 관리자 회원가입]");
                System.out.print("ID: "); id = scanner.nextLine();
                System.out.print("Password: "); pw = scanner.nextLine();
                System.out.print("이름: "); name = scanner.nextLine();
                System.out.print("전화번호 (010...): "); phone = scanner.nextLine();
                System.out.print("관리자 암호: "); String adminPassword = scanner.nextLine();
                
                try {
                    // 관리자 암호 확인: env에서 읽어온 값과 비교
                    String envAdminPassword = System.getProperty("ADMIN_PASSWORD");
                    if (envAdminPassword == null || envAdminPassword.trim().isEmpty()) {
                        throw new IllegalArgumentException("환경 변수 ADMIN_PASSWORD가 설정되지 않았습니다.");
                    }
                    if (adminPassword == null || !adminPassword.equals(envAdminPassword)) {
                        throw new IllegalArgumentException("관리자 암호가 올바르지 않습니다.");
                    }
                    // 관리자 회원가입: membership을 "ADMIN"으로 설정
                    User adminUser = us.signUpAdmin(id, pw, name, phone);
                    System.out.println("✅ " + adminUser.getName() + "님 관리자 회원가입이 완료되었습니다!");
                } catch (IllegalArgumentException e) {
                    System.err.println("❌ " + e.getMessage());
                } catch (IllegalStateException e) {
                    System.err.println("❌ " + e.getMessage());
                }
                break;

            case 3: // 로그인 ⭐️ (일반/관리자 자동 구분)
                System.out.println("\n[3. 로그인]");
                System.out.print("ID: "); id = scanner.nextLine();
                System.out.print("Password: "); pw = scanner.nextLine();

                Optional<User> userOpt = us.login(id, pw);
                if (userOpt.isPresent()) {
                    loggedInUser = userOpt.get(); // ⭐️ 로그인 성공 시 User 객체 저장
                    
                    // 관리자 여부 자동 확인 (DB의 membership 컬럼이 "ADMIN"인지 확인)
                    String membership = loggedInUser.getMembership();
                    isAdmin = membership != null && membership.equals("ADMIN");
                    
                    if (isAdmin) {
                        System.out.println("✅ 관리자로 로그인되었습니다! 환영합니다, " + loggedInUser.getName() + "님.");
                    } else {
                        System.out.println("✅ 로그인 성공! 환영합니다, " + loggedInUser.getName() + "님.");
                    }
                } else {
                    System.err.println("❌ 로그인 실패! ID 또는 비밀번호가 올바르지 않습니다.");
                }
                break;

            default:
                System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                break;
        }
    }
    
    private static void executeAdminMenu(int menu, AdminService adminService, Scanner scanner) {
        switch (menu) {
            case 1: // 차량 등록
                System.out.println("\n[1. 차량 등록]");
                System.out.println("차량 타입 선택:");
                System.out.println("  1. SEDAN");
                System.out.println("  2. SUV");
                System.out.println("  3. BIKE");
                System.out.print("선택: "); 
                int typeChoice = Integer.parseInt(scanner.nextLine());
                CarType type = switch (typeChoice) {
                    case 1 -> CarType.SEDAN;
                    case 2 -> CarType.SUV;
                    case 3 -> CarType.BIKE;
                    default -> throw new IllegalArgumentException("잘못된 타입 선택");
                };
                System.out.print("차량 이름: ");
                String carName = scanner.nextLine().trim();
                if (carName.isEmpty()) {
                    System.err.println("❌ 차량 이름은 필수입니다.");
                    break;
                }
                System.out.print("일일 대여료 (엔터 시 기본 요금 사용): ");
                String feeInput = scanner.nextLine().trim();
                BigDecimal fee = feeInput.isEmpty() ? null : new BigDecimal(feeInput);
                
                // 차량 ID는 자동 생성되거나 이름을 기반으로 생성 (현재는 이름을 ID로 사용)
                // TODO: 차량 ID를 별도로 입력받도록 변경 가능
                adminService.addCar(carName, type, fee, carName);
                break;
                
            case 2: // 차량 삭제
                System.out.println("\n[2. 차량 삭제]");
                System.out.print("삭제할 차량 ID: "); 
                String deleteCarId = scanner.nextLine();
                // 이 메서드는 사용되지 않지만, 호환성을 위해 유지
                try {
                    int carIdInt = Integer.parseInt(deleteCarId);
                    boolean deleted = adminService.deleteCarById(carIdInt);
                    if (deleted) {
                        System.out.println("[관리자] 차량 삭제 완료 -> ID=" + deleteCarId);
                    } else {
                        System.out.println("[관리자] 삭제할 차량이 존재하지 않습니다 -> ID=" + deleteCarId);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("❌ 올바른 차량 ID를 입력해주세요.");
                }
                break;
                
            case 3: // 전체 차량 조회
                System.out.println("\n[3. 전체 차량 조회]");
                adminService.printAllCars();
                break;
                
            case 4: // 전체 대여 이력 조회
                System.out.println("\n[4. 전체 대여 이력 조회]");
                adminService.printAllRentalRecords();
                break;
                
            case 9: // 로그아웃
                System.out.println("\n🚪 로그아웃 되었습니다.");
                adminService.logout();
                loggedInUser = null;
                isAdmin = false;
                break;
                
            default:
                System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                break;
        }
    }
    private static void executePostLoginMenu(int menu, UserService us, AdminService adminService,
                                            CarRepository carRepository, RentalService rentalService, 
                                            PaymentService paymentService, Scanner scanner) {
        String currentId = loggedInUser.getUserId(); // ⭐️ 로그인된 ID 사용

        // 관리자와 일반 사용자 메뉴 분기
        if (isAdmin) {
            switch (menu) {
                case 1: // 정보 조회
                    System.out.println("\n[1. 정보 조회]");
                    Optional<User> userOpt = us.getUserInfo(currentId);
                    userOpt.ifPresentOrElse(
                            user -> {
                                loggedInUser = user;
                                System.out.println("✅ " + user.getName() + "님의 정보는 다음과 같습니다.");
                                System.out.println("ID: " + user.getUserId());
                                System.out.println("이름: " + user.getName());
                                System.out.println("전화번호: " + user.getPhoneNumber());
                                // 관리자는 membership이 "ADMIN"이면 "ADMIN"으로 표시
                                String membership = user.getMembership();
                                String gradeDisplay = (membership != null && membership.equals("ADMIN")) 
                                    ? "ADMIN" 
                                    : user.getUserMembershipStrategy().name().replace("Strategy", "").toUpperCase();
                                System.out.println("등급: " + gradeDisplay);
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
                    loggedInUser = updatedUser;
                    System.out.println("✅ 사용자 정보 수정 완료!");
                    break;

                case 3: // 비밀번호 재설정
                    System.out.println("\n[3. 비밀번호 재설정]");
                    System.out.print("새 비밀번호: "); 
                    String newPassword = scanner.nextLine();
                    if (newPassword.trim().isEmpty()) {
                        System.err.println("❌ 비밀번호는 비워둘 수 없습니다.");
                        break;
                    }
                    
                    User resetUser = us.resetPassword(currentId, newPassword);
                    loggedInUser = resetUser;
                    System.out.println("✅ 비밀번호 재설정이 완료되었습니다!");
                    break;

                case 4: // 차량 등록
                    System.out.println("\n[4. 차량 등록]");
                    try {
                        // 1) 차량 타입 선택
                        System.out.println("차량 타입 선택:");
                        System.out.println("  1. SEDAN");
                        System.out.println("  2. SUV");
                        System.out.println("  3. BIKE");
                        System.out.print("선택: ");
                        String typeInput = scanner.nextLine().trim();
                        int typeChoice;
                        try {
                            typeChoice = Integer.parseInt(typeInput);
                        } catch (NumberFormatException e) {
                            System.err.println("❌ 숫자를 입력해주세요.");
                            break;
                        }
                        
                        CarType type = switch (typeChoice) {
                            case 1 -> CarType.SEDAN;
                            case 2 -> CarType.SUV;
                            case 3 -> CarType.BIKE;
                            default -> {
                                System.err.println("❌ 잘못된 타입 선택입니다. (1-3 중 선택)");
                                yield null;
                            }
                        };
                        
                        if (type == null) {
                            break;
                        }
                        
                        // 2) 차량 이름 입력
                        System.out.print("차량 이름: ");
                        String carName = scanner.nextLine().trim();
                        if (carName.isEmpty()) {
                            System.err.println("❌ 차량 이름은 필수입니다.");
                            break;
                        }
                        
                        // 3) 일일 대여료 입력 (필수)
                        System.out.print("일일 대여료: ");
                        String feeInput = scanner.nextLine().trim();
                        if (feeInput.isEmpty()) {
                            System.err.println("❌ 일일 대여료는 필수입니다.");
                            break;
                        }
                        
                        BigDecimal dailyRentalFee;
                        try {
                            dailyRentalFee = new BigDecimal(feeInput);
                            if (dailyRentalFee.compareTo(BigDecimal.ZERO) <= 0) {
                                System.err.println("❌ 일일 대여료는 0보다 커야 합니다.");
                                break;
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("❌ 올바른 숫자를 입력해주세요.");
                            break;
                        }
                        
                        // 4) 차량 등록
                        adminService.addCar(type, dailyRentalFee, carName);
                        
                    } catch (Exception e) {
                        System.err.println("❌ 차량 등록 실패: " + e.getMessage());
                    }
                    break;

                case 5: // 차량 삭제
                    System.out.println("\n[5. 차량 삭제]");
                    try {
                        // 1) 모든 차량 목록 조회 및 표시
                        List<Map<String, Object>> allCars = adminService.getAllCarsWithStatus();
                        
                        if (allCars.isEmpty()) {
                            System.out.println("❌ 등록된 차량이 없습니다.");
                            break;
                        }
                        
                        System.out.println("\n현재 등록된 차량 목록:");
                        System.out.println("-".repeat(60));
                        for (int i = 0; i < allCars.size(); i++) {
                            Map<String, Object> car = allCars.get(i);
                            String carName = Objects.toString(car.get("name"), "");
                            String carType = Objects.toString(car.get("type"), "");
                            String carStatusDisplay = Objects.toString(car.get("status"), "");
                            
                            System.out.printf("%d. 이름: %s | 타입: %s | 상태: %s%n", 
                                i + 1, carName, carType, carStatusDisplay);
                        }
                        System.out.println("-".repeat(60));
                        
                        // 2) 삭제할 차량 선택 (차량 이름 입력)
                        System.out.print("삭제할 차량 이름 입력: ");
                        String carNameToDelete = scanner.nextLine().trim();
                        
                        if (carNameToDelete.isEmpty()) {
                            System.err.println("❌ 차량 이름을 입력해주세요.");
                            break;
                        }
                        
                        // 3) 차량 조회
                        Optional<Map<String, Object>> carOpt = adminService.findCarByName(carNameToDelete);
                        
                        if (carOpt.isEmpty()) {
                            System.err.println("❌ 해당 이름의 차량을 찾을 수 없습니다.");
                            break;
                        }
                        
                        Map<String, Object> carToDelete = carOpt.get();
                        String carStatus = Objects.toString(carToDelete.get("status"), "");
                        Object idObj = carToDelete.get("id");
                        int carId = (idObj instanceof Number) ? ((Number) idObj).intValue() : 0;
                        
                        // 4) 상태 확인
                        if ("UNAVAILABLE".equalsIgnoreCase(carStatus)) {
                            System.err.println("❌ 현재 렌트중인 차는 삭제할 수 없습니다.");
                            break;
                        }
                        
                        // 5) 삭제 확인
                        System.out.println("⚠️  정말 삭제하시겠습니까? (yes/no)");
                        System.out.print("선택: ");
                        String confirm = scanner.nextLine().trim().toLowerCase();
                        
                        if ("yes".equals(confirm)) {
                            try {
                                boolean deleted = adminService.deleteCarById(carId);
                                if (deleted) {
                                    System.out.println("✅ 차량 삭제가 완료되었습니다.");
                                } else {
                                    System.err.println("❌ 차량 삭제에 실패했습니다.");
                                }
                            } catch (IllegalStateException e) {
                                // 현재 대여 중인 경우
                                System.err.println("❌ " + e.getMessage());
                            } catch (Exception deleteException) {
                                System.err.println("❌ 차량 삭제 실패: " + deleteException.getMessage());
                            }
                        } else if ("no".equals(confirm)) {
                            System.out.println("❌ 차량 삭제가 취소되었습니다.");
                        } else {
                            System.err.println("❌ 'yes' 또는 'no'를 입력해주세요.");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("❌ 차량 삭제 실패: " + e.getMessage());
                    }
                    break;

                case 6: // 대여 기록 조회
                    System.out.println("\n[6. 대여 기록 조회]");
                    try {
                        List<Map<String, Object>> rentalRecords = adminService.getAllRentalRecordsWithCarName();
                        
                        if (rentalRecords.isEmpty()) {
                            System.out.println("❌ 등록된 대여 기록이 없습니다.");
                            break;
                        }
                        
                        System.out.println("\n전체 대여 기록:");
                        System.out.println("-".repeat(40));
                        
                        LocalDateTime now = LocalDateTime.now();
                        
                        for (int i = 0; i < rentalRecords.size(); i++) {
                            Map<String, Object> record = rentalRecords.get(i);
                            
                            // 연체 여부 확인
                            Object endTimeObj = record.get("endTime");
                            LocalDateTime endTime = null;
                            boolean isOverdue = false;
                            
                            if (endTimeObj != null) {
                                if (endTimeObj instanceof Timestamp) {
                                    endTime = ((Timestamp) endTimeObj).toLocalDateTime();
                                } else if (endTimeObj instanceof LocalDateTime) {
                                    endTime = (LocalDateTime) endTimeObj;
                                }
                                
                                // 연체 확인: status가 RENTED이고 현재 시간이 endTime보다 늦으면 연체
                                String recordStatus = Objects.toString(record.get("status"), "");
                                if ("RENTED".equalsIgnoreCase(recordStatus) && endTime != null && now.isAfter(endTime)) {
                                    isOverdue = true;
                                }
                            }
                            
                            // 차량 이름 - 모든 가능한 키 확인
                            String carName = null;
                            if (record.containsKey("carName")) {
                                carName = Objects.toString(record.get("carName"), "");
                            } else if (record.containsKey("c.name")) {
                                carName = Objects.toString(record.get("c.name"), "");
                            }
                            if (carName == null || carName.isEmpty() || "null".equals(carName)) {
                                carName = "알 수 없음";
                            }
                            
                            // 사용자 이름 - 모든 가능한 키 확인
                            String userName = null;
                            if (record.containsKey("userName")) {
                                userName = Objects.toString(record.get("userName"), "");
                            } else if (record.containsKey("u.name")) {
                                userName = Objects.toString(record.get("u.name"), "");
                            }
                            if (userName == null || userName.isEmpty() || "null".equals(userName)) {
                                userName = "알 수 없음";
                            }
                            
                            // 대여 날짜
                            Object startTimeObj = record.get("startTime");
                            String startTimeStr = "";
                            if (startTimeObj != null) {
                                if (startTimeObj instanceof Timestamp) {
                                    startTimeStr = ((Timestamp) startTimeObj).toLocalDateTime().toString();
                                } else if (startTimeObj instanceof LocalDateTime) {
                                    startTimeStr = startTimeObj.toString();
                                } else {
                                    startTimeStr = startTimeObj.toString();
                                }
                                // 날짜 형식 간소화 (시간 부분 제거)
                                if (startTimeStr.contains("T")) {
                                    startTimeStr = startTimeStr.substring(0, startTimeStr.indexOf("T"));
                                }
                            }
                            
                            // 반납 날짜
                            String endTimeStr = "";
                            if (endTime != null) {
                                endTimeStr = endTime.toString();
                                if (endTimeStr.contains("T")) {
                                    endTimeStr = endTimeStr.substring(0, endTimeStr.indexOf("T"));
                                }
                            }
                            
                            // 상태
                            String status = Objects.toString(record.get("status"), "");
                            
                            // 연체 표시
                            String overdueIcon = isOverdue ? "🔴 " : "";
                            
                            // 세로로 출력
                            System.out.println(overdueIcon + "차량: " + carName);
                            System.out.println("사용자: " + userName);
                            System.out.println("대여 날짜: " + startTimeStr);
                            System.out.println("반납 날짜: " + endTimeStr);
                            System.out.println("상태: " + status);
                            
                            // 마지막 항목이 아니면 구분선 추가
                            if (i < rentalRecords.size() - 1) {
                                System.out.println();
                            }
                        }
                        System.out.println("-".repeat(40));
                        
                    } catch (Exception e) {
                        System.err.println("❌ 대여 기록 조회 실패: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;

                case 7: // 시즌 변경
                    System.out.println("\n[7. 시즌 변경]");
                    try {
                        // 현재 시즌 표시
                        String currentSeasonName = "";
                        if (currentSeason instanceof PeakSeasonFeeStrategy) {
                            currentSeasonName = "성수기 (20% 할증)";
                        } else if (currentSeason instanceof OffSeasonFeeStrategy) {
                            currentSeasonName = "비수기 (10% 할인)";
                        } else {
                            currentSeasonName = "기본";
                        }
                        
                        System.out.println("현재 시즌: " + currentSeasonName);
                        System.out.println("\n변경할 시즌 선택:");
                        System.out.println("  1. 기본");
                        System.out.println("  2. 성수기 (20% 할증)");
                        System.out.println("  3. 비수기 (10% 할인)");
                        System.out.print("선택: ");
                        String seasonChoice = scanner.nextLine().trim();
                        
                        FeeStrategy newSeason = null;
                        String newSeasonName = "";
                        switch (seasonChoice) {
                            case "1":
                                newSeason = new BaseFeeStrategy();
                                newSeasonName = "기본";
                                break;
                            case "2":
                                newSeason = new PeakSeasonFeeStrategy();
                                newSeasonName = "성수기 (20% 할증)";
                                break;
                            case "3":
                                newSeason = new OffSeasonFeeStrategy();
                                newSeasonName = "비수기 (10% 할인)";
                                break;
                            default:
                                System.err.println("❌ 잘못된 선택입니다. (1-3 중 선택)");
                                break;
                        }
                        
                        if (newSeason != null) {
                            currentSeason = newSeason;
                            System.out.println("✅ 시즌이 '" + newSeasonName + "'로 변경되었습니다.");
                            System.out.println("   (이후 모든 차량 대여에 적용됩니다)");
                        }
                        
                    } catch (Exception e) {
                        System.err.println("❌ 시즌 변경 실패: " + e.getMessage());
                    }
                    break;

                case 8: // 로그아웃
                    System.out.println("\n🚪 로그아웃 되었습니다.");
                    loggedInUser = null;
                    isAdmin = false;
                    break;

                default:
                    System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                    break;
            }
            return;
        }

        // 일반 사용자 메뉴
        switch (menu) {
            case 1: // 정보 조회
                System.out.println("\n[1. 정보 조회]");
                Optional<User> userOpt = us.getUserInfo(currentId);
                userOpt.ifPresentOrElse(
                        user -> {
                            // ⭐️ 업데이트된 정보를 반영하기 위해 loggedInUser 갱신
                            loggedInUser = user;
                            System.out.println("✅ " + user.getName() + "님의 정보는 다음과 같습니다.");
                            System.out.println("ID: " + user.getUserId());
                            System.out.println("이름: " + user.getName());
                            System.out.println("전화번호: " + user.getPhoneNumber());
                            // 관리자는 membership이 "ADMIN"이면 "ADMIN"으로 표시
                            String membership = user.getMembership();
                            String gradeDisplay = (membership != null && membership.equals("ADMIN")) 
                                ? "ADMIN" 
                                : user.getUserMembershipStrategy().name().replace("Strategy", "").toUpperCase();
                            System.out.println("등급: " + gradeDisplay);
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

            case 3: // 비밀번호 재설정
                System.out.println("\n[3. 비밀번호 재설정]");
                System.out.print("새 비밀번호: "); 
                String newPassword = scanner.nextLine();
                if (newPassword.trim().isEmpty()) {
                    System.err.println("❌ 비밀번호는 비워둘 수 없습니다.");
                    break;
                }
                
                User resetUser = us.resetPassword(currentId, newPassword);
                loggedInUser = resetUser; // ⭐️ 갱신
                System.out.println("✅ 비밀번호 재설정이 완료되었습니다!");
                break;

            case 4: // 카드 등록
                System.out.println("\n[4. 카드 등록]");
                System.out.print("카드 번호: "); String cardNum = scanner.nextLine();
                User userWithCard = us.registerCard(currentId, cardNum);
                loggedInUser = userWithCard; // ⭐️ 갱신
                System.out.println("✅ 카드 등록이 완료되었습니다.");
                break;

            case 5: // 회원 탈퇴
                System.out.println("\n[5. 회원 탈퇴]");
                System.out.println("⚠️  정말 회원 탈퇴를 하시겠습니까? (yes/no)");
                System.out.print("선택: ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if ("yes".equals(confirm)) {
                    // 탈퇴 전에 이름 저장
                    String userName = loggedInUser != null ? loggedInUser.getName() : currentId;
                    us.withdraw(currentId);
                    System.out.println("✅ " + userName + "님 회원 탈퇴가 완료되었습니다.");
                    loggedInUser = null; // ⭐️ 탈퇴 후 로그아웃 처리
                    isAdmin = false;
                } else if ("no".equals(confirm)) {
                    System.out.println("❌ 회원 탈퇴가 취소되었습니다.");
                } else {
                    System.err.println("❌ 'yes' 또는 'no'를 입력해주세요.");
                }
                break;
                
            case 6: // 빌릴 수 있는 차량 조회
                System.out.println("\n[6. 빌릴 수 있는 차량 조회]");
                List<Car> allCarsForAvailable = carRepository.findAllCars();
                List<Car> availableCars = allCarsForAvailable.stream()
                    .filter(car -> car.status() == domain.car.carFactory.CarStatus.AVAILABLE)
                    .collect(java.util.stream.Collectors.toList());
                
                if (availableCars.isEmpty()) {
                    System.out.println("❌ 현재 대여 가능한 차량이 없습니다.");
                } else {
                    System.out.println("✅ 대여 가능한 차량 목록:");
                    System.out.println("-".repeat(60));
                    for (int i = 0; i < availableCars.size(); i++) {
                        Car car = availableCars.get(i);
                        BigDecimal fee = car.getDailyRentalFee() != null ? car.getDailyRentalFee() : car.type().baseRate();
                        System.out.printf("%d. [%s] %s | 일일 요금: %s원%n", 
                            i + 1, car.type(), car.getName(), formatMoney(fee));
                    }
                    System.out.println("-".repeat(60));
                }
                break;
                
            case 7: // 차량 대여
                System.out.println("\n[7. 차량 대여]");
                // 1) 차량 타입 선택
                System.out.println("차량 타입 선택:");
                System.out.println("  1. SEDAN");
                System.out.println("  2. SUV");
                System.out.println("  3. BIKE");
                System.out.print("선택: ");
                int rentTypeChoice = Integer.parseInt(scanner.nextLine());
                CarType rentType = switch (rentTypeChoice) {
                    case 1 -> CarType.SEDAN;
                    case 2 -> CarType.SUV;
                    case 3 -> CarType.BIKE;
                    default -> throw new IllegalArgumentException("잘못된 타입 선택");
                };
                
                // 2) 해당 타입의 사용 가능한 차량 목록 표시
                List<Car> allCars = carRepository.findAllCars();
                List<Car> cars = allCars.stream()
                    .filter(car -> car.status() == domain.car.carFactory.CarStatus.AVAILABLE && car.type() == rentType)
                    .collect(java.util.stream.Collectors.toList());
                if (cars.isEmpty()) {
                    System.out.println("❌ 현재 대여 가능한 " + rentType + " 차량이 없습니다.");
                    break;
                }
                System.out.println("\n대여 가능한 " + rentType + " 차량 목록:");
                for (int i = 0; i < cars.size(); i++) {
                    Car car = cars.get(i);
                    BigDecimal fee = car.getDailyRentalFee() != null ? car.getDailyRentalFee() : car.type().baseRate();
                    System.out.printf("%d. %s | 일일 요금: %s원%n", 
                        i + 1, car.getName(), formatMoney(fee));
                }
                
                // 3) 차량 이름으로 선택
                System.out.print("\n대여할 차량 이름 입력: ");
                String carName = scanner.nextLine().trim();
                Optional<Car> carOpt = cars.stream()
                    .filter(car -> car.getName().equals(carName))
                    .findFirst();
                if (carOpt.isEmpty()) {
                    System.err.println("❌ 해당 이름의 차량을 찾을 수 없습니다.");
                    break;
                }
                Car selectedCar = carOpt.get();
                
                if (selectedCar.status() != domain.car.carFactory.CarStatus.AVAILABLE) {
                    System.err.println("❌ 해당 차량은 현재 대여 불가능합니다.");
                    break;
                }
                
                // 3) 대여 일수 입력
                System.out.print("대여 일수: ");
                int rentalDays = Integer.parseInt(scanner.nextLine());
                if (rentalDays <= 0) {
                    System.err.println("❌ 대여 일수는 1일 이상이어야 합니다.");
                    break;
                }
                
                // 4) 옵션 선택
                System.out.println("추가 옵션 선택 (여러 개 선택 가능, 엔터로 종료):");
                System.out.println("  - Blackbox (블랙박스) - 5,000원/일");
                System.out.println("  - Navigation (네비게이션) - 7,000원/일");
                System.out.println("  - Sunroof (선루프) - 15,000원/일");
                List<String> options = new ArrayList<>();
                while (true) {
                    System.out.print("옵션 이름 (엔터로 종료): ");
                    String option = scanner.nextLine().trim();
                    if (option.isEmpty()) break;
                    if (option.equals("Blackbox") || option.equals("Navigation") || option.equals("Sunroof")) {
                        options.add(option);
                        System.out.println("✅ " + option + " 옵션이 추가되었습니다.");
                    } else {
                        System.out.println("⚠️ 알 수 없는 옵션입니다. 무시됩니다.");
                    }
                }
                
                // 5) 요금 정책: 현재 설정된 시즌 사용 (사용자 선택 없음)
                FeeStrategy feeStrategy = currentSeason;
                
                // 6) 대여 실행 및 요금 계산 과정 출력
                try {
                    // 요금 계산을 위한 사전 계산
                    BigDecimal dailyFee = selectedCar.getDailyRentalFee() != null ? 
                                         selectedCar.getDailyRentalFee() : 
                                         selectedCar.type().baseRate();
                    
                    // 정책 적용 전 기본 요금
                    BigDecimal baseFeeBeforePolicy = dailyFee.multiply(new BigDecimal(rentalDays));
                    // 정책 적용 후 기본 요금
                    BigDecimal baseFee = feeStrategy.calculateTotalFee(selectedCar, rentalDays);
                    
                    // 요금 정책 적용 금액 (할인/할증)
                    BigDecimal policyAdjustment = baseFee.subtract(baseFeeBeforePolicy);
                    String policyDescription = "";
                    if (feeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyDescription = " (20% 할증)";
                    } else if (feeStrategy instanceof OffSeasonFeeStrategy) {
                        policyDescription = " (10% 할인)";
                    }
                    
                    // 옵션 비용 계산
                    BigDecimal totalOptionCost = BigDecimal.ZERO;
                    Map<String, BigDecimal> optionCosts = new HashMap<>();
                    if (options != null && !options.isEmpty()) {
                        for (String option : options) {
                            BigDecimal optionDailyCost = switch (option) {
                                case "Blackbox" -> new BigDecimal("5000");
                                case "Navigation" -> new BigDecimal("7000");
                                case "Sunroof" -> new BigDecimal("15000");
                                default -> BigDecimal.ZERO;
                            };
                            BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(rentalDays));
                            optionCosts.put(option, optionTotal);
                            totalOptionCost = totalOptionCost.add(optionTotal);
                        }
                    }
                    
                    BigDecimal totalFee = baseFee.add(totalOptionCost);
                    
                    // 요금 계산 과정 출력 (간단하게)
                    System.out.println("\n--- [요금 계산 내역] ---");
                    System.out.printf("차량: %s (%s)%n", selectedCar.id(), selectedCar.type());
                    System.out.printf("차량 일일 요금: %s원%n", formatMoney(dailyFee));
                    System.out.printf("대여 일수: %d일%n", rentalDays);
                    System.out.printf("요금 정책: %s%s%n", feeStrategy.getClass().getSimpleName(), policyDescription);
                    
                    // 옵션 표시 (가격 포함)
                    if (!optionCosts.isEmpty()) {
                        System.out.print("옵션: ");
                        List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                        for (int i = 0; i < optionNames.size(); i++) {
                            if (i > 0) System.out.print(", ");
                            String optionName = optionNames.get(i);
                            BigDecimal optionTotal = optionCosts.get(optionName);
                            BigDecimal optionDaily = optionTotal.divide(new BigDecimal(rentalDays));
                            System.out.printf("%s (%s원/일)", optionName, formatMoney(optionDaily));
                        }
                        System.out.println();
                    } else {
                        System.out.println("옵션: 없음");
                    }
                    System.out.println();
                    
                    // 간단한 계산식 출력
                    String policyPercent = "";
                    if (feeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyPercent = " × 120%";
                    } else if (feeStrategy instanceof OffSeasonFeeStrategy) {
                        policyPercent = " × 90%";
                    }
                    
                    System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                        formatMoney(dailyFee), rentalDays, policyPercent, 
                        formatMoney(totalOptionCost), formatMoney(totalFee));
                    System.out.println("-------------------\n");
                    
                    RentalRecord record = rentalService.rent(currentId, selectedCar, rentalDays, options, feeStrategy);
                    // 대여 시 생성된 RentalRecord를 메모리에 저장 (반납 시 정보 유지)
                    // ⚠️ 중요: record 객체를 그대로 저장하면 나중에 수정될 수 있으므로, 
                    // 새로운 RentalRecord를 생성하여 복사본을 저장
                    RentalRecord recordCopy = new RentalRecord();
                    recordCopy.setId(record.getId());
                    recordCopy.setUserId(record.getUserId());
                    recordCopy.setCarId(record.getCarId());
                    recordCopy.setRentalDays(record.getRentalDays());
                    recordCopy.setStartAt(record.getStartAt());
                    recordCopy.setEndAt(record.getEndAt());
                    recordCopy.setStatus(record.getStatus());
                    recordCopy.setFeeStrategyType(record.getFeeStrategyType());
                    recordCopy.setMembershipStrategyType(record.getMembershipStrategyType());
                    recordCopy.setOptions(new ArrayList<>(record.getOptions()));
                    recordCopy.setBaseFee(record.getBaseFee());
                    recordCopy.setOptionFee(record.getOptionFee());
                    recordCopy.setDiscount(record.getDiscount());
                    recordCopy.setPenalty(record.getPenalty());
                    recordCopy.setTotalFee(record.getTotalFee());
                    
                    rentalRecordCache.put(record.getId(), recordCopy);
                    // 차량 상태를 DB에 업데이트 (UNAVAILABLE로 변경)
                    selectedCar.occupy();
                    carRepository.update(selectedCar);
                    System.out.println("✅ 차량 대여가 완료되었습니다!");
                    System.out.println("대여 ID: " + record.getId());
                } catch (Exception e) {
                    System.err.println("❌ 대여 실패: " + e.getMessage());
                }
                break;
                
            case 8: // 차량 반납
                System.out.println("\n[8. 차량 반납]");
                
                // 1) 현재 사용자의 렌트 중인 차량 목록 조회
                User currentUser = us.getUserInfo(currentId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                int userPk = currentUser.getId();
                
                RentalRepository rentalRepo = new RentalRepository(new DBConnection());
                List<RentalRecord> activeRentals = rentalRepo.findActiveByUserId(userPk);
                
                if (activeRentals.isEmpty()) {
                    System.out.println("❌ 현재 대여 중인 차량이 없습니다.");
                    break;
                }
                
                // 2) 렌트 중인 차량 목록 표시
                System.out.println("\n현재 대여 중인 차량 목록:");
                System.out.println("-".repeat(60));
                List<RentalRecord> validRecords = new ArrayList<>();
                for (int i = 0; i < activeRentals.size(); i++) {
                    RentalRecord record = activeRentals.get(i);
                    // 메모리 캐시에서 정보 가져오기 (없으면 DB에서 가져온 것 사용)
                    RentalRecord cachedRecord = rentalRecordCache.get(record.getId());
                    if (cachedRecord != null) {
                        record = cachedRecord; // 캐시된 정보 사용 (baseFee, optionFee 등 포함)
                    }
                    
                    String carIdStr = record.getCarId();
                    Car car = carRepository.findById(carIdStr);
                    if (car == null) {
                        continue; // 차량을 찾을 수 없으면 건너뛰기
                    }
                    
                    String displayCarName = car.getName();
                    String startDate = record.getStartAt() != null ? 
                            record.getStartAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
                            "알 수 없음";
                    
                    System.out.printf("%d. %s | 대여일: %s%n", i + 1, displayCarName, startDate);
                    validRecords.add(record);
                }
                System.out.println("-".repeat(60));
                
                if (validRecords.isEmpty()) {
                    System.err.println("❌ 반납 가능한 차량이 없습니다.");
                    break;
                }
                
                // 3) 차량 이름으로 반납할 차량 선택
                System.out.print("\n반납할 차량 이름 입력: ");
                String returnCarName = scanner.nextLine().trim();
                
                // 차량 이름으로 대여 기록 찾기
                RentalRecord selectedRecord = null;
                Car returnCar = null;
                for (RentalRecord record : validRecords) {
                    String carIdStr = record.getCarId();
                    Car car = carRepository.findById(carIdStr);
                    if (car != null && car.getName().equals(returnCarName)) {
                        // 먼저 캐시에서 정보 가져오기 (baseFee, optionFee 포함)
                        RentalRecord cachedRecord = rentalRecordCache.get(record.getId());
                        if (cachedRecord != null) {
                            selectedRecord = cachedRecord; // 캐시된 정보 사용 (baseFee, optionFee 포함)
                        } else {
                            selectedRecord = record; // 캐시가 없으면 DB에서 가져온 것 사용
                        }
                        returnCar = car;
                        break;
                    }
                }
                
                if (selectedRecord == null || returnCar == null) {
                    System.err.println("❌ 해당 이름의 대여 중인 차량을 찾을 수 없습니다.");
                    break;
                }
                
                // baseFee와 optionFee가 0이면 캐시에서 다시 확인
                if ((selectedRecord.getBaseFee() == null || selectedRecord.getBaseFee().compareTo(BigDecimal.ZERO) == 0) &&
                    (selectedRecord.getOptionFee() == null || selectedRecord.getOptionFee().compareTo(BigDecimal.ZERO) == 0)) {
                    RentalRecord cachedRecordForReturn = rentalRecordCache.get(selectedRecord.getId());
                    if (cachedRecordForReturn != null) {
                        selectedRecord = cachedRecordForReturn; // 캐시된 정보 사용
                    }
                }
                
                long rentalId = selectedRecord.getId();
                
                try {
                    // 반납 전 사용자 등급 저장
                    User userBeforeReturn = us.getUserInfo(currentId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    String membershipBefore = userBeforeReturn.getUserMembershipStrategy().getClass().getSimpleName();
                    
                    // 반납 실행 (캐시된 레코드 전달하여 baseFee, optionFee 유지)
                    // selectedRecord는 이미 캐시에서 가져온 정보이므로 baseFee, optionFee가 포함되어 있음
                    rentalService.returnCar(rentalId, returnCar, selectedRecord);
                    
                    // 반납 후 selectedRecord가 이미 업데이트됨 (penalty, discount, totalFee 포함)
                    // baseFee, optionFee는 유지됨
                    // 하지만 discount와 totalFee는 RentalService에서 계산된 값이므로 다시 가져와야 함
                    // selectedRecord는 이미 업데이트되었으므로 그대로 사용
                    
                    // 차량 상태를 DB에 업데이트 (AVAILABLE로 변경)
                    returnCar.release();
                    carRepository.update(returnCar);
                    
                    System.out.println("\n✅ 반납이 성공적으로 완료되었습니다!");
                    System.out.println("\n반납 요금은 다음과 같습니다:\n");
                    
                    // 요금 명세서 출력 (대여 시와 동일한 형식)
                    // selectedRecord는 이미 반납 후 업데이트됨 (penalty, discount, totalFee 포함)
                    BigDecimal dailyFee = returnCar.getDailyRentalFee() != null ? 
                                         returnCar.getDailyRentalFee() : 
                                         returnCar.type().baseRate();
                    int returnRentalDays = selectedRecord.getRentalDays();
                    
                    // 요금 정책 재구성 (명세서 출력용)
                    FeeStrategy returnFeeStrategy;
                    String feeStrategyType = selectedRecord.getFeeStrategyType();
                    if (feeStrategyType == null || feeStrategyType.isEmpty()) {
                        returnFeeStrategy = new BaseFeeStrategy();
                    } else if ("PeakSeasonFeeStrategy".equals(feeStrategyType)) {
                        returnFeeStrategy = new PeakSeasonFeeStrategy();
                    } else if ("OffSeasonFeeStrategy".equals(feeStrategyType)) {
                        returnFeeStrategy = new OffSeasonFeeStrategy();
                    } else {
                        returnFeeStrategy = new BaseFeeStrategy();
                    }
                    
                    // baseFee는 요금 정책을 사용하여 재계산 (대여 시와 동일하게)
                    // selectedRecord의 baseFee가 잘못 저장되었을 수 있으므로 항상 재계산
                    BigDecimal baseFee = returnFeeStrategy.calculateTotalFee(returnCar, returnRentalDays);
                    
                    // 정책 적용 전 기본 요금 (명세서 출력용)
                    BigDecimal baseFeeBeforePolicy = dailyFee.multiply(new BigDecimal(returnRentalDays));
                    String policyDescription = "";
                    if (returnFeeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyDescription = " (20% 할증)";
                    } else if (returnFeeStrategy instanceof OffSeasonFeeStrategy) {
                        policyDescription = " (10% 할인)";
                    }
                    
                    // 옵션 비용 계산 (대여 시 저장된 옵션 정보 사용)
                    Map<String, BigDecimal> optionCosts = new HashMap<>();
                    List<String> returnOptions = selectedRecord.getOptions();
                    if (returnOptions != null && !returnOptions.isEmpty()) {
                        for (String option : returnOptions) {
                            BigDecimal optionDailyCost = switch (option) {
                                case "Blackbox" -> new BigDecimal("5000");
                                case "Navigation" -> new BigDecimal("7000");
                                case "Sunroof" -> new BigDecimal("15000");
                                default -> BigDecimal.ZERO;
                            };
                            BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(returnRentalDays));
                            optionCosts.put(option, optionTotal);
                        }
                    }
                    
                    // 연체 패널티
                    BigDecimal penalty = selectedRecord.getPenalty() != null ? selectedRecord.getPenalty() : BigDecimal.ZERO;
                    
                    // 요금 계산 과정 출력 (대여 시와 동일한 형식)
                    System.out.println("--- [반납 요금 계산 내역] ---");
                    System.out.printf("차량: %s (%s)%n", returnCar.getName(), returnCar.type());
                    System.out.printf("차량 일일 요금: %s원%n", formatMoney(dailyFee));
                    System.out.printf("대여 일수: %d일%n", returnRentalDays);
                    System.out.printf("요금 정책: %s%s%n", returnFeeStrategy.getClass().getSimpleName(), policyDescription);
                    
                    // 옵션 표시 (대여 시와 동일한 형식)
                    if (!optionCosts.isEmpty()) {
                        System.out.print("옵션: ");
                        List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                        for (int i = 0; i < optionNames.size(); i++) {
                            if (i > 0) System.out.print(", ");
                            String optionName = optionNames.get(i);
                            BigDecimal optionTotal = optionCosts.get(optionName);
                            BigDecimal optionDaily = optionTotal.divide(new BigDecimal(returnRentalDays));
                            System.out.printf("%s (%s원/일)", optionName, formatMoney(optionDaily));
                        }
                        System.out.println();
                    } else {
                        System.out.println("옵션: 없음");
                    }
                    System.out.println();
                    
                    // 간단한 계산식 출력 (대여 시와 동일한 형식)
                    String policyPercent = "";
                    if (returnFeeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyPercent = " × 120%";
                    } else if (returnFeeStrategy instanceof OffSeasonFeeStrategy) {
                        policyPercent = " × 90%";
                    }
                    
                    // 옵션 비용 계산 (optionCosts에서 계산한 값 사용 - 대여 시와 동일하게 계산)
                    BigDecimal totalOptionCost = BigDecimal.ZERO;
                    if (!optionCosts.isEmpty()) {
                        for (BigDecimal cost : optionCosts.values()) {
                            totalOptionCost = totalOptionCost.add(cost);
                        }
                    }
                    // optionCosts가 비어있고 optionFee가 있으면 optionFee 사용 (옵션이 없는 경우)
                    // 하지만 optionCosts에서 계산한 값이 더 정확하므로 우선 사용
                    
                    // 계산식: (일일요금 × 일수) × 정책 + 옵션비용 = baseFee + totalOptionCost
                    // baseFee는 이미 정책이 적용된 값이므로 그대로 사용
                    BigDecimal calculatedTotal = baseFee.add(totalOptionCost);
                    System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                        formatMoney(dailyFee), returnRentalDays, policyPercent, 
                        formatMoney(totalOptionCost), formatMoney(calculatedTotal));
                    System.out.println();
                    
                    // 대여 시 요금 계산 (계산식과 동일하게 baseFee + totalOptionCost 사용)
                    // penalty는 대여 시 요금에 포함하지 않음 (반납 시 추가되는 것)
                    BigDecimal rentalFee = calculatedTotal;
                    System.out.printf("대여 시 요금: %s원%n", formatMoney(rentalFee));
                    
                    // 회원 등급 할인 재계산 (대여 시 요금에만 적용, penalty 제외)
                    // rentalFee = baseFee + totalOptionCost (penalty 제외)
                    BigDecimal discountedAmount = userBeforeReturn.applyDiscount(rentalFee);
                    BigDecimal discount = rentalFee.subtract(discountedAmount);
                    
                    // 최종 결제 금액 = 할인된 대여 시 요금 + penalty
                    BigDecimal totalFee = discountedAmount.add(penalty);
                    
                    // 회원 등급 할인 (등급 표시 포함) - 반납 전 등급 사용
                    String membershipName = userBeforeReturn.getUserMembershipStrategy().getClass().getSimpleName();
                    String membershipDisplay = membershipName.replace("Strategy", "").toUpperCase();
                    
                    if (discount.compareTo(BigDecimal.ZERO) > 0) {
                        System.out.printf("회원 등급 할인(%s): -%s원%n", membershipDisplay, formatMoney(discount));
                    } else {
                        System.out.printf("회원 등급 할인(%s): 없음%n", membershipDisplay);
                    }
                    
                    System.out.println();
                    System.out.printf("총 결제 금액: %s원%n", formatMoney(totalFee));
                    System.out.println("-------------------\n");
                    
                    // 등급 승급 확인 및 메시지 출력
                    // 반납 후 사용자 정보를 다시 조회하여 최신 등급 정보 가져오기
                    User userAfterReturn = us.getUserInfo(currentId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    // ⭐️ 반납 후 등급이 승급되었을 수 있으므로 loggedInUser 갱신
                    loggedInUser = userAfterReturn;
                    String membershipAfter = userAfterReturn.getUserMembershipStrategy().getClass().getSimpleName();
                    
                    if (!membershipBefore.equals(membershipAfter)) {
                        String beforeGrade = membershipBefore.replace("Strategy", "");
                        String afterGrade = membershipAfter.replace("Strategy", "");
                        System.out.printf("🎉 회원 등급이 %s에서 %s로 올랐습니다!%n", beforeGrade, afterGrade);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ 반납 실패: " + e.getMessage());
                }
                break;
                
            case 9: // 결제 금액 확인
                System.out.println("\n[9. 결제 금액 확인]");
                
                // 1) 현재 사용자의 렌트 중인 차량 목록 조회
                User currentUserForPayment = us.getUserInfo(currentId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                int userPkForPayment = currentUserForPayment.getId();
                
                RentalRepository paymentRentalRepo = new RentalRepository(new DBConnection());
                List<RentalRecord> activeRentalsForPayment = paymentRentalRepo.findActiveByUserId(userPkForPayment);
                
                if (activeRentalsForPayment.isEmpty()) {
                    System.out.println("❌ 현재 대여 중인 차량이 없습니다.");
                    break;
                }
                
                // 2) 렌트 중인 차량 목록 표시
                System.out.println("\n현재 대여 중인 차량 목록:");
                System.out.println("-".repeat(60));
                List<RentalRecord> validRecordsForPayment = new ArrayList<>();
                for (int i = 0; i < activeRentalsForPayment.size(); i++) {
                    RentalRecord record = activeRentalsForPayment.get(i);
                    // 메모리 캐시에서 정보 가져오기 (없으면 DB에서 가져온 것 사용)
                    RentalRecord cachedRecord = rentalRecordCache.get(record.getId());
                    if (cachedRecord != null) {
                        record = cachedRecord; // 캐시된 정보 사용 (baseFee, optionFee 등 포함)
                    }
                    
                    String carIdStr = record.getCarId();
                    Car car = carRepository.findById(carIdStr);
                    if (car == null) {
                        continue; // 차량을 찾을 수 없으면 건너뛰기
                    }
                    
                    String displayCarName = car.getName();
                    String startDate = record.getStartAt() != null ? 
                            record.getStartAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) : 
                            "알 수 없음";
                    
                    System.out.printf("%d. %s | 대여일: %s%n", i + 1, displayCarName, startDate);
                    validRecordsForPayment.add(record);
                }
                System.out.println("-".repeat(60));
                
                if (validRecordsForPayment.isEmpty()) {
                    System.err.println("❌ 확인 가능한 차량이 없습니다.");
                    break;
                }
                
                // 3) 차량 이름으로 확인할 차량 선택
                System.out.print("\n결제 금액을 확인할 차량 이름 입력: ");
                String paymentCarName = scanner.nextLine().trim();
                
                // 차량 이름으로 대여 기록 찾기
                RentalRecord selectedRecordForPayment = null;
                Car paymentCar = null;
                for (RentalRecord record : validRecordsForPayment) {
                    String carIdStr = record.getCarId();
                    Car car = carRepository.findById(carIdStr);
                    if (car != null && car.getName().equals(paymentCarName)) {
                        // 먼저 캐시에서 정보 가져오기 (baseFee, optionFee 포함)
                        RentalRecord cachedRecord = rentalRecordCache.get(record.getId());
                        if (cachedRecord != null) {
                            selectedRecordForPayment = cachedRecord; // 캐시된 정보 사용 (baseFee, optionFee 포함)
                        } else {
                            selectedRecordForPayment = record; // 캐시가 없으면 DB에서 가져온 것 사용
                        }
                        paymentCar = car;
                        break;
                    }
                }
                
                if (selectedRecordForPayment == null || paymentCar == null) {
                    System.err.println("❌ 해당 이름의 대여 중인 차량을 찾을 수 없습니다.");
                    break;
                }
                
                // baseFee와 optionFee가 0이면 캐시에서 다시 확인
                if ((selectedRecordForPayment.getBaseFee() == null || selectedRecordForPayment.getBaseFee().compareTo(BigDecimal.ZERO) == 0) &&
                    (selectedRecordForPayment.getOptionFee() == null || selectedRecordForPayment.getOptionFee().compareTo(BigDecimal.ZERO) == 0)) {
                    RentalRecord cachedRecordForPayment = rentalRecordCache.get(selectedRecordForPayment.getId());
                    if (cachedRecordForPayment != null) {
                        selectedRecordForPayment = cachedRecordForPayment; // 캐시된 정보 사용
                    }
                }
                
                try {
                    // 대여 시 청구한 금액 표시 (대여 시와 동일한 형식)
                    BigDecimal dailyFee = paymentCar.getDailyRentalFee() != null ? 
                                         paymentCar.getDailyRentalFee() : 
                                         paymentCar.type().baseRate();
                    int paymentRentalDays = selectedRecordForPayment.getRentalDays();
                    
                    // 요금 정책 재구성
                    FeeStrategy paymentFeeStrategy;
                    String feeStrategyType = selectedRecordForPayment.getFeeStrategyType();
                    if (feeStrategyType == null || feeStrategyType.isEmpty()) {
                        paymentFeeStrategy = new BaseFeeStrategy();
                    } else if ("PeakSeasonFeeStrategy".equals(feeStrategyType)) {
                        paymentFeeStrategy = new PeakSeasonFeeStrategy();
                    } else if ("OffSeasonFeeStrategy".equals(feeStrategyType)) {
                        paymentFeeStrategy = new OffSeasonFeeStrategy();
                    } else {
                        paymentFeeStrategy = new BaseFeeStrategy();
                    }
                    
                    // baseFee 재계산 (대여 시와 동일하게)
                    BigDecimal baseFee = paymentFeeStrategy.calculateTotalFee(paymentCar, paymentRentalDays);
                    
                    String policyDescription = "";
                    if (paymentFeeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyDescription = " (20% 할증)";
                    } else if (paymentFeeStrategy instanceof OffSeasonFeeStrategy) {
                        policyDescription = " (10% 할인)";
                    }
                    
                    // 옵션 비용 계산
                    Map<String, BigDecimal> optionCosts = new HashMap<>();
                    List<String> paymentOptions = selectedRecordForPayment.getOptions();
                    if (paymentOptions != null && !paymentOptions.isEmpty()) {
                        for (String option : paymentOptions) {
                            BigDecimal optionDailyCost = switch (option) {
                                case "Blackbox" -> new BigDecimal("5000");
                                case "Navigation" -> new BigDecimal("7000");
                                case "Sunroof" -> new BigDecimal("15000");
                                default -> BigDecimal.ZERO;
                            };
                            BigDecimal optionTotal = optionDailyCost.multiply(new BigDecimal(paymentRentalDays));
                            optionCosts.put(option, optionTotal);
                        }
                    }
                    
                    // 옵션 비용 합계
                    BigDecimal totalOptionCost = BigDecimal.ZERO;
                    if (!optionCosts.isEmpty()) {
                        for (BigDecimal cost : optionCosts.values()) {
                            totalOptionCost = totalOptionCost.add(cost);
                        }
                    }
                    
                    // 총 요금
                    BigDecimal totalFee = baseFee.add(totalOptionCost);
                    
                    // 요금 계산 과정 출력 (대여 시와 동일한 형식)
                    System.out.println("\n--- [요금 계산 내역] ---");
                    System.out.printf("차량: %s (%s)%n", paymentCar.getName(), paymentCar.type());
                    System.out.printf("차량 일일 요금: %s원%n", formatMoney(dailyFee));
                    System.out.printf("대여 일수: %d일%n", paymentRentalDays);
                    System.out.printf("요금 정책: %s%s%n", paymentFeeStrategy.getClass().getSimpleName(), policyDescription);
                    
                    // 옵션 표시 (가격 포함)
                    if (!optionCosts.isEmpty()) {
                        System.out.print("옵션: ");
                        List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                        for (int i = 0; i < optionNames.size(); i++) {
                            if (i > 0) System.out.print(", ");
                            String optionName = optionNames.get(i);
                            BigDecimal optionTotal = optionCosts.get(optionName);
                            BigDecimal optionDaily = optionTotal.divide(new BigDecimal(paymentRentalDays));
                            System.out.printf("%s (%s원/일)", optionName, formatMoney(optionDaily));
                        }
                        System.out.println();
                    } else {
                        System.out.println("옵션: 없음");
                    }
                    System.out.println();
                    
                    // 간단한 계산식 출력
                    String policyPercent = "";
                    if (paymentFeeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyPercent = " × 120%";
                    } else if (paymentFeeStrategy instanceof OffSeasonFeeStrategy) {
                        policyPercent = " × 90%";
                    }
                    
                    System.out.printf("(%s원 × %d일)%s + %s원 = %s원%n",
                        formatMoney(dailyFee), paymentRentalDays, policyPercent, 
                        formatMoney(totalOptionCost), formatMoney(totalFee));
                    System.out.println("-------------------\n");
                    
                } catch (Exception e) {
                    System.err.println("❌ 결제 금액 확인 실패: " + e.getMessage());
                }
                break;

            case 10: // 로그아웃 ⭐️
                System.out.println("\n🚪 로그아웃 되었습니다.");
                loggedInUser = null; // ⭐️ loggedInUser를 null로 설정하여 로그인 전 상태로 돌아감
                isAdmin = false;
                break;

            default:
                System.err.println("\n🚨 [오류] 유효하지 않은 메뉴 번호입니다.");
                break;
        }
    }
}