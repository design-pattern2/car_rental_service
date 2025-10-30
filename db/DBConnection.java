package db;

import java.util.Map;
import java.util.Optional;

public class DBConnection {
//DBConnection.execute(쿼리, 파라미터)는 변경된 행 수를 반환
    public int execute(String sql, Map<String, Object> params) {
        return 0;
    }
//DB에서 자동 생성된 PK(id) 값을 반환해주는 메서드
    public int executeAndReturnKey(String sql, Map<String, Object> params) {
    }
//쿼리를 실행하고, 결과로 단일 레코드(Map)를 Optional로 반환
    public Optional<Map<String, Object>> queryForObject(String sql, Map<String, Object> params) {
    }
}