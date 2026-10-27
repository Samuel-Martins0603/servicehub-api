package br.com.servicehub.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Tabela "reviews": nota de 1 a 5 dada por quem contratou, apos a conclusao (RN09). */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private ServiceRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    // short -> SMALLINT, igual a coluna (o validate do Hibernate compara o tipo).
    @Column(nullable = false)
    private short rating;

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Review() {
        // exigido pelo JPA
    }

    public Review(ServiceRequest request, User reviewer, short rating, String comment) {
        this.request = request;
        this.reviewer = reviewer;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public ServiceRequest getRequest() { return request; }
    public User getReviewer() { return reviewer; }
    public short getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
