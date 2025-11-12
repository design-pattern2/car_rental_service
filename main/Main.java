package main;

import db.DBConnection;
import db.EnvLoader; // ⭐️ 추가
import java.sql.Connection;
import java.sql.SQLException;
import domain.car.Car;
import domain.car.car_Factory.*;
import domain.car.decorator.*;

import java.math.BigDecimal;

public class Main {

    public static void main(String[] args) {
        EnvLoader.load();

        System.out.println(" 차량 렌트 시스템 시뮬레이션을 시작합니다. ");

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println(" DB 연결에 성공했습니다! 스키마: " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println(" DB 연결에 실패했습니다! 오류: " + e.getMessage());
        }
        System.out.println("=== 팩토리 & 데코레이터 테스트 ===");

        // 1️⃣ 팩토리로 차량 생성
        CarFactory sedanFactory = new SedanFactory();
        CarFactory suvFactory = new SuvFactory();

        Car sedan = sedanFactory.createCar("S001");
        Car suv = suvFactory.createCar("SUV001");

        System.out.println("생성된 차량:");
        System.out.println(sedan);
        System.out.println(suv);

        // 2️⃣ 기본 요금 프라이서
        CarPricer sedanPricer = new BaseCarPricer(sedan);
        CarPricer suvPricer = new BaseCarPricer(suv);

        System.out.println("\n기본 요금:");
        System.out.println(sedanPricer.description() + ": " + sedanPricer.quote(sedan));
        System.out.println(suvPricer.description() + ": " + suvPricer.quote(suv));

        // 3️⃣ 옵션 추가 (데코레이터)
        CarPricer sedanWithOptions = new SunroofOption(new NavigationOption(new BlackboxOption(sedanPricer)));
        CarPricer suvWithOptions   = new BlackboxOption(suvPricer);

        System.out.println("\n옵션 적용 후 요금:");
        System.out.println(sedanWithOptions.description() + ": " + sedanWithOptions.quote(sedan));
        System.out.println(suvWithOptions.description() + ": " + suvWithOptions.quote(suv));

        // 4️⃣ 차량 상태 변경 테스트
        sedan.occupy();
        System.out.println("\n차량 상태 변경 후:");
        System.out.println(sedan);
    }
        
}