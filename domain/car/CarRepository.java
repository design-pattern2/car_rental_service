package domain.car;

import db.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import domain.car.carFactory.CarFactory;
import domain.car.carFactory.CarFactoryProvider;

public class CarRepository {

    private final DBConnection db;

    public CarRepository(DBConnection db) {
        this.db = db;
    }

    /**
     * 모든 차량 로드 (DB → Car 객체)
     * 스키마 assumed: id, type, status, dailyrentalfee
     */
    public List<Car> findAllCars() {
        List<Car> cars = new ArrayList<>();

        // 컬럼명 dailyrentalfee로 수정
        String sql = "SELECT id, type, status, dailyrentalfee FROM car";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String type = rs.getString("type");
                String status = rs.getString("status");

                // dailyrentalfee 컬럼은 SELECT에 포함하지만 팩토리에서 하드코딩하므로 여기서는 사용하지 않음
                // 예시: double fee = rs.getDouble("dailyrentalfee");

                // 팩토리에서 차량 생성
                CarFactory factory = CarFactoryProvider.getFactory(type);
                Car car = factory.createCar(id);

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
        String sql = "SELECT id, type, status, dailyrentalfee FROM car WHERE id=?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, carId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String id = rs.getString("id");
                String type = rs.getString("type");
                String status = rs.getString("status");

                // 필요하면 요금 읽을 수 있음: double fee = rs.getDouble("dailyrentalfee");

                CarFactory factory = CarFactoryProvider.getFactory(type);
                Car car = factory.createCar(id);

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
