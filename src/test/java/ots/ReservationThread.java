package ots;

import ots.strategy.JPAReservationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * The class ReservationThread implements a thread that repeatedly makes seat reservations.
 */
public class ReservationThread extends Thread {

	public static final int NUMBER_RESERVATIONS = 40;
	public static final int MIN_SEATS_RESERVATION = 1;
	public static final int MAX_SEATS_RESERVATION = 10;
	public static final int AVERAGE_THINKTIME = 0;
	private final List<String> categories;
	private final ReservationService reservationService;
	private final CyclicBarrier barrier;
	private final List<Reservation> reservations;
	private final List<long[]> times;

	public ReservationThread(Class<? extends JPAReservationStrategy> reservationStrategy, List<String> categories, CyclicBarrier barrier) {
		this.categories = categories;
		this.barrier = barrier;
		reservationService = ReservationService.getInstance(reservationStrategy);
		reservations = new ArrayList<>();
		times = new ArrayList<>();
	}

	public List<Reservation> getReservations() {
		return reservations;
	}

	public List<long[]> getTimes() {
		return times;
	}

	@Override
	public void run() {
		try {
			barrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
		}
		Random random = new Random();
		for (int i = 0; i < NUMBER_RESERVATIONS; i++) {
			try {
				int thinkTime = random.nextInt(AVERAGE_THINKTIME + 1);
				if (thinkTime > 0) {
					Thread.sleep(thinkTime);
				}
			} catch (Exception e) {
			}
			String category = categories.get(random.nextInt(categories.size()));
			int numberOfSeats = MIN_SEATS_RESERVATION
					+ random.nextInt(MAX_SEATS_RESERVATION - MIN_SEATS_RESERVATION + 1);
			long startTime = System.currentTimeMillis();
			Seat[] seats = reservationService.makeReservation(category, numberOfSeats);

			long endTime = System.currentTimeMillis();
			reservations.add(new Reservation(category, numberOfSeats, seats));
			times.add(new long[]{startTime, endTime});
		}
	}
}
