package main;

import db.DBConnection;
import db.EnvLoader; // ⭐️ 추가
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        EnvLoader.load();

        System.out.println(" 차량 렌트 시스템 시뮬레이션을 시작합니다. ");

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println(" DB 연결에 성공했습니다! 스키마: " + conn.getSchema());
        } catch (SQLException e) {
            System.err.println(" DB 연결에 실패했습니다! 오류: " + e.getMessage());
        }
    }
}