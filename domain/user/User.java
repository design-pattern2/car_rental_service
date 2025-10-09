package domain.user;

import domain.user.strategy.UserMembershipStrategy;

import java.math.BigDecimal;

public class User {
    private String userId;
    private String password;
    private String name;
    private String phoneNumber;
    private String cardNumber;
    private UserMembershipStrategy userMembershipStrategy; // ✅ 할인 전략 객체만 유지

    // 생성자: 사용자 정보와 함께 사용할 전략 객체를 바로 주입받음(사용횟수 따라도 될듯)
    public User(String userId, String password, String name, String phoneNumber,String cardNumber, UserMembershipStrategy strategy) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.userMembershipStrategy = strategy;
        this.cardNumber = cardNumber;
    }

    // Getter
    public String getUserId() { return userId; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getPhoneNumber(){return phoneNumber;}
    public UserMembershipStrategy getUserMembershipStrategy(){return userMembershipStrategy;}
    public String getCardNumber() { return cardNumber; }


    public void updateName(String name) { this.name = name; }
    public void updatephoneNumber(String phoneNumber){this.phoneNumber = phoneNumber;}
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
    public void updateCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    // 💡 동적으로 전략을 변경할 수 있도록 Setter를 제공할 수도 있음
    public void setDiscountStrategy(UserMembershipStrategy discountStrategy) {
        this.userMembershipStrategy = discountStrategy;
    }

    /**
     * 전략을 사용하여 할인 금액을 계산합니다.
     */
    public BigDecimal applyDiscount(BigDecimal originalAmount) {
        // 할인 전략에 로직 실행을 위임
        return this.userMembershipStrategy.calculateDiscount(originalAmount);
    }

    @Override
    public String toString() {
        return "User {" +
                "ID: '" + userId + '\'' +
                ", 이름: '" + name + '\'' +
                ", 전화번호: '"+phoneNumber+'\''+
                ", 등급: " + userMembershipStrategy.name() +
                '}';
    }
}