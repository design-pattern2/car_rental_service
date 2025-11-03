# car_rental_service

---

## Local DataBase 생성 방법(CLI 기준)

### 1. rentalsystem DataBase 생성
    CREATE DATABASE IF NOT EXISTS rentalsystem
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

### 2. 해당 DataBase 이동
    USE rentalsystem;

### 3. user Table 생성
    CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId VARCHAR(100) NOT NULL UNIQUE,
    pw VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phoneNumber VARCHAR(50) UNIQUE,
    cardNumber VARCHAR(50),
    membership VARCHAR(50)  
    );

### 4. car Table 생성
    CREATE TABLE IF NOT EXISTS car (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('SEDAN', 'SUV', 'BIKE') NOT NULL, 
    status VARCHAR(50) NOT NULL, 
    dailyRentalFee DECIMAL(10, 2) 
    );

### 5. rental Table 생성
    CREATE TABLE IF NOT EXISTS rental (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId INT NOT NULL,
    carId INT NOT NULL,
    startTime DATETIME NOT NULL,
    endTime DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL,

        FOREIGN KEY (userId) REFERENCES user(id) ON DELETE CASCADE,
        FOREIGN KEY (carId) REFERENCES car(id)
    );

---

## Build 및 실행 방법

### 1. 컴파일 (소스 코드를 build 폴더에 저장)
    javac -d build -cp ".;lib/*" (Get-ChildItem -Recurse -Filter *.java).FullName

### 2. 실행
    java -cp "build;lib/*" main.Main

---

## .env 설정

1. DB 연결 URL (DB 이름은 'rentalsystem'으로 통일)
2. 본인의 MySQL 사용자 이름
3. 본인의 MySQL 비밀번호


    DB_URL=
    DB_USERNAME=
    DB_PASSWORD=