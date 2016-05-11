package ots;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

/**
 * The class ReservationService implements a service that allows to make seat reservations.
 */
public class ReservationService {

	private static final String PERSISTENCE_UNIT = "ots";
	private final EntityManager entityManager;

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
		entityManager.getTransaction().begin();
		Query query = entityManager.createQuery("SELECT s FROM SeatEntity s WHERE s.category = :category AND s.reserved = false");
		query.setParameter("category", category);
		List<SeatEntity> entities = query.getResultList();
		try {
			Seat[] seats = new Seat[numberOfSeats];
			for (int i = 0; i < numberOfSeats; i++) {
				SeatEntity entity = entities.get(i);
				seats[i] = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber());
				entity.setReserved(true);
			}
			entityManager.getTransaction().commit();
			return seats;
		} catch (IndexOutOfBoundsException ex) {
			entityManager.getTransaction().rollback();
			return null;
		}
	}
}
