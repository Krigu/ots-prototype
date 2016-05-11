package ots;

/**
 * The class Reservation contains the data of a seat reservation.
 */
public class Reservation {

	private final String category;
	private final int numberOfSeats;
	private final Seat[] seats;

	public Reservation(String category, int numberOfSeats, Seat[] seats) {
		this.category = category;
		this.numberOfSeats = numberOfSeats;
		this.seats = seats;
	}

	public String getCategory() {
		return category;
	}

	public int getNumberOfSeats() {
		return numberOfSeats;
	}

	public Seat[] getSeats() {
		return seats;
	}
}
