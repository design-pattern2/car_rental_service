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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static User loggedInUser = null;
    private static boolean isAdmin = false;
    // 대여 시 생성된 RentalRecord를 메모리에 저장 (반납 시 정보 유지)
    private static Map<Long, RentalRecord> rentalRecordCache = new HashMap<>();
    
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
            
            String message2 = "   회원 인증을 해주세요!";
            typeWriter(message2, 100);
            System.out.println();
            
            System.out.println("=".repeat(50));
            
        } catch (InterruptedException e) {
            // 인터럽트 발생 시 그냥 메시지만 표시
            System.out.println("\n" + "=".repeat(50));
            System.out.println("   어서오세요 차량 렌트 시스템입니다.");
            System.out.println("   회원 인증을 해주세요!");
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
            } else if (isAdmin) {
                displayAdminMenu();
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
                } else if (isAdmin) {
                    executeAdminMenu(menu, adminService, scanner);
                } else {
                    executePostLoginMenu(menu, us, carRepository, rentalService, paymentService, scanner);
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
        System.out.println("👤 [" + loggedInUser.getUserId() + "님] 회원 관리 시스템 메뉴");
        System.out.println("-".repeat(40));
        System.out.println(" 1. 정보 조회 ");
        System.out.println(" 2. 정보 수정 ");
        System.out.println(" 3. 비밀번호 재설정 ");
        System.out.println(" 4. 카드 등록 ");
        System.out.println(" 5. 회원 탈퇴 ");
        System.out.println(" 6. 빌릴 수 있는 차량 조회 ");
        System.out.println(" 7. 차량 대여 ");
        System.out.println(" 8. 차량 반납 ");
        System.out.println(" 9. 결제 ");
        System.out.println(" 10. 로그아웃 ");
        System.out.println(" 0. 종료");
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
                System.out.println("✅ 회원가입이 완료되었습니다! (" + id + ")");
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
                    // 일반 회원가입과 동일하게 처리 (ID가 'admin'이면 관리자 권한)
                    User adminUser = us.signUp(id, pw, name, phone);
                    System.out.println("✅ 관리자 회원가입이 완료되었습니다! (" + adminUser.getUserId() + ")");
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
                    
                    // 관리자 여부 자동 확인 (userId가 "admin"인지 확인)
                    isAdmin = "admin".equals(loggedInUser.getUserId());
                    
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
                
                adminService.addCar(carName, type, fee);
                break;
                
            case 2: // 차량 삭제
                System.out.println("\n[2. 차량 삭제]");
                System.out.print("삭제할 차량 ID: "); 
                String deleteCarId = scanner.nextLine();
                adminService.deleteCar(deleteCarId);
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
    private static void executePostLoginMenu(int menu, UserService us, CarRepository carRepository,
                                            RentalService rentalService, PaymentService paymentService,
                                            Scanner scanner) {
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
                us.withdraw(currentId);
                System.out.println("✅ 회원 탈퇴가 완료되었습니다. (" + currentId + ")");
                loggedInUser = null; // ⭐️ 탈퇴 후 로그아웃 처리
                isAdmin = false;
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
                        System.out.printf("%d. [%s] %s (ID: %s) | 일일 요금: %s원%n", 
                            i + 1, car.type(), car.id(), car.id(), formatMoney(fee));
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
                    System.out.printf("%d. %s (ID: %s) | 일일 요금: %s원%n", 
                        i + 1, car.id(), car.id(), formatMoney(fee));
                }
                
                // 3) 차량 ID로 선택
                System.out.print("\n대여할 차량 ID 입력: ");
                String carId = scanner.nextLine().trim();
                Optional<Car> carOpt = cars.stream()
                    .filter(car -> car.id().equals(carId))
                    .findFirst();
                if (carOpt.isEmpty()) {
                    System.err.println("❌ 해당 ID의 차량을 찾을 수 없습니다.");
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
                
                // 5) 요금 정책 선택
                System.out.println("요금 정책 선택:");
                System.out.println("  1. 기본 요금 (BaseFeeStrategy)");
                System.out.println("  2. 성수기 요금 (PeakSeasonFeeStrategy) - 20% 할증");
                System.out.println("  3. 비수기 요금 (OffSeasonFeeStrategy) - 10% 할인");
                System.out.print("선택 (기본값: 1): ");
                String strategyChoice = scanner.nextLine().trim();
                FeeStrategy feeStrategy;
                if ("2".equals(strategyChoice)) {
                    feeStrategy = new PeakSeasonFeeStrategy();
                } else if ("3".equals(strategyChoice)) {
                    feeStrategy = new OffSeasonFeeStrategy();
                } else {
                    feeStrategy = new BaseFeeStrategy(); // 기본값
                }
                
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
                System.out.print("반납할 대여 ID: ");
                long rentalId = Long.parseLong(scanner.nextLine());
                
                // 메모리에서 대여 시 생성된 RentalRecord 조회 (정보 유지)
                RentalRecord cachedRecord = rentalRecordCache.get(rentalId);
                if (cachedRecord == null) {
                    // 메모리에 없으면 DB에서 조회 (하지만 정보가 불완전할 수 있음)
                    RentalRepository rentalRepo = new RentalRepository(new DBConnection());
                    Optional<RentalRecord> recordOpt = rentalRepo.findById(rentalId);
                    if (recordOpt.isEmpty()) {
                        System.err.println("❌ 대여 기록을 찾을 수 없습니다.");
                        break;
                    }
                    cachedRecord = recordOpt.get();
                }
                
                if (!currentId.equals(cachedRecord.getUserId())) {
                    System.err.println("❌ 본인의 대여 기록만 반납할 수 있습니다.");
                    break;
                }
                
                // 차량 조회
                String carIdStr = cachedRecord.getCarId();
                Car returnCar = carRepository.findById(carIdStr);
                if (returnCar == null) {
                    System.err.println("❌ 차량 정보를 찾을 수 없습니다.");
                    break;
                }
                
                try {
                    // 반납 전 사용자 등급 저장
                    User userBeforeReturn = us.getUserInfo(currentId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    String membershipBefore = userBeforeReturn.getUserMembershipStrategy().getClass().getSimpleName();
                    
                    // 반납 전에 대여 시 저장된 baseFee와 optionFee를 미리 저장 (RentalService에서 수정되기 전)
                    BigDecimal originalBaseFee = cachedRecord.getBaseFee();
                    BigDecimal originalOptionFee = cachedRecord.getOptionFee();
                    
                    // 반납 실행 (RentalService.returnCar는 long, Car만 받음)
                    rentalService.returnCar(rentalId, returnCar);
                    
                    // 반납 후 DB에서 다시 조회하여 업데이트된 정보 가져오기
                    RentalRepository rentalRepo = new RentalRepository(new DBConnection());
                    Optional<RentalRecord> returnedRecordOpt = rentalRepo.findById(rentalId);
                    RentalRecord returnedRecord = returnedRecordOpt.orElse(cachedRecord);
                    
                    // 디버깅: 반납 후 cachedRecord 값 확인
                    // System.out.println("DEBUG: after returnCar cachedRecord baseFee=" + cachedRecord.getBaseFee() + ", optionFee=" + cachedRecord.getOptionFee());
                    
                    // 반납 후 업데이트된 정보로 캐시 업데이트
                    // 대여 시 정보(baseFee, optionFee, feeStrategyType, options)는 유지
                    // 반납 시 계산된 penalty, discount, totalFee는 업데이트
                    // ⚠️ 중요: returnedRecord는 새로운 객체이므로 cachedRecord를 직접 수정하지 않음
                    // 대신 returnedRecord에 cachedRecord의 baseFee와 optionFee를 복사
                    returnedRecord.setBaseFee(cachedRecord.getBaseFee());
                    returnedRecord.setOptionFee(cachedRecord.getOptionFee());
                    returnedRecord.setFeeStrategyType(cachedRecord.getFeeStrategyType());
                    returnedRecord.setOptions(new ArrayList<>(cachedRecord.getOptions()));
                    // 캐시도 업데이트
                    cachedRecord.setPenalty(returnedRecord.getPenalty());
                    cachedRecord.setDiscount(returnedRecord.getDiscount());
                    cachedRecord.setTotalFee(returnedRecord.getTotalFee());
                    cachedRecord.setEndAt(returnedRecord.getEndAt());
                    cachedRecord.setStatus(returnedRecord.getStatus());
                    
                    // 차량 상태를 DB에 업데이트 (AVAILABLE로 변경)
                    returnCar.release();
                    carRepository.update(returnCar);
                    
                    System.out.println("\n✅ 반납이 성공적으로 완료되었습니다!");
                    System.out.println("\n반납 요금은 다음과 같습니다:\n");
                    
                    // 요금 명세서 출력 (대여 시와 동일한 형식)
                    // 대여 시 저장된 정보 사용
                    BigDecimal dailyFee = returnCar.getDailyRentalFee() != null ? 
                                         returnCar.getDailyRentalFee() : 
                                         returnCar.type().baseRate();
                    int returnRentalDays = returnedRecord.getRentalDays();
                    
                    // 대여 시 저장된 baseFee와 optionFee 사용 (반납 전에 미리 저장한 원본 값)
                    BigDecimal baseFee = originalBaseFee;
                    BigDecimal optionFee = originalOptionFee;
                    
                    // 요금 정책 재구성 (명세서 출력용)
                    FeeStrategy returnFeeStrategy;
                    String feeStrategyType = returnedRecord.getFeeStrategyType();
                    if (feeStrategyType == null || feeStrategyType.isEmpty()) {
                        returnFeeStrategy = new BaseFeeStrategy();
                    } else if ("PeakSeasonFeeStrategy".equals(feeStrategyType)) {
                        returnFeeStrategy = new PeakSeasonFeeStrategy();
                    } else if ("OffSeasonFeeStrategy".equals(feeStrategyType)) {
                        returnFeeStrategy = new OffSeasonFeeStrategy();
                    } else {
                        returnFeeStrategy = new BaseFeeStrategy();
                    }
                    
                    // 정책 적용 전 기본 요금 (명세서 출력용)
                    BigDecimal baseFeeBeforePolicy = dailyFee.multiply(new BigDecimal(returnRentalDays));
                    // 요금 정책 적용 금액 (할인/할증)
                    BigDecimal policyAdjustment = baseFee.subtract(baseFeeBeforePolicy);
                    String policyDescription = "";
                    if (returnFeeStrategy instanceof PeakSeasonFeeStrategy) {
                        policyDescription = " (20% 할증)";
                    } else if (returnFeeStrategy instanceof OffSeasonFeeStrategy) {
                        policyDescription = " (10% 할인)";
                    }
                    
                    // 옵션 비용 계산 (대여 시 저장된 옵션 정보 사용)
                    Map<String, BigDecimal> optionCosts = new HashMap<>();
                    List<String> returnOptions = returnedRecord.getOptions();
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
                    BigDecimal penalty = returnedRecord.getPenalty();
                    
                    // 회원 등급 할인
                    BigDecimal discount = returnedRecord.getDiscount();
                    
                    // 최종 요금
                    BigDecimal totalFee = returnedRecord.getTotalFee();
                    
                    // 요금 계산 과정 출력 (더 이해하기 쉽게)
                    System.out.println("--- [반납 요금 계산 내역] ---");
                    System.out.printf("차량: %s (%s)%n", returnCar.id(), returnCar.type());
                    System.out.printf("차량 일일 요금: %s원%n", formatMoney(dailyFee));
                    System.out.printf("대여 일수: %d일%n", returnRentalDays);
                    System.out.printf("요금 정책: %s%s%n", returnFeeStrategy.getClass().getSimpleName(), policyDescription);
                    
                    // 옵션 표시
                    if (!optionCosts.isEmpty()) {
                        System.out.print("옵션: ");
                        List<String> optionNames = new ArrayList<>(optionCosts.keySet());
                        for (int i = 0; i < optionNames.size(); i++) {
                            if (i > 0) System.out.print(", ");
                            System.out.print(optionNames.get(i));
                        }
                        System.out.println();
                    } else {
                        System.out.println("옵션: 없음");
                    }
                    System.out.println();
                    
                    // 대여 시 요금 계산 (대여 시 저장된 baseFee + optionFee 사용)
                    // baseFee는 이미 정책이 적용된 값이므로 그대로 사용
                    BigDecimal rentalFee = baseFee.add(optionFee);
                    if (penalty.compareTo(BigDecimal.ZERO) > 0) {
                        rentalFee = rentalFee.add(penalty);
                    }
                    
                    System.out.printf("대여 시 요금: %s원%n", formatMoney(rentalFee));
                    
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
                    User userAfterReturn = us.getUserInfo(currentId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
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
                
            case 9: // 결제
                System.out.println("\n[9. 결제]");
                System.out.print("결제할 대여 ID: ");
                long paymentRentalId = Long.parseLong(scanner.nextLine());
                
                // 대여 기록 조회
                RentalRepository paymentRentalRepo = new RentalRepository(new DBConnection());
                Optional<RentalRecord> paymentRecordOpt = paymentRentalRepo.findById(paymentRentalId);
                if (paymentRecordOpt.isEmpty()) {
                    System.err.println("❌ 대여 기록을 찾을 수 없습니다.");
                    break;
                }
                
                RentalRecord paymentRecord = paymentRecordOpt.get();
                if (!currentId.equals(paymentRecord.getUserId())) {
                    System.err.println("❌ 본인의 대여 기록만 결제할 수 있습니다.");
                    break;
                }
                
                // 차량 조회
                String paymentCarId = paymentRecord.getCarId();
                Car paymentCar = carRepository.findById(paymentCarId);
                if (paymentCar == null) {
                    System.err.println("❌ 차량 정보를 찾을 수 없습니다.");
                    break;
                }
                
                // 옵션 데코레이터 재구성 (간단화)
                FeeStrategy paymentFeeStrategy = new BaseFeeStrategy(); // 기본값
                // TODO: RentalComponent 재구성 필요
                
                try {
                    // PaymentService의 processPayment는 RentalComponent를 요구하므로
                    // 임시로 간단한 처리
                    System.out.println("⚠️ 결제 기능은 대여 시 자동으로 처리됩니다.");
                    System.out.println("대여 ID: " + paymentRecord.getId());
                    System.out.println("예상 총액: " + formatMoney(paymentRecord.getTotalFee()) + "원");
                } catch (Exception e) {
                    System.err.println("❌ 결제 실패: " + e.getMessage());
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