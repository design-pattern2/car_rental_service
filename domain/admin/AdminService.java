package domain.admin;

import db.DBConnection;
import domain.car.carFactory.CarStatus;
import domain.car.carFactory.CarType;
import domain.user.User;
import domain.user.UserService;

import java.math.BigDecimal;
import java.util.*;

/**
 * 관리자 전용 서비스.
 *
 * - "admin" 이라는 userId 를 가진 일반 회원 계정에게 관리자 권한을 부여하는 방식
 * - 로그인 로직은 UserService.login(...) 을 그대로 재사용
 * - 관리자 로그인 후:
 *      - 차량 DB 추가 / 삭제 (car 테이블)
 *      - 대여 이력 조회 (rental 테이블)
 *
 */
public class AdminService {

    // === 실제 DB 스키마에 맞춘 테이블 이름 ===
    private static final String CAR_TBL    = "car";
    private static final String RENTAL_TBL = "rental";
    // =====================================

    private final DBConnection db;
    private final UserService userService;

    // 현재 로그인된 관리자 상태
    private boolean adminLoggedIn = false;
    private String adminUserId; // 보통 "admin"

    public AdminService(DBConnection db, UserService userService) {
        this.db = db;
        this.userService = userService;
    }

    // =====================================================================
    // 1. 관리자 로그인 (UserService.login 재사용)
    // =====================================================================

    /**
     * 관리자 로그인.
     * - UserService.login(userId, pw) 로 먼저 일반 로그인 수행
     * - 그 결과 User 의 userId 가 "admin" 인 경우에만 관리자 로그인 성공으로 인정
     */
    public boolean loginAsAdmin(String userId, String rawPassword) {
        // 1) 일반 로그인 로직 재사용
        Optional<User> userOpt = userService.login(userId, rawPassword);

        if (userOpt.isEmpty()) {
            System.out.println("[관리자] 로그인 실패: 잘못된 ID 또는 비밀번호입니다.");
            return false;
        }

        User user = userOpt.get();

        // 2) 관리자 권한 확인: userId == "admin"
        if (!"admin".equals(user.getUserId())) {
            System.out.println("[관리자] 권한 없음: 관리자 계정이 아닙니다. (요청 ID=" + user.getUserId() + ")");
            return false;
        }

        this.adminLoggedIn = true;
        this.adminUserId = user.getUserId();

        System.out.println("[관리자] 관리자 로그인 성공! (" + adminUserId + ")");
        return true;
    }

    public void logout() {
        this.adminLoggedIn = false;
        this.adminUserId = null;
        System.out.println("[관리자] 로그아웃 되었습니다.");
    }

    private void ensureAdminLoggedIn() {
        if (!adminLoggedIn) {
            throw new IllegalStateException("관리자 로그인이 필요합니다. (admin 계정으로 로그인하세요)");
        }
    }

    // =====================================================================
    // 2. 차량 DB 추가 / 삭제 (car 테이블)
    // =====================================================================

    /**
     * 차량 등록 기능.
     *
     * car 테이블 예시 스키마:
     *  - id              VARCHAR(50)   PK
     *  - type            VARCHAR(20)   (SEDAN, SUV ...)
     *  - status          VARCHAR(20)   (AVAILABLE, UNAVAILABLE)
     *  - dailyRentalFee  DECIMAL(10,2)
     *  - name            VARCHAR(100)  차량 이름
     */
    public void addCar(String carId, CarType type, BigDecimal dailyRentalFee, String carName) {
        ensureAdminLoggedIn();

        if (carId == null || carId.isBlank()) {
            throw new IllegalArgumentException("carId 는 비어 있을 수 없습니다.");
        }
        Objects.requireNonNull(type, "CarType 은 null 일 수 없습니다.");

        // 1) ID 중복 체크
        String checkSql = "SELECT id FROM " + CAR_TBL + " WHERE id = :id";
        if (db.queryForObject(checkSql, Map.of("id", carId)).isPresent()) {
            System.out.println("[관리자] 이미 존재하는 차량 ID 입니다: " + carId);
            return;
        }

        // 2) 요금 결정: null 이면 CarType 기본 요금 사용
        BigDecimal fee = (dailyRentalFee != null) ? dailyRentalFee : type.baseRate();
        
        // 3) 이름 결정: null이거나 비어있으면 carId 사용
        String name = (carName != null && !carName.trim().isEmpty()) ? carName : carId;

        String insertSql =
                "INSERT INTO " + CAR_TBL +
                        " (id, type, status, dailyRentalFee, name) " +
                        "VALUES (:id, :type, :status, :dailyRentalFee, :name)";

        Map<String, Object> params = new HashMap<>();
        params.put("id", carId);
        params.put("type", type.name());
        params.put("status", CarStatus.AVAILABLE.name());
        params.put("dailyRentalFee", fee);
        params.put("name", name);

        int rows = db.execute(insertSql, params);
        if (rows > 0) {
            System.out.println("[관리자] 차량 등록 완료 -> ID=" + carId +
                    ", 이름=" + name +
                    ", 타입=" + type +
                    ", 1일 대여료=" + fee);
        } else {
            System.out.println("[관리자] 차량 등록 실패 (영향 받은 행 없음)");
        }
    }

    /**
     * 차량 삭제 기능.
     */
    public void deleteCar(String carId) {
        ensureAdminLoggedIn();

        String sql = "DELETE FROM " + CAR_TBL + " WHERE id = :id";
        int rows = db.execute(sql, Map.of("id", carId));

        if (rows > 0) {
            System.out.println("[관리자] 차량 삭제 완료 -> ID=" + carId);
        } else {
            System.out.println("[관리자] 삭제할 차량이 존재하지 않습니다 -> ID=" + carId);
        }
    }

    /**
     * 현재 등록된 모든 차량 조회.
     */
    public void printAllCars() {
        ensureAdminLoggedIn();

        String sql = "SELECT id, type, status, dailyRentalFee FROM " + CAR_TBL + " ORDER BY id";
        var rows = db.queryForList(sql, Map.of());

        if (rows.isEmpty()) {
            System.out.println("[관리자] 등록된 차량이 없습니다.");
            return;
        }

        System.out.println("==== 차량 목록 ====");
        for (Map<String, Object> row : rows) {
            String id = Objects.toString(row.get("id"), "");
            String type = Objects.toString(row.get("type"), "");
            String status = Objects.toString(row.get("status"), "");
            Object fee = row.get("dailyRentalFee");

            System.out.printf("- ID=%s, 타입=%s, 상태=%s, 1일 대여료=%s%n",
                    id, type, status, fee);
        }
    }

    // =====================================================================
    // 3. 대여 DB 조회 (rental 테이블)
    // =====================================================================

    /**
     * rental 테이블 전체 대여 이력을 조회해서 콘솔에 출력.
     *
     * rental 테이블 예시 스키마:
     *  - id        BIGINT PK
     *  - userId    VARCHAR
     *  - carId     VARCHAR
     *  - startTime DATETIME
     *  - endTime   DATETIME
     *  
     */
    public void printAllRentalRecords() {
        ensureAdminLoggedIn();

        String sql = "SELECT * FROM " + RENTAL_TBL + " ORDER BY id DESC";
        var rows = db.queryForList(sql, Map.of());

        if (rows.isEmpty()) {
            System.out.println("[관리자] 등록된 대여 이력이 없습니다.");
            return;
        }

        System.out.println("==== 전체 대여 이력 ====");
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("id");
            long id = (idObj instanceof Number) ? ((Number) idObj).longValue() : 0L;

            String userId = Objects.toString(row.get("userId"), "");
            String carId  = Objects.toString(row.get("carId"), "");
            Object start  = row.get("startTime");
            Object end    = row.get("endTime");

            System.out.printf("#%d | user=%s, car=%s, 시작=%s, 종료=%s%n",
                    id, userId, carId, start, end);
        }
    }
}
