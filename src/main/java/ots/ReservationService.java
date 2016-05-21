package ots;

import ots.cache.SeatCache;
import ots.strategy.JPAReservationStrategy;
import ots.strategy.ReservationStrategy;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import java.util.List;


/**
 * The class ReservationService implements a service that allows to make seat reservations.
 */
public class ReservationService {

    public static final String PERSISTENCE_UNIT = "ots";
    private final EntityManager entityManager;

    private static SeatCache seatCache = new SeatCache();
    private final ReservationStrategy reservationStrategy;

    /* Returns an instance of the reservation service. */
    public static ReservationService getInstance(Class<? extends JPAReservationStrategy> jpaReservationStrategy) {
        return new ReservationService(jpaReservationStrategy);
    }

    private ReservationService(Class<? extends JPAReservationStrategy> jpaReservationStrategy) {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        entityManager = factory.createEntityManager();

        try {
            reservationStrategy = jpaReservationStrategy.getConstructor(EntityManager.class).newInstance(entityManager);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not initialize ReservationStrategy");
        }
    }


    /* Sets up the database tables. */
    public static void setup(List<Seat> seats) {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        EntityManager entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        for (Seat seat : seats) {
            entityManager.persist(new SeatEntity(seat.getCategory(), seat.getSector(), seat.getRow(), seat.getNumber()));
        }
        entityManager.getTransaction().commit();
        entityManager.close();
    }


    /* Cleans up the database tables. */
    public static void cleanup() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        EntityManager entityManager = factory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.createQuery("DELETE FROM SeatEntity").executeUpdate();
        entityManager.getTransaction().commit();
        entityManager.close();
    }

    /* Makes a seat reservation. */
    public Seat[] makeReservation(String category, int numberOfSeats) {

        return reservationStrategy.makeReservation(category, numberOfSeats);
    }


}
