package db;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DB 연결 및 SQL 실행을 전담하는 헬퍼 클래스 (DB 관리자 담당)
 * 1. DB 연결 관리 (시스템 속성 'System.getProperty' 기반)
 * 2. 이름 기반 파라미터(:paramName)를 JDBC(? 기반)로 변환 및 실행
 * 3. 템플릿 메서드 제공 (execute, queryForObject, queryForList 등)
 */
public class DBConnection {

    // --- 1. DB 연결 설정 (시스템 속성 사용) ---

    private static final String DB_URL;
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;
    private static final String DB_DRIVER;

    static {
        // ⭐️ EnvLoader가 로드한 시스템 속성(System.getProperty)에서 값을 읽어옵니다.
        DB_URL = System.getProperty("DB_URL");
        DB_USERNAME = System.getProperty("DB_USERNAME");
        DB_PASSWORD = System.getProperty("DB_PASSWORD");
        DB_DRIVER = "org.mariadb.jdbc.Driver";

        // 2. 환경 변수가 올바르게 설정되었는지 확인
        if (DB_URL == null || DB_USERNAME == null || DB_PASSWORD == null) {
            // EnvLoader.load()가 main에서 호출되었더라도, 값이 없는 경우를 대비
            System.err.println("FATAL ERROR: .env 파일에서 DB 환경 변수를 읽어오지 못했습니다. DB 연결을 중단합니다.");
            System.err.println("필요한 키: DB_URL, DB_USERNAME, DB_PASSWORD");
            throw new RuntimeException("DB 환경 변수 누락");
        }

        try {
            // 3. MySQL JDBC 드라이버 로드
            Class.forName(DB_DRIVER);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("DBConnection 초기화 실패: JDBC 드라이버 로드 실패", e);
        }
    }

    /**
     * DB 연결(Connection)을 제공합니다.
     */
    public static Connection getConnection() throws SQLException {
        // 시스템 속성에서 읽어온 정보로 DB 연결
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
    }

    // --- 2. SQL 실행 헬퍼 메서드 (팀원들이 사용할 공용 API) ---

    /**
     * INSERT, UPDATE, DELETE 쿼리를 실행합니다.
     * @param sql    이름 기반 파라미터(:paramName)를 포함한 SQL
     * @param params 쿼리 파라미터 Map
     * @return 영향을 받은 행(row)의 수
     */
    public int execute(String sql, Map<String, Object> params) {
        ParsedQuery parsedQuery = parseNamedQuery(sql, params);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsedQuery.parsedSql)) {

            setParameters(pstmt, parsedQuery.parameters);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("DB execute 실패: " + e.getMessage(), e);
        }
    }

    /**
     * INSERT 쿼리를 실행하고, DB에서 자동 생성된 ID(PK)를 반환합니다.
     * @param sql    이름 기반 파라미터(:paramName)를 포함한 SQL
     * @param params 쿼리 파라미터 Map
     * @return 생성된 ID (PK)
     */
    public int executeAndReturnKey(String sql, Map<String, Object> params) {
        ParsedQuery parsedQuery = parseNamedQuery(sql, params);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsedQuery.parsedSql, Statement.RETURN_GENERATED_KEYS)) {

            setParameters(pstmt, parsedQuery.parameters);
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("ID 생성 실패: 생성된 키를 반환받지 못했습니다.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB executeAndReturnKey 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 단일 레코드(row)를 조회하는 SELECT 쿼리를 실행합니다.
     * @param sql    이름 기반 파라미터(:paramName)를 포함한 SQL
     * @param params 쿼리 파라미터 Map
     * @return 조회된 레코드 (Map) 또는 Optional.empty()
     */
    public Optional<Map<String, Object>> queryForObject(String sql, Map<String, Object> params) {
        ParsedQuery parsedQuery = parseNamedQuery(sql, params);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsedQuery.parsedSql)) {

            setParameters(pstmt, parsedQuery.parameters);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToMap(rs));
                } else {
                    return Optional.empty(); // 조회 결과 없음
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB queryForObject 실패: " + e.getMessage(), e);
        }
    }

    /**
     * (보너스) 여러 레코드(row)를 조회하는 SELECT 쿼리를 실행합니다.
     */
    public List<Map<String, Object>> queryForList(String sql, Map<String, Object> params) {
        List<Map<String, Object>> results = new ArrayList<>();
        ParsedQuery parsedQuery = parseNamedQuery(sql, params);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(parsedQuery.parsedSql)) {

            setParameters(pstmt, parsedQuery.parameters);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRowToMap(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB queryForList 실패: " + e.getMessage(), e);
        }
        return results;
    }


    // --- 3. 내부 헬퍼 메서드 (JDBC 처리) ---

    private Map<String, Object> mapRowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> rowMap = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            // getColumnLabel은 AS 별칭을 반환하고, 별칭이 없으면 컬럼명을 반환
            // getColumnName은 항상 실제 컬럼명만 반환
            String columnLabel = metaData.getColumnLabel(i);
            rowMap.put(columnLabel, rs.getObject(i));
        }
        return rowMap;
    }

    private void setParameters(PreparedStatement pstmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
    }

    private ParsedQuery parseNamedQuery(String sql, Map<String, Object> params) {
        List<Object> paramValues = new ArrayList<>();
        Pattern pattern = Pattern.compile(":(\\w+)");
        Matcher matcher = pattern.matcher(sql);
        StringBuffer parsedSql = new StringBuffer();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            if (params != null && !params.containsKey(paramName)) {
                throw new IllegalArgumentException("SQL 파라미터 매핑 오류: 쿼리에 명시된 '" + paramName + "'이(가) 파라미터 Map에 존재하지 않습니다.");
            }
            matcher.appendReplacement(parsedSql, "?");
            if (params != null) {
                paramValues.add(params.get(paramName));
            }
        }
        matcher.appendTail(parsedSql);

        return new ParsedQuery(parsedSql.toString(), paramValues);
    }

    private static class ParsedQuery {
        final String parsedSql;
        final List<Object> parameters;

        ParsedQuery(String parsedSql, List<Object> parameters) {
            this.parsedSql = parsedSql;
            this.parameters = parameters;
        }
    }

    // --- 4. 리소스 정리 헬퍼 메서드 (필요시 사용) ---

    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
        try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void close(Connection conn, PreparedStatement pstmt) {
        close(conn, pstmt, null);
    }
}