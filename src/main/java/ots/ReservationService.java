package ots;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.OptimisticLockException;
import javax.persistence.Persistence;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * The class ReservationService implements a service that allows to make seat reservations.
 */
public class ReservationService {

    private static final String PERSISTENCE_UNIT = "ots";
    private final EntityManager entityManager;

    private static SeatCache seatCache = new SeatCache();

    /* Returns an instance of the reservation service. */
    public static ReservationService getInstance() {
        return new ReservationService();
    }

    private ReservationService() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        entityManager = factory.createEntityManager();
    }


    /* Sets up the database tables. */
    public static void setup(List<Seat> seats) {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        EntityManager entityManager = factory.createEntityManager();
        Set<String> categories = new HashSet<>();
        entityManager.getTransaction().begin();
        for (Seat seat : seats) {
            categories.add(seat.getCategory());
            entityManager.persist(new SeatEntity(seat.getCategory(), seat.getSector(), seat.getRow(), seat.getNumber()));
        }

        seatCache.buildCaches(categories);

        List<SeatEntity> seatEntities = entityManager.createNamedQuery("SeatEntity.findAll", SeatEntity.class).getResultList();
        seatCache.buildCache(seatEntities);

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

        List<SeatEntity> availableSeats = seatCache.getAllEmptySeatsFromCategory(category, numberOfSeats);
        try {
            entityManager.getTransaction().begin();
            Seat[] seats = new Seat[numberOfSeats];
            for (int i = 0; i < numberOfSeats; i++) {
                SeatEntity entity = availableSeats.get(i);
                seats[i] = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber());
                entity.setReserved(true);
                entityManager.merge(entity);
            }
            entityManager.getTransaction().commit();
            return seats;

        } catch (OptimisticLockException | IndexOutOfBoundsException ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return null;
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return null;
        }
    }


}
