package domain.rental;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RentalRecord {
    public enum Status { RENTED, RETURNED }

    private Long id;
    private String userId;          // 로그인 식별자(문자열 기준)
    private String carId;           // 차량 식별자
    private int rentalDays;         // 대여 일수
    private LocalDateTime startAt;  // 대여 시작
    private LocalDateTime endAt;    // 반납 시각
    private Status status = Status.RENTED;

    // 전략/옵션 메타
    private String feeStrategyType;        // ex) PeakSeasonFeeStrategy
    private String membershipStrategyType; // ex) GoldStrategy
    private List<String> options = new ArrayList<>();

    // 금액 요약
    private BigDecimal baseFee   = BigDecimal.ZERO; // 정책 적용 기본료(일수 포함)
    private BigDecimal optionFee = BigDecimal.ZERO; // 옵션 총액
    private BigDecimal discount  = BigDecimal.ZERO; // 멤버십 할인액(결제 시 확정)
    private BigDecimal penalty   = BigDecimal.ZERO; // 연체 패널티(반납 시 확정)
    private BigDecimal totalFee  = BigDecimal.ZERO; // 최종 결제액

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCarId() { return carId; }
    public void setCarId(String carId) { this.carId = carId; }

    public int getRentalDays() { return rentalDays; }
    public void setRentalDays(int rentalDays) { this.rentalDays = rentalDays; }

    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }

    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getFeeStrategyType() { return feeStrategyType; }
    public void setFeeStrategyType(String feeStrategyType) { this.feeStrategyType = feeStrategyType; }

    public String getMembershipStrategyType() { return membershipStrategyType; }
    public void setMembershipStrategyType(String membershipStrategyType) { this.membershipStrategyType = membershipStrategyType; }

    public List<String> getOptions() { return Collections.unmodifiableList(options); }
    public void setOptions(List<String> options) {
        this.options.clear();
        if (options != null) this.options.addAll(options);
    }

    public BigDecimal getBaseFee() { return baseFee; }
    public void setBaseFee(BigDecimal baseFee) { this.baseFee = baseFee == null ? BigDecimal.ZERO : baseFee; }

    public BigDecimal getOptionFee() { return optionFee; }
    public void setOptionFee(BigDecimal optionFee) { this.optionFee = optionFee == null ? BigDecimal.ZERO : optionFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount == null ? BigDecimal.ZERO : discount; }

    public BigDecimal getPenalty() { return penalty; }
    public void setPenalty(BigDecimal penalty) { this.penalty = penalty == null ? BigDecimal.ZERO : penalty; }

    public BigDecimal getTotalFee() { return totalFee; }
    public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee == null ? BigDecimal.ZERO : totalFee; }
}
