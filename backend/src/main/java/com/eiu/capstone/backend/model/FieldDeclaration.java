package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "field_declaration")
public class FieldDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type")
    private String dataType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scope",
            foreignKey = @ForeignKey(name = "field_declaration_scope_fkey"))
    private MasterData scope;

    public FieldDeclaration() {}

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public MasterData getScope() {
        return scope;
    }

    public void setScope(MasterData scope) {
        this.scope = scope;
    }
}
