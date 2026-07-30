package io.github.cihadacar.taskhub.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    protected Tag() {
    }

    Tag(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String color() {
        return color;
    }
}
