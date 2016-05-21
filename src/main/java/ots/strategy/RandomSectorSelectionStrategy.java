package ots.strategy;

import ots.Seat;
import ots.SeatEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by krigu on 21.05.16.
 */
public class RandomSectorSelectionStrategy extends JPAReservationStrategy {

    public RandomSectorSelectionStrategy(EntityManager em) {
        super(em);
    }

    @Override
    protected Seat[] handleReservation(String category, int numberOfSeats) {

        List<String> sectors = getSectors(category);
        if (sectors.isEmpty()) {
            entityManager.getTransaction().rollback();
            return null;
        }

        int index = sectors.size() == 1 ? 0 : (int) (Math.random() * (sectors.size()));
        String sector = sectors.get(index);

        List<SeatEntity> entities = getAvailableSeats(category, sector);
        List<SeatEntity> reserved = getSeats(entities, numberOfSeats);

        if (isCorrect(reserved, numberOfSeats)) {
            List<Seat> seats = new ArrayList<>();
            for (SeatEntity entity : reserved) {
                Seat seat = new Seat(entity.getCategory(), entity.getSector(), entity.getRow(), entity.getNumber());
                seats.add(seat);
                entity.setReserved(true);
            }

            return seats.toArray(new Seat[seats.size()]);
        }

        return null;
    }

    private List<String> getSectors(String category) {
        Query query = entityManager.createQuery("SELECT distinct(s.sector) FROM SeatEntity s WHERE s.category = :category AND s.reserved = false");
        query.setParameter("category", category);
        List<String> entities = query.getResultList();
        return entities;
    }

    private List<SeatEntity> getAvailableSeats(String category, String sector) {
        Query query = entityManager.createQuery("SELECT s FROM SeatEntity s WHERE s.category = :category AND s.sector = :sector AND s.reserved = false order by s.sector, s.row, s.number");
        query.setParameter("category", category);
        query.setParameter("sector", sector);

        List<SeatEntity> entities = query.getResultList();
        return entities;
    }

    private List<SeatEntity> getSeats(List<SeatEntity> entities, int numberOfSeats) {
        List<SeatEntity> reserved = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i).isReserved()) {
                reserved.clear();
                continue;
            }

            if (reserved.isEmpty()) {
                reserved.add(entities.get(i));
            }

            if (reserved.size() == numberOfSeats) {
                return reserved;
            }

            for (int j = i + 1; j < entities.size(); j++) {
                SeatEntity last = reserved.get(reserved.size() - 1);
                SeatEntity entity = entities.get(j);
                if (!entity.isReserved() && isAdjacent(last, entity)) {
                    reserved.add(entity);

                    if (reserved.size() == numberOfSeats) {
                        return reserved;
                    }
                } else {
                    reserved.clear();
                    break;
                }
            }
        }
        return reserved;
    }

    private boolean isCorrect(List<SeatEntity> entities, int numberOfSeats) {
        if (entities.isEmpty()) {
            return false;
        }
        if (numberOfSeats != entities.size()) {
            return false;
        }

        if (numberOfSeats == 1) {
            return true;
        }
        SeatEntity last = entities.get(0);
        for (int i = 1; i < entities.size(); i++) {
            SeatEntity entity = entities.get(i);

            if (!isAdjacent(last, entity)) {
                return false;
            }
            last = entity;
        }
        return true;
    }

    private boolean isAdjacent(SeatEntity seat1, SeatEntity seat2) {
        return seat1.getCategory().equals(seat2.getCategory()) && seat1.getSector().equals(seat2.getSector()) && seat1.getRow() == seat2.getRow() && seat2.getNumber() - seat1.getNumber() == 1;
    }
}
