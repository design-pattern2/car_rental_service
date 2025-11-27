package domain.rental;

import db.DBConnection;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * rental 테이블과 상호작용하는 리포지토리.
 *  - README의 MySQL 스키마에 맞춰 userId/carId는 정수 PK를 사용
 *  - 비즈니스 계층(RentalService)에서는 RentalRecord를 통해 도메인 정보를 주고받음
 */
public class RentalRepository {
    private static final String TBL = "rental";

    private final DBConnection db;

    public RentalRepository(DBConnection db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    /** carId(INT) 기준으로 'RENTED' 상태인 활성 대여가 있는지 확인 */
    public Optional<RentalRecord> findActiveByCarId(int carId) {
        String sql = "SELECT * FROM " + TBL + " WHERE carId=:carId AND status='RENTED' LIMIT 1";
        Map<String, Object> p = Map.of("carId", carId);
        return db.queryForObject(sql, p).map(this::mapRowToRecord);
    }

    /** PK로 단건 조회 + user 조인으로 로그인 아이디(user.userId)까지 가져오기 */
    public Optional<RentalRecord> findById(long id) {
        String sql =
                "SELECT r.*, u.userId AS loginUserId " +
                "FROM " + TBL + " r " +
                "JOIN user u ON r.userId = u.id " +
                "WHERE r.id = :id";

        return db.queryForObject(sql, Map.of("id", id)).map(this::mapRowToRecord);
    }

    /**
     * 대여 저장 (README의 rental 테이블)
     *  - startTime: now
     *  - endTime  : 예약 종료( startTime + rentalDays )  ← NOT NULL 제약 충족
     *  - status   : 'RENTED'
     */
    public long save(int userId, int carId, RentalRecord r) {
        String sql =
                "INSERT INTO " + TBL + " (userId, carId, startTime, endTime, status) " +
                "VALUES (:userId, :carId, :startTime, :endTime, :status)";

        LocalDateTime start = r.getStartAt();
        LocalDateTime scheduledEnd = start.plusDays(r.getRentalDays());

        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("carId", carId);
        p.put("startTime", Timestamp.valueOf(start));
        p.put("endTime", Timestamp.valueOf(scheduledEnd));
        p.put("status", "RENTED");

        int generatedId = db.executeAndReturnKey(sql, p);
        r.setId((long) generatedId);
        return r.getId();
    }

    /** 반납 처리: status='RETURNED', endTime=현재시각. 현재 상태가 RENTED일 때만 반납 처리. */
    public boolean markReturnedIfRented(long id) {
        String sql = "UPDATE " + TBL + " SET status='RETURNED', endTime=CURRENT_TIMESTAMP " +
                     "WHERE id=:id AND status='RENTED'";
        int affected = db.execute(sql, Map.of("id", id));
        return affected > 0;
    }

    // ===== 내부 매핑 =====
    private RentalRecord mapRowToRecord(Map<String, Object> row) {
        RentalRecord rec = new RentalRecord();

        Object idObj = row.get("id");
        if (idObj != null) {
            rec.setId(((Number) idObj).longValue());
        }

        // loginUserId 별칭이 존재하면 로그인 아이디로 사용
        Object loginUserIdObj = row.get("loginUserId");
        if (loginUserIdObj != null) {
            rec.setUserId(String.valueOf(loginUserIdObj));
        } else {
            // loginUserId가 없으면 Fallback: 정수 FK 그대로 문자열로
            Object userIdObj = row.get("userId");
            if (userIdObj != null) {
                rec.setUserId(String.valueOf(((Number) userIdObj).intValue()));
            }
        }

        Object carIdObj = row.get("carId");
        if (carIdObj != null) {
            rec.setCarId(String.valueOf(((Number) carIdObj).intValue()));
        }

        LocalDateTime start = toLdt(row.get("startTime"));
        LocalDateTime end   = toLdt(row.get("endTime"));
        rec.setStartAt(start);
        rec.setEndAt(end);

        String st = String.valueOf(row.get("status"));
        rec.setStatus("RETURNED".equalsIgnoreCase(st) ? RentalRecord.Status.RETURNED
                                                      : RentalRecord.Status.RENTED);

        if (start != null && end != null) {
            long days = Math.max(1, Duration.between(start, end).toDays());
            rec.setRentalDays((int) days);
        }

        // 요금 관련 필드는 DB에 아직 없으므로 도메인 레벨에서만 사용
        rec.setBaseFee(BigDecimal.ZERO);
        rec.setOptionFee(BigDecimal.ZERO);
        rec.setDiscount(BigDecimal.ZERO);
        rec.setPenalty(BigDecimal.ZERO);
        rec.setTotalFee(BigDecimal.ZERO);

        return rec;
    }

    private LocalDateTime toLdt(Object o) {
        if (o == null) return null;
        if (o instanceof Timestamp ts) return ts.toLocalDateTime();
        if (o instanceof java.sql.Date d) return d.toLocalDate().atStartOfDay();
        return LocalDateTime.parse(o.toString().replace(' ', 'T')); // 안전망
    }
}
