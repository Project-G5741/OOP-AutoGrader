package com.eiu.capstone.backend.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "lab")
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    public Lab() {}

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Term getTerm() { return term; }
    public void setTerm(Term term) { this.term = term; }
}