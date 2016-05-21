package ots.strategy;

import ots.Seat;

/**
 * Created by krigu on 21.05.16.
 */
public interface ReservationStrategy {

    Seat[] makeReservation(String category, int numberOfSeats);

}
