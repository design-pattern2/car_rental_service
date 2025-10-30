package domain.user;

import domain.user.strategy.*;
import java.util.Optional;

/**
 * 회원 관리의 핵심 비즈니스 로직을 처리하는 서비스.
 * 모든 데이터 접근은 UserRepository에 위임합니다.
 */
public class UserService {

    // UserRepository에 의존하며, 생성자를 통해 주입받습니다.
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 새로운 사용자의 기본 전략을 결정하는 헬퍼 메서드 (전략 선택 역할)
    private UserMembershipStrategy getDefaultStrategy() {
        return new SilverStrategy();
    }

    // =================================================================
    // 1. 회원가입: signUp(userId, rawPassword, name, phoneNumber)
    // =================================================================

    public User signUp(String userId, String rawPassword, String name, String phoneNumber) {
        // 1. 비즈니스 규칙: ID 중복 확인
        if (userRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자 ID입니다.");
        }

        // 2. ✅ 비즈니스 규칙: 전화번호 중복 확인 (하나의 전화번호당 하나의 ID만 허용)
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("이미 해당 전화번호로 가입된 계정이 존재합니다.");
        }

        // 3. 비즈니스 로직: 비밀번호 해싱
        String passwordHash = hashPassword(rawPassword); // hashPassword는 Bcrypt 시뮬레이션 로직을 사용한다고 가정

        // 4. 비즈니스 로직: 전략 객체 결정 및 주입
        UserMembershipStrategy initialStrategy = getDefaultStrategy();

        // 5. User 객체 생성
        User newUser = new User(userId, passwordHash, name, phoneNumber, null, initialStrategy);

        // 6. 데이터 접근 위임: UserRepository에 저장 요청
        return userRepository.save(newUser);
    }
    // =================================================================
    // 2. 로그인: login(userId, rawPassword)
    // =================================================================
    public Optional<User> login(String userId, String rawPassword) {
        // 1. 데이터 접근 위임: ID로 사용자 조회
        Optional<User> userOpt = userRepository.findByUserId(userId);

        if (userOpt.isEmpty()) {
            return Optional.empty(); // 사용자 없음
        }

        User user = userOpt.get();

        // 2. 비즈니스 로직: 비밀번호 일치 확인
        if (verifyPassword(rawPassword, user.getPassword())) {
            return Optional.of(user); // 로그인 성공
        } else {
            return Optional.empty(); // 비밀번호 불일치
        }
    }

    // =================================================================
    // 3. 정보 조회: getUserInfo(String userId)
    // =================================================================
    public Optional<User> getUserInfo(String userId) {
        // 데이터 접근 위임: Repository의 findById 메서드를 호출하여 User 객체 반환
        return userRepository.findByUserId(userId);
    }

    // =================================================================
    // 4. 정보 수정: updateUserInfo(userId, newName, newPassword)
    // =================================================================
    public User updateUserInfo(String userId, String newName, String newPassword, String newPhoneNumber) {
        // 1. 데이터 접근 위임: 수정할 기존 사용자 정보 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (userRepository.findByPhoneNumber(newPhoneNumber).isPresent()) {
            throw new IllegalArgumentException("이미 해당 전화번호로 가입된 계정이 존재합니다.");
        }
        // 2. 비즈니스 로직: User 객체 상태 변경
        if (newPhoneNumber != null && !newPhoneNumber.trim().isEmpty()) {
            user.updatephoneNumber(newPhoneNumber);
        }
        if (newName != null && !newName.trim().isEmpty()) {
            user.updateName(newName);
        }

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            String newPasswordHash = hashPassword(newPassword);
            user.updatePassword(newPasswordHash);
        }




        // 3. 데이터 접근 위임: 변경된 User 객체를 Repository에 저장 요청 (DB 업데이트)
        return userRepository.save(user);
    }

    // =================================================================
    // 5. 아이디 및 비밀번호 찾기 (간소화) --회원정보
    // =================================================================
    public String findPasswordResetLink(String userId) {
        // ID 존재 여부 확인을 Repository에 위임
        return userRepository.findByUserId(userId)
                .map(u -> "비밀번호 재설정 링크를 발송했습니다.")
                .orElse("해당 ID로 등록된 정보가 없습니다.");
    }

    // =================================================================
    // 6. 회원 탈퇴: withdraw(userId)
    // =================================================================
    public void withdraw(String userId) {
        // 데이터 접근 위임: Repository에 삭제 요청
        boolean deleted = userRepository.delete(userId);
        if (!deleted) {
            throw new IllegalArgumentException("탈퇴할 사용자가 존재하지 않습니다.");
        }
    }

    // =================================================================
    // 7. 전략 패턴 활용 예시: VIP 승급 (비즈니스 로직)
    // =================================================================
    public User upgradeGrade(String userId, int i) {
        // 1. 데이터 접근 위임: 사용자 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        UserMembershipStrategy newStrategy = switch (i) {
            case 1 -> new SilverStrategy();
            case 2 -> new GoldStrategy();
            case 3 -> new PlatinumStrategy();
            case 4 -> new VIPStrategy();
            default ->
                    throw new IllegalArgumentException("유효하지 않은 등급 번호(" + i + ")입니다. (1=Silver, 2=Gold, 3=Platinum, 4=VIP)");
        };
        // 2. 비즈니스 로직: VIP 전략으로 교체 (전략 패턴 Context 업데이트)

        // 3. 데이터 접근 위임: 변경된 User 객체를 저장 (DB 업데이트)
        return userRepository.save(user);
    }
    // =================================================================
    // 8. 카드 등록: registerCard(userId, cardNumber)
    // =================================================================
    public User registerCard(String userId, String cardNumber) {
        // 모든 비즈니스 규칙(유효성 검사, DB 업데이트)을 Repository에 위임합니다.
        // 현재는 복잡한 도메인 로직이 없으므로 단순 위임합니다.

        // 1. 사용자 존재 유무 확인 및 업데이트 로직을 Repository에 요청
        return userRepository.registerCard(userId, cardNumber);
    }

    // =================================================================
    // Utility 메서드 (보안 로직 가정)
    // =================================================================
    private final String BCRYPT_PREFIX = "$2a$10$";
    private String hashPassword(String rawPassword) {
        // 실제 구현은 보안 라이브러리를 사용해야 합니다.
        // 현재는 가상의 솔트와 rawPassword 해시를 조합하여 해시된 문자열처럼 만듭니다.
        String salt = "salt1234567890";
        return BCRYPT_PREFIX + salt + rawPassword.hashCode();
    }
    private boolean verifyPassword(String rawPassword, String passwordHash) {
        // 실제 구현은 보안 라이브러리를 사용해야 합니다.
        String hashedRawPassword = hashPassword(rawPassword);
        return hashedRawPassword.equals(passwordHash);
    }
}