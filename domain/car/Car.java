package domain.car;

import java.math.BigDecimal;
import java.util.Objects;

import domain.car.carFactory.CarStatus;
import domain.car.carFactory.CarType;

public class Car {
	private final String id;
	private final CarType type;
	private CarStatus status;
    private BigDecimal dailyRentalFee;
    
    
    public Car(String id, CarType type)
    {
    	this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.status = CarStatus.AVAILABLE;
    }
    
    public String id() { return id; }
    public CarType type() { return type; }
    public CarStatus status() { return status; }
    public BigDecimal getDailyRentalFee(){return dailyRentalFee;}
    
    public void occupy() { this.status = CarStatus.UNAVAILABLE; }
    public void release() { this.status = CarStatus.AVAILABLE; }
    
    
    @Override public String toString() {
    	return "Car{id='%s', type=%s, status=%s}".formatted(id, type, status);
    }
    
}