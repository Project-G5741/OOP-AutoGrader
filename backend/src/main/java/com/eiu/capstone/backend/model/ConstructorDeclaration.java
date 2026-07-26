package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "constructor_declaration")
public class ConstructorDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope", nullable = false,
            foreignKey = @ForeignKey(name = "constructor_declaration_scope_fkey"))
    private MasterData scope;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    public ConstructorDeclaration() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MasterData getScope() {
        return scope;
    }

    public void setScope(MasterData scope) {
        this.scope = scope;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
