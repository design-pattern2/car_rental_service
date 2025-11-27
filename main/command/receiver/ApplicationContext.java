package main.command.receiver;

import domain.admin.AdminService;
import domain.car.CarRepository;
import domain.payment.PaymentService;
import domain.rental.RentalRepository;
import domain.rental.RentalService;
import domain.user.User;
import domain.user.UserService;
import db.DBConnection;

import java.util.HashMap;
import java.util.Map;

/**
 * Command Pattern: Receiver
 * 실제 비즈니스 로직을 수행하는 서비스들을 관리하는 컨텍스트
 */
public class ApplicationContext {
    private final UserService userService;
    private final AdminService adminService;
    private final CarRepository carRepository;
    private final RentalService rentalService;
    private final RentalRepository rentalRepository;
    private final PaymentService paymentService;
    
    // 애플리케이션 상태
    private User loggedInUser = null;
    private boolean isAdmin = false;
    private final Map<Long, domain.rental.RentalRecord> rentalRecordCache = new HashMap<>();
    private domain.payment.strategy.FeeStrategy currentSeason = new domain.payment.strategy.BaseFeeStrategy();
    
    public ApplicationContext(UserService userService, AdminService adminService,
                             CarRepository carRepository, RentalService rentalService,
                             PaymentService paymentService) {
        this.userService = userService;
        this.adminService = adminService;
        this.carRepository = carRepository;
        this.rentalService = rentalService;
        this.rentalRepository = new RentalRepository(new DBConnection());
        this.paymentService = paymentService;
    }
    
    // Getters
    public UserService getUserService() { return userService; }
    public AdminService getAdminService() { return adminService; }
    public CarRepository getCarRepository() { return carRepository; }
    public RentalService getRentalService() { return rentalService; }
    public RentalRepository getRentalRepository() { return rentalRepository; }
    public PaymentService getPaymentService() { return paymentService; }
    
    public User getLoggedInUser() { return loggedInUser; }
    public void setLoggedInUser(User user) { 
        this.loggedInUser = user;
        // 관리자 여부 자동 확인
        if (user != null) {
            String membership = user.getMembership();
            this.isAdmin = membership != null && membership.equals("ADMIN");
        } else {
            this.isAdmin = false;
        }
    }
    
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { this.isAdmin = admin; }
    
    public Map<Long, domain.rental.RentalRecord> getRentalRecordCache() { return rentalRecordCache; }
    
    public domain.payment.strategy.FeeStrategy getCurrentSeason() { return currentSeason; }
    public void setCurrentSeason(domain.payment.strategy.FeeStrategy season) { this.currentSeason = season; }
    
    /**
     * BigDecimal을 정수 문자열로 변환 (소수점 제거)
     */
    public String formatMoney(java.math.BigDecimal amount) {
        if (amount == null) return "0";
        return String.valueOf(amount.setScale(0, java.math.RoundingMode.HALF_UP).intValue());
    }
}

