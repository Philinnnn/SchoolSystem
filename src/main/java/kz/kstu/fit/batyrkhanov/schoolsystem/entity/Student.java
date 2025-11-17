package kz.kstu.fit.batyrkhanov.schoolsystem.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    private Long id; // same as user ID

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @Column(name = "class_name")
    private String className;

    public Student() {}

    public Student(User user, String className) {
        this.user = user;
        this.className = className;
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
    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
}
