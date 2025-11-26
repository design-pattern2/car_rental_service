package domain.car.car_Factory;

public class CarFactoryProvider {
    public static CarFactory getFactory(String type) {
        return switch (type) {
            case "SEDAN" -> new SedanFactory();
            case "SUV"   -> new SuvFactory();
            case "BIKE" -> new BikeFactory();
            default -> throw new IllegalArgumentException("Unknown car type: " + type);
        };
    }
}