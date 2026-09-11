package com.eiu.capstone.backend.model;

import jakarta.persistence.*;

import java.time.LocalDate;
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

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "student_visible", nullable = false)
    private boolean studentVisible = true;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    public Lab() {}

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Term getTerm() { return term; }
    public void setTerm(Term term) { this.term = term; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public boolean isStudentVisible() { return studentVisible; }
    public void setStudentVisible(boolean studentVisible) { this.studentVisible = studentVisible; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
}
