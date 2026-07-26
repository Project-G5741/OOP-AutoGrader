package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "method_declaration")
public class MethodDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scope", nullable = false,
            foreignKey = @ForeignKey(name = "method_declaration_scope_fkey"))
    private MasterData scope;

    @Column(name = "return_type", nullable = false)
    private String returnType;

    @Column(name = "is_static", nullable = false)
    private boolean isStatic = false;

    @Column(name = "is_abstract", nullable = false)
    private boolean isAbstract = false;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal = false;

    public MethodDeclaration() {}

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

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}
