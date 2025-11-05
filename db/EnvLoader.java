package db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvLoader {

    public static void load() {
        File envFile = new File(".env");

        if (!envFile.exists()) {
            System.err.println("FATAL ERROR: .env 파일을 찾을 수 없습니다! 프로젝트 루트에 .env 파일을 생성해주세요.");
            return;
        }

        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(envFile)) {
            properties.load(fis);

            for (String name : properties.stringPropertyNames()) {
                System.setProperty(name, properties.getProperty(name));
            }

            System.out.println(" .env 환경 변수 로드 성공.");

        } catch (IOException e) {
            System.err.println("FATAL ERROR: .env 파일 로드 중 오류 발생: " + e.getMessage());
        }
    }
}