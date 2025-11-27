package domain.user;

import db.DBConnection; // 외부 DB 통신 클래스 임포트
import domain.user.strategy.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepository {

    // DB 통신을 담당하는 외부 클래스에 의존
    private final DBConnection dbConnection;

    public UserRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // =================================================================
    // 1. 저장 및 업데이트 (DB 내부 ID 유무에 따라 INSERT/UPDATE 분리)
    // =================================================================
    public User save(User user) {
        // User 객체의 DB 내부 ID(Long id)를 확인하여 Insert 또는 Update 결정
        if (user.getId() != 0) {
            // DB ID가 존재하는 경우: 기존 레코드 수정 (UPDATE)
            return update(user);
        } else {
            // DB ID가 존재하지 않는 경우: 새로운 레코드 삽입 (INSERT)
            return insert(user);
        }
    }

    /**
     * 새로운 사용자 레코드를 DB에 삽입하고, 생성된 ID를 User 객체에 설정합니다.
     */
    private User insert(User user) {
        Map<String, Object> params = mapUserToDbData(user);

        // 쿼리 작성: id 컬럼은 DB에서 자동 생성되므로 쿼리에 포함하지 않습니다.
        String sql = "INSERT INTO user (userId, pw, name, phoneNumber, cardNumber, membership) " +
                "VALUES (:userId, :pw, :name, :phoneNumber, :cardNumber, :membership)";

        // DB 실행: 삽입 후 DB에서 자동 생성된 PK(id) 값을 반환받습니다.
        int generatedId = dbConnection.executeAndReturnKey(sql, params);

        // User 객체에 생성된 DB ID를 설정하여, 다음 save 호출 시 Update가 되도록 준비
        user.updateId(generatedId);
        return user;
    }

    /**
     * 기존 사용자 레코드를 DB에 업데이트합니다.
     */
    private User update(User user) {
        // 매핑된 파라미터 맵 복사본을 만들어 id를 추가 (WHERE 절 사용)
        Map<String, Object> updateParams = new HashMap<>(mapUserToDbData(user));
        updateParams.put("id", user.getId());

        // UPDATE 쿼리 작성: 내부 DB ID를 WHERE 조건으로 사용합니다.
        String sql = "UPDATE user SET " +
                "pw = :pw, name = :name, phoneNumber = :phoneNumber, " +
                "cardNumber = :cardNumber, membership = :membership " +
                "WHERE id = :id";

        // DBConnection에 쿼리 실행 위임
        dbConnection.execute(sql, updateParams);
        return user;
    }
// =================================================================
    // 2. ID 기반 조회 (userId 기반 조회)
    // =================================================================
    /**
     * 사용자 로그인 ID(userId)를 기반으로 사용자 정보를 조회합니다.
     */
    public Optional<User> findByUserId(String userId) {
        String sql = "SELECT id, userId, pw, name, phoneNumber, cardNumber, membership " +
                "FROM user WHERE userId = :userId";
        Map<String, Object> params = Map.of("userId", userId);

        // 1. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        Optional<Map<String, Object>> dbDataOpt = dbConnection.queryForObject(sql, params);

        // 2. DB 데이터(Map) -> User 객체로 변환 (매핑) 및 반환
        return dbDataOpt.flatMap(this::mapDbDataToUser);
    }
    // =================================================================
    // 3. 전화번호 기반 조회 (쿼리 작성 후 DBConnection에 위임)
    // =================================================================
    /**
     * 전화번호를 기반으로 사용자 정보를 조회합니다.
     */
    public Optional<User> findByPhoneNumber(String phoneNumber) {
        String sql = "SELECT id, userId, pw, name, phoneNumber, cardNumber, membership " +
                "FROM user WHERE phoneNumber = :phoneNumber";
        Map<String, Object> params = Map.of("phoneNumber", phoneNumber);

        // 1. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        Optional<Map<String, Object>> dbDataOpt = dbConnection.queryForObject(sql, params);

        // 2. DB 데이터(Map) -> User 객체로 변환 (매핑) 및 반환
        return dbDataOpt.flatMap(this::mapDbDataToUser);
    }

    // =================================================================
    // 3-1. 이름 기반 조회 (중복 확인용)
    // =================================================================
    /**
     * 이름을 기반으로 사용자 정보를 조회합니다.
     */
    public Optional<User> findByName(String name) {
        String sql = "SELECT id, userId, pw, name, phoneNumber, cardNumber, membership " +
                "FROM user WHERE name = :name";
        Map<String, Object> params = Map.of("name", name);

        // 1. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        Optional<Map<String, Object>> dbDataOpt = dbConnection.queryForObject(sql, params);

        // 2. DB 데이터(Map) -> User 객체로 변환 (매핑) 및 반환
        return dbDataOpt.flatMap(this::mapDbDataToUser);
    }
    // =================================================================
    // 4. 삭제 (쿼리 작성 후 DBConnection에 위임)
    // =================================================================
    public boolean delete(String userId) {
        String sql = "DELETE FROM user WHERE userId = :userId";
        Map<String, Object> params = Map.of("userId", userId);

        // DBConnection.execute(쿼리, 파라미터)는 변경된 행 수를 반환한다고 가정
        int affectedRows = dbConnection.execute(sql, params);

        return affectedRows > 0;
    }
    // =================================================================
    // 5. 카드 등록 (로직 + 쿼리 작성 후 DBConnection에 위임)
    // =================================================================
    public User registerCard(String userId, String cardNumber) {
        // 1. 사용자 조회 (기존 findById 재사용)
        User user = findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 비즈니스 로직: 카드 번호 설정
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new IllegalArgumentException("유효한 카드 번호가 필요합니다.");
        }
        user.updateCardNumber(cardNumber);

        // 3. 데이터 접근 위임: UPDATE 쿼리로 변경된 필드만 업데이트
        String sql = "UPDATE user SET cardNumber = :cardNumber WHERE userId = :userId";
        Map<String, Object> params = Map.of(
                "cardNumber", user.getCardNumber(),
                "userId", user.getUserId()
        );

        dbConnection.execute(sql, params); // execute(쿼리, 파라미터) 메서드 가정

        return user;
    }
    // =================================================================
    // 매핑 로직 (UserRepository의 핵심 책임 중 하나)
    // =================================================================

    // User 객체를 쿼리 파라미터(Map) 형식으로 변환
    private Map<String, Object> mapUserToDbData(User user) {
        Map<String, Object> dbData = new HashMap<>();

        // DB 스키마: id, userId, pw, name, phoneNumber, cardNumber, membership
        dbData.put("userId", user.getUserId());
        dbData.put("pw", user.getPassword());
        dbData.put("name", user.getName());
        dbData.put("phoneNumber", user.getPhoneNumber());
        dbData.put("cardNumber", user.getCardNumber());
        
        // membership 필드가 설정되어 있으면 그것을 사용, 없으면 전략 이름 사용
        String membership = user.getMembership();
        if (membership != null && !membership.isEmpty()) {
            dbData.put("membership", membership);
        } else {
            dbData.put("membership", user.getUserMembershipStrategy().name());
        }

        return dbData;
    }

    // DB 데이터(Map)를 User 객체로 변환
// DB 데이터(Map)를 User 객체로 변환 (DB 컬럼 이름 기반 추출)
    private Optional<User> mapDbDataToUser(Map<String, Object> data) {
        // DB 내부 ID 추출 (int 타입)
        int id = (int) data.get("id");

        // 나머지 컬럼 데이터 추출
        String userId = (String) data.get("userId");
        String password = (String) data.get("pw");
        String name = (String) data.get("name");
        String phoneNumber = (String) data.get("phoneNumber");
        String cardNumber = (String) data.get("cardNumber");
        String membershipType = (String) data.get("membership");

        if (userId == null) {
            return Optional.empty();
        }

        // 저장된 전략 타입 문자열을 기반으로 실제 전략 객체를 복원
        UserMembershipStrategy strategy = createStrategyByType(membershipType);

        // User 객체 생성 (id를 제외한 초기 필수값 사용 가정)
        User user = new User(userId, password, name, phoneNumber, cardNumber, strategy);

        // DB에서 가져온 id를 setter를 통해 설정 (필요 시 User 클래스에 setId 필요)
        if (id != 0) {
            user.updateId(id);
        }
        
        // membership 값 설정 (관리자 판별을 위해)
        user.setMembership(membershipType);

        return Optional.of(user);
    }

    // 저장된 전략 이름에 따라 전략 객체를 생성하는 헬퍼 메서드
    private UserMembershipStrategy createStrategyByType(String typeName) {
        // ADMIN은 전략이 아니므로 기본 전략(Silver)을 반환
        if (typeName != null && typeName.equals("ADMIN")) {
            return new SilverStrategy(); // ADMIN도 기본 전략 사용
        }
        if (typeName != null && typeName.contains("Gold")) {
            return new GoldStrategy();
        }
        if (typeName != null && typeName.contains("Platinum")) {
            return new PlatinumStrategy();
        }
        if (typeName != null && typeName.contains("VIP")) {
            return new VIPStrategy();
        }
        return new SilverStrategy();
    }
}
