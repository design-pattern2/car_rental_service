package domain.rental;

import db.DBConnection;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class RentalRepository {
    // === 팀 DB 스키마에 맞게 필요시 수정 ===
    private static final String TBL = "rental_records";
    private static final String COL_OPTIONS = "options_json"; // 문자열 CSV로 저장 중
    // ===========================================

    private final DBConnection db;

    public RentalRepository(DBConnection db) {
        this.db = db;
    }

    public Optional<RentalRecord> findActiveByCarId(String carId) {
        String sql = "SELECT * FROM " + TBL + " WHERE car_id = :carId AND status = 'RENTED' ORDER BY id DESC LIMIT 1";
        return db.queryForObject(sql, Map.of("carId", carId)).map(this::mapRow);
    }

    public Optional<RentalRecord> findById(long id) {
        String sql = "SELECT * FROM " + TBL + " WHERE id = :id";
        return db.queryForObject(sql, Map.of("id", id)).map(this::mapRow);
    }

    public List<RentalRecord> findByUser(String userId) {
        String sql = "SELECT * FROM " + TBL + " WHERE user_id = :uid ORDER BY id DESC";
        return db.queryForList(sql, Map.of("uid", userId)).stream().map(this::mapRow).collect(Collectors.toList());
    }

    public long save(RentalRecord r) {
        String sql = "INSERT INTO " + TBL + " (user_id, car_id, rental_days, start_at, status, " +
                "fee_strategy, membership_strategy, " + COL_OPTIONS + ", base_fee, option_fee, discount, penalty, total_fee) " +
                "VALUES (:user_id, :car_id, :rental_days, :start_at, :status, :fee, :mbr, :opts, :base, :opt, :disc, :pen, :total)";
        Map<String, Object> p = new HashMap<>();
        p.put("user_id", r.getUserId());
        p.put("car_id", r.getCarId());
        p.put("rental_days", r.getRentalDays());
        p.put("start_at", Timestamp.valueOf(r.getStartAt()));
        p.put("status", r.getStatus().name());
        p.put("fee", r.getFeeStrategyType());
        p.put("mbr", r.getMembershipStrategyType());
        p.put("opts", String.join(",", r.getOptions()));
        p.put("base", r.getBaseFee());
        p.put("opt", r.getOptionFee());
        p.put("disc", r.getDiscount());
        p.put("pen", r.getPenalty());
        p.put("total", r.getTotalFee());

        db.execute(sql, p);
        // DBConnection이 생성키 반환을 직접 지원하지 않는 경우: LAST_INSERT_ID() 조회
        Long id = db.queryForObject("SELECT LAST_INSERT_ID() AS id", Map.of())
                .map(row -> ((Number) row.get("id")).longValue())
                .orElse(null);
        if (id != null) r.setId(id);
        return id == null ? 0L : id;
    }

    public void markReturned(long id, BigDecimal penalty, BigDecimal discount, BigDecimal total) {
        String sql = "UPDATE " + TBL + " SET status='RETURNED', end_at=CURRENT_TIMESTAMP, " +
                "penalty=:pen, discount=:disc, total_fee=:total WHERE id=:id";
        Map<String, Object> p = new HashMap<>();
        p.put("pen", penalty);
        p.put("disc", discount);
        p.put("total", total);
        p.put("id", id);
        db.execute(sql, p);
    }

    // ---- Row Mapper ----
    private RentalRecord mapRow(Map<String, Object> row) {
        RentalRecord r = new RentalRecord();
        Object id = row.get("id");
        if (id != null) r.setId(((Number) id).longValue());
        r.setUserId(Objects.toString(row.get("user_id"), null));
        r.setCarId(Objects.toString(row.get("car_id"), null));
        Object days = row.get("rental_days");
        if (days != null) r.setRentalDays(((Number) days).intValue());

        Object st = row.get("start_at");
        if (st instanceof Timestamp ts) r.setStartAt(ts.toLocalDateTime());
        Object et = row.get("end_at");
        if (et instanceof Timestamp ts2) r.setEndAt(ts2.toLocalDateTime());

        Object status = row.get("status");
        if (status != null) r.setStatus(RentalRecord.Status.valueOf(status.toString()));

        r.setFeeStrategyType(Objects.toString(row.get("fee_strategy"), null));
        r.setMembershipStrategyType(Objects.toString(row.get("membership_strategy"), null));

        String opts = Objects.toString(row.get(COL_OPTIONS), "");
        if (!opts.isBlank()) r.setOptions(Arrays.asList(opts.split("\\s*,\\s*")));

        r.setBaseFee(toBd(row.get("base_fee")));
        r.setOptionFee(toBd(row.get("option_fee")));
        r.setDiscount(toBd(row.get("discount")));
        r.setPenalty(toBd(row.get("penalty")));
        r.setTotalFee(toBd(row.get("total_fee")));
        return r;
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
}
