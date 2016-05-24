package ots;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ots.strategy.CacheBasedSelectionStrategy;
import ots.strategy.JPAReservationStrategy;
import ots.strategy.RandomSeatSelectionStrategy;
import ots.strategy.RandomSectorSelectionStrategy;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CyclicBarrier;

/**
 * The class ReservationTest implements a load test of the reservation service.
 */
public class ReservationTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(ReservationTest.class.getName());

    private static final String SEATS_FILE = "seats.csv";
    private static final String TIMES_FILE = "times_%s.csv";
    private static final String RESULTS_FILE = "results_%s.txt";
    private static final String CSV_DELIMITER = ",";
    private static final int NUMBER_THREADS = 100;
    //private static final int NUMBER_THREADS = 1;
    private static List<String> categories;
    private static List<Seat> availableSeats;
    private List<Seat> reservedSeats;
    private List<Reservation> reservations;
    private List<long[]> times;

    private void setup() throws Exception {
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
        setup();
    }

    @After
    public void after() {
        ReservationService.cleanup();
    }

    private void testReservationService(Class<? extends JPAReservationStrategy> reservationStrategy) throws Exception {

        // run reservation threads
        CyclicBarrier barrier = new CyclicBarrier(NUMBER_THREADS);
        List<ReservationThread> threads = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= NUMBER_THREADS; i++) {
            ReservationThread thread = new ReservationThread(reservationStrategy, categories, barrier);
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
        PrintWriter writer = new PrintWriter(new FileWriter(String.format(TIMES_FILE, reservationStrategy.getSimpleName())));
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

        StringBuilder sb = new StringBuilder();

        // print results
        sb.append("Seats").append("\n");
        sb.append("  available:     ").append(availableSeats.size()).append("\n");
        sb.append("  reserved:      ").append(reservedSeats.size()).append("\n");
        sb.append("  remaining:     ").append(availableSeats.size() - reservedSeats.size()).append("\n");
        sb.append("Reservations" + "\n");
        sb.append("  total:         ").append(reservations.size()).append("\n");
        sb.append("  rejected:      ").append(rejectedReservations).append("\n");
        sb.append("  adjacent:      ").append(adjacentReservations).append("\n");
        sb.append("  non-adjacent:  ").append(nonAdjacentReservations).append("\n");
        sb.append("  incorrect:     ").append(incorrectReservations).append("\n");
        sb.append("Latency Time" + "\n");
        sb.append("  minimum:       ").append((double) minTime / 1000).append("s").append("\n");
        sb.append("  maximum:       ").append((double) maxTime / 1000).append("s").append("\n");
        sb.append("  average:       ").append((double) averageTime / 1000).append("s").append("\n");
        sb.append("  deviation:     ").append((double) deviation / 1000).append("s").append("\n");
        sb.append("\n");
        sb.append("Total Time:      ").append((endTime - startTime) / 1000).append("s").append("\n");
        sb.append("Throughput:      ").append(throughput).append(" requests/s").append("\n");

        System.out.println(sb.toString());

        writer = new PrintWriter(new FileWriter(String.format(RESULTS_FILE, reservationStrategy.getSimpleName())));
        for (String s : sb.toString().split("\n")) {
            writer.println(s);
        }
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

    @Test
    public void testRandomSetSelectionStrategy() throws Exception {
        LOGGER.debug("Start testRandomSetSelectionStrategy");
        testReservationService(RandomSeatSelectionStrategy.class);
        LOGGER.debug("End testRandomSetSelectionStrategy");
    }

    @Test
    public void testRandomSectorSelectionStrategy() throws Exception {
        LOGGER.debug("Start testRandomSectorSelectionStrategy");
        testReservationService(RandomSectorSelectionStrategy.class);
        LOGGER.debug("End testRandomSectorSelectionStrategy");
    }

    @Test
    public void testCachedSeatSelectionStrategy() throws Exception {
        LOGGER.debug("Start testCachedSeatSelectionStrategy");
        CacheBasedSelectionStrategy.setupCache();
        testReservationService(CacheBasedSelectionStrategy.class);
        LOGGER.debug("End testCachedSeatSelectionStrategy");
    }
}
