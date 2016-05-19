package ots;

import javax.persistence.*;
import java.io.Serializable;

/**
 * The class Sector contains the data of a sector of seats.
 */
@Entity
@NamedQueries({
        @NamedQuery(name = "SeatEntity.findAll", query = "SELECT s FROM SeatEntity s ORDER BY s.sector, s.row, s.number")
})
public class SeatEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String category;
    private String sector;
    private int row;
    private int number;
    private boolean reserved;
    @Version
    private int version;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public SeatEntity() {
    }

    public SeatEntity(String category, String sector, int row, int number) {
        this.category = category;
        this.sector = sector;
        this.row = row;
        this.number = number;
        this.reserved = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }
}
