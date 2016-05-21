package ots.strategy;

import ots.Seat;

import javax.persistence.EntityManager;

/**
 * Created by krigu on 21.05.16.
 */
public abstract class JPAReservationStrategy implements ReservationStrategy {

    protected EntityManager entityManager;

    public JPAReservationStrategy(EntityManager em) {
        this.entityManager = em;
    }

    @Override
    public Seat[] makeReservation(String category, int numberOfSeats) {
        try {
            entityManager.getTransaction().begin();

            Seat[] seats = handleReservation(category, numberOfSeats);

            entityManager.getTransaction().commit();

            return seats;

        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return null;
        }
    }

    protected abstract Seat[] handleReservation(String category, int numberOfSeats);
}
