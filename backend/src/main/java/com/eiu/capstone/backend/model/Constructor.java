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

@Entity
@Table(name = "constructor")
public class Constructor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "constructor_class_id_fkey"))
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "constructor_declaration_id", nullable = false,
            foreignKey = @ForeignKey(name = "constructor_constructor_declaration_id_fkey"))
    private ConstructorDeclaration constructorDeclaration;

    @Column(name = "name", nullable = false)
    private String name;

    public Constructor() {}

    public UUID getId() { return id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public ConstructorDeclaration getConstructorDeclaration() { return constructorDeclaration; }
    public void setConstructorDeclaration(ConstructorDeclaration constructorDeclaration) { this.constructorDeclaration = constructorDeclaration; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}