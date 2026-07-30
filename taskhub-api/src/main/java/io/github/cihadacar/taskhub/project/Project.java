package io.github.cihadacar.taskhub.project;

import java.time.Instant;

import io.github.cihadacar.taskhub.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, insertable = false, updatable = false)
    private UserAccount owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
    }

    Project(String name, String description, Long ownerId, UserAccount owner, Instant now) {
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.owner = owner;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void update(String name, String description, Instant now) {
        this.name = name;
        this.description = description;
        this.updatedAt = now;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Long ownerId() {
        return ownerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
