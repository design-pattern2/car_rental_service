package domain.user;

import db.DBConnection; // 외부 DB 통신 클래스 임포트
import domain.user.strategy.*;

import java.util.Map;
import java.util.Optional;

public class UserRepository {

    // DB 통신을 담당하는 외부 클래스에 의존
    private final DBConnection dbConnection;

    public UserRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // =================================================================
    // 1. 저장 및 업데이트 (DBConnection에 위임)
    // =================================================================
    public User save(User user) {
        // 1. User 객체 -> DB 데이터 형식(Map)으로 변환 (매핑)
        Map<String, Object> userData = mapUserToDbData(user);

        // 2. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        dbConnection.saveRecord(userData);

        return user;
    }

    // =================================================================
    // 2. ID 기반 조회 (DBConnection에 위임)
    // =================================================================
    public Optional<User> findById(String userId) {

        // 1. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        Optional<Map<String, Object>> dbDataOpt =
                dbConnection.findRecordById(userId);

        // 2. DB 데이터(Map) -> User 객체로 변환 (매핑) 및 반환
        return dbDataOpt.flatMap(this::mapDbDataToUser);
    }
    // =================================================================
    // 3. 전화번호 기반 조회 (DBConnection에 위임)
    // =================================================================
    public Optional<User> findByPhoneNumber(String phoneNumber) {

        // 1. DBConnection의 실행 메서드 호출 (DB 통신 위임)
        // DBConnection.executeQueryFindOne은 쿼리와 조회할 파라미터를 받습니다.
        Optional<Map<String, Object>> dbDataOpt =
                dbConnection.findRecordByPhoneNumber(phoneNumber);

        // 2. DB 데이터(Map) -> User 객체로 변환 (매핑) 및 반환
        return dbDataOpt.flatMap(this::mapDbDataToUser);
    }
    // =================================================================
    // 4. 삭제 (DBConnection에 위임)
    // =================================================================
    public boolean delete(String userId) {
        //DBConnection의 실행 메서드 호출 (DB 통신 위임)
        int affectedRows = dbConnection.deleteRecordById(Map.of("user_id", userId));

        return affectedRows > 0;
    }
    // =================================================================
    // 5. 카드 등록 (DBConnection에 위임)
    // =================================================================
    public User registerCard(String userId, String cardNumber) {
        // 1. 사용자 조회
        User user = findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 2. 비즈니스 로직: 카드 번호 설정 (실제로는 유효성 검사 필요)
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new IllegalArgumentException("유효한 카드 번호가 필요합니다.");
        }
        user.updateCardNumber(cardNumber);

        // 3. 데이터 접근 위임: 변경된 User 객체를 DB에 저장 (카드 정보 업데이트)
        return save(user);
    }
    // =================================================================
    // 매핑 로직 (UserRepository의 핵심 책임 중 하나)
    // =================================================================

    // User 객체를 DB 테이블의 컬럼 형식(Map)으로 변환
    private Map<String, Object> mapUserToDbData(User user) {
        return Map.of(
                "user_id", user.getUserId(),
                "password_hash", user.getPassword(),
                "name", user.getName(),
                "phoneNumber",user.getPhoneNumber(),
                "card_number", user.getCardNumber(),
                "strategy_type", user.getUserMembershipStrategy().name()
        );
    }

    // DB 데이터(Map)를 User 객체로 변환
    private Optional<User> mapDbDataToUser(Map<String, Object> data) {
        // DB 컬럼 이름을 기반으로 데이터를 추출
        String userId = (String) data.get("user_id");
        String password = (String) data.get("password_hash");
        String name = (String) data.get("name");
        String phoneNumber = (String)data.get("phoneNumber");
        String cardNumber = (String) data.get("card_number");
        String strategyType = (String) data.get("discount_strategy_type");

        // 저장된 전략 타입 문자열을 기반으로 실제 전략 객체를 다시 생성 (복원)
        UserMembershipStrategy strategy = createStrategyByType(strategyType);

        if (userId == null || strategy == null) {
            return Optional.empty();
        }

        // 복원된 User 객체 반환
        return Optional.of(new User(userId, password, name, phoneNumber, cardNumber,strategy));
    }

    // 저장된 전략 이름에 따라 전략 객체를 생성하는 헬퍼 메서드
    private UserMembershipStrategy createStrategyByType(String typeName) {
        if (typeName != null && typeName.contains("Gold")) {
            return new GoldStrategy();
        }
        if (typeName != null && typeName.contains("Platinum")) {
            return new PlatinumStrategy();
        }
        if (typeName != null && typeName.contains("Vip")) {
            return new VIPStrategy();
        }
        return new SilverStrategy();
    }
}