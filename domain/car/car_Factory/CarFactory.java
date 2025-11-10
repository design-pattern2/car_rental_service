package domain.car.car_Factory;

import domain.car.Car;

public interface CarFactory {
    Car createCar(String id);
}