package ots;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The class ReservationTest implements a load test of the reservation service.
 */
public class ReservationTest {

	private static final String SEATS_FILE = "seats.csv";
	private static final String TIMES_FILE = "times.csv";
	private static final String RESULTS_FILE = "results.txt";
	private static final String CSV_DELIMITER = ",";
	private static final int NUMBER_THREADS = 100;
	//private static final int NUMBER_THREADS = 1;
	private static List<String> categories;
	private static List<Seat> availableSeats;
	private List<Seat> reservedSeats;
	private List<Reservation> reservations;
	private List<long[]> times;
	private long startTime;

	@BeforeClass
	public static void setup() throws Exception {
		categories = new ArrayList<>();
		availableSeats = new ArrayList<>();
		try (Scanner scanner = new Scanner(new FileReader(SEATS_FILE))) {
			scanner.nextLine();
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String tokens[] = line.split(CSV_DELIMITER);
				if (!categories.contains(tokens[0])) {
					categories.add(tokens[0]);
				}
				Seat seat = new Seat(tokens[0], tokens[1], Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3]));
				availableSeats.add(seat);
			}
		}
		ReservationService.setup(availableSeats);
	}

	@Before
	public void init() throws Exception {
		reservedSeats = new ArrayList<>();
		reservations = new ArrayList<>();
		times = new ArrayList<>();
	}

	@AfterClass
	public static void cleanup() throws Exception {
		ReservationService.cleanup();
	}

	@Test
	public void testReservationService() throws Exception {

		// run reservation threads
		CyclicBarrier barrier = new CyclicBarrier(NUMBER_THREADS);
		List<ReservationThread> threads = new ArrayList<>();
		startTime = System.currentTimeMillis();
		for (int i = 1; i <= NUMBER_THREADS; i++) {
			ReservationThread thread = new ReservationThread(categories, barrier);
			threads.add(thread);
			thread.start();
		}
		for (ReservationThread thread : threads) {
			thread.join();
			reservations.addAll(thread.getReservations());
			times.addAll(thread.getTimes());
		}
		long endTime = System.currentTimeMillis();

		// check reservations
		int rejectedReservations = 0, incorrectReservations = 0;
		int adjacentReservations = 0, nonAdjacentReservations = 0;
		for (Reservation reservation : reservations) {
			if (reservation.getSeats() == null || reservation.getSeats().length == 0) {
				rejectedReservations++;
			} else if (!isCorrect(reservation)) {
				incorrectReservations++;
			} else if (isAdjacent(reservation)) {
				adjacentReservations++;
			} else {
				nonAdjacentReservations++;
			}
		}

		// calculate times
		long totalTime = 0, minTime = 0, maxTime = 0;
		PrintWriter writer = new PrintWriter(new FileWriter(TIMES_FILE));
		for (long[] t : times) {
			long start = t[0] - startTime;
			long time = t[1] - t[0];
			totalTime += time;
			minTime = Math.min(minTime, time);
			maxTime = Math.max(maxTime, time);
			writer.println(start + "," + time);
		}
		writer.close();
		long averageTime = totalTime / reservations.size();
		long deviation = 0;
		for (long[] t : times) {
			long time = t[1] - t[0];
			deviation += (time - averageTime) * (time - averageTime);
		}
		deviation = (long) Math.sqrt(deviation / times.size());
		long throughput = 1000 * reservations.size() / (endTime - startTime);

		// print results
		writer = new PrintWriter(new FileWriter(RESULTS_FILE));
		writer.println("Seats");
		writer.println("  available:     " + availableSeats.size());
		writer.println("  reserved:      " + reservedSeats.size());
		writer.println("  remaining:     " + (availableSeats.size() - reservedSeats.size()));
		writer.println("Reservations");
		writer.println("  total:         " + reservations.size());
		writer.println("  rejected:      " + rejectedReservations);
		writer.println("  adjacent:      " + adjacentReservations);
		writer.println("  non-adjacent:  " + nonAdjacentReservations);
		writer.println("  incorrect:     " + incorrectReservations);
		writer.println("Latency Time");
		writer.println("  minimum:       " + (double) minTime / 1000 + "s");
		writer.println("  maximum:       " + (double) maxTime / 1000 + "s");
		writer.println("  average:       " + (double) averageTime / 1000 + "s");
		writer.println("  deviation:     " + (double) deviation / 1000 + "s");
		writer.println();
		writer.println("Total Time:      " + (endTime - startTime) / 1000 + "s");
		writer.println("Throughput:      " + throughput + " requests/s");
		writer.close();
	}

	private boolean isCorrect(Reservation reservation) {
		if (reservation.getSeats().length != reservation.getNumberOfSeats()) {
			return false;
		}
		String category = reservation.getCategory();
		String sector = null;
		for (Seat seat : reservation.getSeats()) {
			if (!availableSeats.contains(seat)) {
				return false;
			}
			if (!seat.getCategory().equals(category)) {
				return false;
			}
			if (reservedSeats.contains(seat)) {
				return false;
			}
			reservedSeats.add(seat);
			if (sector == null) {
				sector = seat.getSector();
			} else if (!seat.getSector().equals(sector)) {
				return false;
			}
		}
		return true;
	}

	private boolean isAdjacent(Reservation reservation) {
		Seat[] seats = reservation.getSeats();
		for (int i = 0; i < seats.length - 1; i++) {
			if (seats[i].getRow() != seats[i + 1].getRow()
					|| seats[i].getNumber() != seats[i + 1].getNumber() - 1) {
				return false;
			}
		}
		return true;
	}
}
