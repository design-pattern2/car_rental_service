package domain.car;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import domain.car.car_Factory.*;

public class CarInventory {

    private final CarRepository carRepository;
    private final List<Car> cars;

    public CarInventory(CarRepository carRepository) {
        this.carRepository = carRepository;
        this.cars = carRepository.findAllCars(); // DB에서 전체 차량 로드
    }

    /** 사용 가능한 차량 리스트 반환 */
    public List<Car> getAvailableCars() {
        return cars.stream()
                .filter(car -> car.status() == CarStatus.AVAILABLE)
                .collect(Collectors.toList());
    }
    // 전체 차량 리스트 반환
    public List<Car> getAllCars() {
    	return cars.stream()
    			.collect(Collectors.toList());
    }

    /** ID로 차량 찾기 */
    public Optional<Car> findById(String id) {
        return cars.stream()
                .filter(car -> car.id().equals(id))
                .findFirst();
    }

    /** 차량 대여 */
    public void rentCar(String carId) {
        Optional<Car> opt = findById(carId);

        if (opt.isPresent()) {
            Car car = opt.get();
            car.occupy();                 // 상태 변경
            carRepository.update(car);    // DB 업데이트
        } else {
            throw new IllegalArgumentException("존재하지 않는 차량 ID입니다: " + carId);
        }
    }

    /** 차량 반납 */
    public void returnCar(String carId) {
        Optional<Car> opt = findById(carId);

        if (opt.isPresent()) {
            Car car = opt.get();
            car.release();
            carRepository.update(car);
        } else {
            throw new IllegalArgumentException("존재하지 않는 차량 ID입니다: " + carId);
        }
    }
}