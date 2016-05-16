package ots;

import java.util.List;
import java.util.Random;

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
		Query query = entityManager.createQuery("SELECT s FROM SeatEntity s WHERE s.category = :category AND s.reserved = false ORDER BY s.sector, s.row, s.number");
		query.setParameter("category", category);
		List<SeatEntity> entities = query.getResultList();
		int nSeatsAvailable = entities.size();
		if ( nSeatsAvailable >= numberOfSeats )
		{
			try {
				boolean adjacentSeatsFound=false;
				int seatIndex=0;
				
				for ( int tryCount=0; !adjacentSeatsFound && tryCount <= 5; tryCount++ ){
				
					int randIdx = new Random().nextInt(nSeatsAvailable);
					
					SeatEntity entity = entities.get( randIdx );
					int row = entity.getRow();
					String sector = entity.getSector();
					int j=1;
					SeatEntity tmpEntity = entities.get( randIdx - j );
					while ( tmpEntity.getRow() == row && tmpEntity.getSector().equals(sector) )
					{
						entity = tmpEntity;
						j++;
						tmpEntity = entities.get( randIdx - j );
					}
					
					seatIndex=randIdx - j + 1;
					
					tmpEntity = entities.get( seatIndex + numberOfSeats - 1 );
					if ( tmpEntity.getRow() == row && tmpEntity.getSector().equals(sector) )
					{
						adjacentSeatsFound=true;
					}
				}
							
				Seat[] seats = new Seat[numberOfSeats];
				for (int i = 0; i < numberOfSeats; i++) {
					SeatEntity entity = entities.get( seatIndex + i );	
					seats[i] = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber() );
					entity.setReserved(true);
				}
				entityManager.getTransaction().commit();
				return seats;
			
			} catch (IndexOutOfBoundsException ex) {
				entityManager.getTransaction().rollback();
				return null;
			}
		}
		entityManager.getTransaction().rollback();
		return null;
	}
}
