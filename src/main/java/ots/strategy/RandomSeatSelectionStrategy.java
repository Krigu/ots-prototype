package ots.strategy;

import ots.Seat;
import ots.SeatEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Random;

public class RandomSeatSelectionStrategy extends JPAReservationStrategy {

    public RandomSeatSelectionStrategy(EntityManager em) {
        super(em);
    }

    @Override
    protected Seat[] handleReservation(String category, int numberOfSeats) {
        TypedQuery<SeatEntity> query = entityManager.createQuery("SELECT s FROM SeatEntity s WHERE s.category = :category AND s.reserved = false ORDER BY s.sector, s.row, s.number", SeatEntity.class);
        query.setParameter("category", category);
        List<SeatEntity> entities = query.getResultList();
        int nSeatsAvailable = entities.size();
        if (nSeatsAvailable >= numberOfSeats) {
            boolean adjacentSeatsFound = false;
            int seatIndex = 0;

            for (int tryCount = 0; !adjacentSeatsFound && tryCount <= 5; tryCount++) {

                int randIdx = new Random().nextInt(nSeatsAvailable);

                SeatEntity entity = entities.get(randIdx);
                int row = entity.getRow();
                String sector = entity.getSector();
                int j = 1;
                SeatEntity tmpEntity = entities.get(randIdx - j);
                while (tmpEntity.getRow() == row && tmpEntity.getSector().equals(sector)) {
                    j++;
                    tmpEntity = entities.get(randIdx - j);
                }

                seatIndex = randIdx - j + 1;

                tmpEntity = entities.get(seatIndex + numberOfSeats - 1);
                if (tmpEntity.getRow() == row && tmpEntity.getSector().equals(sector)) {
                    adjacentSeatsFound = true;
                }
            }

            Seat[] seats = new Seat[numberOfSeats];
            for (int i = 0; i < numberOfSeats; i++) {
                SeatEntity entity = entities.get(seatIndex + i);
                seats[i] = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber());
                entity.setReserved(true);
            }
            return seats;
        }
        return null;
    }
}
