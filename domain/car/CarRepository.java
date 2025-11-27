package domain.car;

import db.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import domain.car.carFactory.CarFactory;
import domain.car.carFactory.CarFactoryProvider;
import domain.car.carFactory.CarType;

public class CarRepository {

    private final DBConnection db;

    public CarRepository(DBConnection db) {
        this.db = db;
    }

    /**
     * 모든 차량 로드 (DB → Car 객체)
     * 스키마 assumed: id, type, status, dailyrentalfee, name
     */
    public List<Car> findAllCars() {
        List<Car> cars = new ArrayList<>();

        // 컬럼명 dailyrentalfee로 수정, name 컬럼 추가
        String sql = "SELECT id, type, status, dailyrentalfee, name FROM car";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String type = rs.getString("type");
                String status = rs.getString("status");
                String name = rs.getString("name");
                BigDecimal dailyRentalFee = rs.getBigDecimal("dailyrentalfee");

                // 팩토리에서 차량 생성
                CarFactory factory = CarFactoryProvider.getFactory(type);
                Car car = factory.createCar(id);
                
                // 일일 대여료 설정 (DB에서 읽은 값이 null이면 타입의 기본 요금 사용)
                if (dailyRentalFee != null) {
                    car.setDailyRentalFee(dailyRentalFee);
                } else {
                    // 타입의 기본 요금 사용
                    CarType carType = CarType.valueOf(type);
                    car.setDailyRentalFee(carType.baseRate());
                }
                
                // 이름 설정 (name이 null이면 id 사용)
                if (name != null && !name.trim().isEmpty()) {
                    car.setName(name);
                }

                // 상태 반영
                if ("UNAVAILABLE".equalsIgnoreCase(status)) {
                    car.occupy();
                } else {
                    car.release();
                }

                cars.add(car);
            }

        } catch (SQLException e) {
            System.err.println("❌ 차량 목록 조회 실패: " + e.getMessage());
        }

        return cars;
    }

    /**
     * 차량 상태 업데이트 (대여/반납)
     */
    public void update(Car car) {
        String sql = "UPDATE car SET status=? WHERE id=?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, car.status().name());
            ps.setString(2, car.id());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ 차량 상태 업데이트 실패: " + e.getMessage());
        }
    }

    /**
     * 차량 1대 조회
     */
    public Car findById(String carId) {
        String sql = "SELECT id, type, status, dailyrentalfee, name FROM car WHERE id=?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, carId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String id = rs.getString("id");
                String type = rs.getString("type");
                String status = rs.getString("status");
                String name = rs.getString("name");
                BigDecimal dailyRentalFee = rs.getBigDecimal("dailyrentalfee");

                CarFactory factory = CarFactoryProvider.getFactory(type);
                Car car = factory.createCar(id);
                
                // 일일 대여료 설정 (DB에서 읽은 값이 null이면 타입의 기본 요금 사용)
                if (dailyRentalFee != null) {
                    car.setDailyRentalFee(dailyRentalFee);
                } else {
                    // 타입의 기본 요금 사용
                    CarType carType = CarType.valueOf(type);
                    car.setDailyRentalFee(carType.baseRate());
                }
                
                // 이름 설정 (name이 null이면 id 사용)
                if (name != null && !name.trim().isEmpty()) {
                    car.setName(name);
                }

                if ("UNAVAILABLE".equalsIgnoreCase(status)) {
                    car.occupy();
                }

                return car;
            }

        } catch (SQLException e) {
            System.err.println("❌ 차량 조회 실패: " + e.getMessage());
            return null;
        }
    }
}
