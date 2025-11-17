package kz.kstu.fit.batyrkhanov.schoolsystem.entity;

import jakarta.persistence.*;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;

@Entity
@Table(name = "directors")
public class Director {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    public Director() {}

    public Director(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
}
