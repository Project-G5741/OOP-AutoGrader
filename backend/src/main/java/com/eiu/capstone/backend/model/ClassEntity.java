package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "class_entity")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_entity_challenge_id_fkey"))
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope", nullable = false,
            foreignKey = @ForeignKey(name = "class_entity_scope_fkey"))
    private MasterData scope;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "declaring_type", nullable = false,
            foreignKey = @ForeignKey(name = "class_entity_declaring_type_fkey"))
    private MasterData declaringType;

    @Column(name = "is_abstract", nullable = false)
    private boolean isAbstract = false;

    public ClassEntity() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public MasterData getScope() {
        return scope;
    }

    public void setScope(MasterData scope) {
        this.scope = scope;
    }

    public MasterData getDeclaringType() {
        return declaringType;
    }

    public void setDeclaringType(MasterData declaringType) {
        this.declaringType = declaringType;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }
}
