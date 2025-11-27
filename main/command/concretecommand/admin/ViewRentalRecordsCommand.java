package main.command.concretecommand.admin;

import main.command.command.Command;
import main.command.receiver.ApplicationContext;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * Command Pattern: ConcreteCommand
 * 대여 기록 조회 명령 (관리자 전용)
 */
public class ViewRentalRecordsCommand implements Command {
    private final ApplicationContext context;
    
    public ViewRentalRecordsCommand(ApplicationContext context, Scanner scanner) {
        this.context = context;
    }
    
    @Override
    public void execute() {
        System.out.println("\n[6. 대여 기록 조회]");
        try {
            List<Map<String, Object>> rentalRecords = context.getAdminService().getAllRentalRecordsWithCarName();
            
            if (rentalRecords.isEmpty()) {
                System.out.println("❌ 등록된 대여 기록이 없습니다.");
                return;
            }
            
            System.out.println("\n전체 대여 기록:");
            System.out.println("-".repeat(40));
            
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < rentalRecords.size(); i++) {
                Map<String, Object> record = rentalRecords.get(i);
                
                // 연체 여부 확인
                Object endTimeObj = record.get("endTime");
                LocalDateTime endTime = null;
                boolean isOverdue = false;
                
                if (endTimeObj != null) {
                    if (endTimeObj instanceof Timestamp) {
                        endTime = ((Timestamp) endTimeObj).toLocalDateTime();
                    } else if (endTimeObj instanceof LocalDateTime) {
                        endTime = (LocalDateTime) endTimeObj;
                    }
                    
                    String recordStatus = Objects.toString(record.get("status"), "");
                    if ("RENTED".equalsIgnoreCase(recordStatus) && endTime != null && now.isAfter(endTime)) {
                        isOverdue = true;
                    }
                }
                
                // 차량 이름
                String carName = null;
                if (record.containsKey("carName")) {
                    carName = Objects.toString(record.get("carName"), "");
                } else if (record.containsKey("c.name")) {
                    carName = Objects.toString(record.get("c.name"), "");
                }
                if (carName == null || carName.isEmpty() || "null".equals(carName)) {
                    carName = "알 수 없음";
                }
                
                // 사용자 이름
                String userName = null;
                if (record.containsKey("userName")) {
                    userName = Objects.toString(record.get("userName"), "");
                } else if (record.containsKey("u.name")) {
                    userName = Objects.toString(record.get("u.name"), "");
                }
                if (userName == null || userName.isEmpty() || "null".equals(userName)) {
                    userName = "알 수 없음";
                }
                
                // 대여 날짜
                Object startTimeObj = record.get("startTime");
                String startTimeStr = "";
                if (startTimeObj != null) {
                    if (startTimeObj instanceof Timestamp) {
                        startTimeStr = ((Timestamp) startTimeObj).toLocalDateTime().toString();
                    } else if (startTimeObj instanceof LocalDateTime) {
                        startTimeStr = startTimeObj.toString();
                    } else {
                        startTimeStr = startTimeObj.toString();
                    }
                    if (startTimeStr.contains("T")) {
                        startTimeStr = startTimeStr.substring(0, startTimeStr.indexOf("T"));
                    }
                }
                
                // 반납 날짜
                String endTimeStr = "";
                if (endTime != null) {
                    endTimeStr = endTime.toString();
                    if (endTimeStr.contains("T")) {
                        endTimeStr = endTimeStr.substring(0, endTimeStr.indexOf("T"));
                    }
                }
                
                // 상태
                String status = Objects.toString(record.get("status"), "");
                
                // 연체 표시
                String overdueIcon = isOverdue ? "🔴 " : "";
                
                // 세로로 출력
                System.out.println(overdueIcon + "차량: " + carName);
                System.out.println("사용자: " + userName);
                System.out.println("대여 날짜: " + startTimeStr);
                System.out.println("반납 날짜: " + endTimeStr);
                System.out.println("상태: " + status);
                
                // 마지막 항목이 아니면 구분선 추가
                if (i < rentalRecords.size() - 1) {
                    System.out.println();
                }
            }
            System.out.println("-".repeat(40));
            
        } catch (Exception e) {
            System.err.println("❌ 대여 기록 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

