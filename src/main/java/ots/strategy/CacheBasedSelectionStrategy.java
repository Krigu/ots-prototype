package ots.strategy;

import ots.ReservationService;
import ots.Seat;
import ots.SeatEntity;
import ots.cache.SeatCache;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by krigu on 21.05.16.
 */
public class CacheBasedSelectionStrategy extends JPAReservationStrategy {

    private static SeatCache seatCache = new SeatCache();

    /**
     * Static initialization of the cache. Should be handled more gracefully in production code
     */
    public static void setupCache() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(ReservationService.PERSISTENCE_UNIT);
        EntityManager em = factory.createEntityManager();
        List<String> categories = em.createQuery("select s.category from SeatEntity s group by s.category", String.class).getResultList();

        seatCache.buildCaches(categories);

        List<SeatEntity> seatEntities = em.createNamedQuery("SeatEntity.findAll", SeatEntity.class).getResultList();
        seatCache.buildCache(seatEntities);
        em.close();
    }

    public CacheBasedSelectionStrategy(EntityManager em) {
        super(em);
    }

    @Override
    protected Seat[] handleReservation(String category, int numberOfSeats) {
        List<SeatEntity> availableSeats = seatCache.getAllEmptySeatsFromCategory(category, numberOfSeats);
        Seat[] seats = new Seat[numberOfSeats];
        for (int i = 0; i < numberOfSeats; i++) {
            SeatEntity entity = availableSeats.get(i);
            seats[i] = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber());
            entity.setReserved(true);
            entityManager.merge(entity);
        }
        return seats;
    }
}
