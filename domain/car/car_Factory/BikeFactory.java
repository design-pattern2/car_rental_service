package domain.car.car_Factory;

import domain.car.Car;

public class BikeFactory implements CarFactory {
    @Override
    public Car createCar(String id) {
        return new Car(id, CarType.BIKE);
    }
}