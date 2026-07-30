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
@Table(name = "method")
public class Method {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "method_class_id_fkey"))
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_declaration_id", nullable = false,
            foreignKey = @ForeignKey(name = "method_method_declaration_id_fkey"))
    private MethodDeclaration methodDeclaration;

    @Column(name = "name", nullable = false)
    private String name;

    public Method() {}

    public UUID getId() { return id; }

    public ClassEntity getClassEntity() { return classEntity; }
    public void setClassEntity(ClassEntity classEntity) { this.classEntity = classEntity; }

    public MethodDeclaration getMethodDeclaration() { return methodDeclaration; }
    public void setMethodDeclaration(MethodDeclaration methodDeclaration) { this.methodDeclaration = methodDeclaration; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}