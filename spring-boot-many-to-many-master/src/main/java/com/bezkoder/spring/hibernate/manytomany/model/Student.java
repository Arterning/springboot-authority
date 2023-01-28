package com.bezkoder.spring.hibernate.manytomany.model;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student")
class Student {

    @Id
    @Column(name = "student_id")
    Long id;

    @Column(name = "student_name")
    private String name;


    @OneToMany(mappedBy = "student")
    Set<CourseRegistration> ratings = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<CourseRegistration> getRatings() {
        return ratings;
    }

    public void setRatings(Set<CourseRegistration> ratings) {
        this.ratings = ratings;
    }
}