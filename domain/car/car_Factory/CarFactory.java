package car_Factory;

public class CarFactory {
    
    protected CarTypePolicy createPolicy(CarType type) {
        return switch (type) {
            case SEDAN -> new SEDANPolicy();
            case SUV -> new SUVPolicy();
            case BIKE   -> new BIKEPolicy();
        };
    }
}