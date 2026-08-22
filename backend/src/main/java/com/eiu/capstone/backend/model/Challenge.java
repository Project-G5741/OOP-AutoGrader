package com.eiu.capstone.backend.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "challenge",
        uniqueConstraints = @UniqueConstraint(
                name = "challenge_lab_id_challenge_number_key",
                columnNames = {"lab_id", "challenge_number"}
        )
)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_id", nullable = false,
            foreignKey = @ForeignKey(name = "challenge_lab_id_fkey"))
    private Lab lab;

    @Column(name = "challenge_number", nullable = false)
    private Integer challengeNumber;

    @Column(name = "has_mmd", nullable = false)
    private boolean hasMmd = true;

    @Column(name = "weight", nullable = false)
    private int weight = 1;

    @Column(name = "class_weight", nullable = false)
    private int classWeight = 1;

    @Column(name = "mmd_weight", nullable = false)
    private int mmdWeight = 1;

    @Column(name = "testcase_weight", nullable = false)
    private int testcaseWeight = 1;

    public Challenge() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Lab getLab() { return lab; }
    public void setLab(Lab lab) { this.lab = lab; }

    public Integer getChallengeNumber() { return challengeNumber; }
    public void setChallengeNumber(Integer challengeNumber) { this.challengeNumber = challengeNumber; }

    public boolean isHasMmd() { return hasMmd; }
    public void setHasMmd(boolean hasMmd) { this.hasMmd = hasMmd; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public int getClassWeight() { return classWeight; }
    public void setClassWeight(int classWeight) { this.classWeight = classWeight; }

    public int getMmdWeight() { return mmdWeight; }
    public void setMmdWeight(int mmdWeight) { this.mmdWeight = mmdWeight; }

    public int getTestcaseWeight() { return testcaseWeight; }
    public void setTestcaseWeight(int testcaseWeight) { this.testcaseWeight = testcaseWeight; }
}