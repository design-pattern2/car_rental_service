package domain.car.carFactory;

import domain.car.Car;

public interface CarFactory {
    Car createCar(String id);
}