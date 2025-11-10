package domain.car.car_Factory;

import domain.car.Car;

public class SedanFactory implements CarFactory {
    @Override
    public Car createCar(String id) {
        return new Car(id, CarType.SEDAN);
    }
}