package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "parameter")
public class Parameter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id", foreignKey = @ForeignKey(name = "parameter_method_id_fkey"))
    private Method method;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "constructor_id", foreignKey = @ForeignKey(name = "parameter_constructor_id_fkey"))
    private Constructor constructorEntity;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal = false;

    public Parameter() {}

    public UUID getId() {
        return id;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Constructor getConstructorEntity() {
        return constructorEntity;
    }

    public void setConstructorEntity(Constructor constructorEntity) {
        this.constructorEntity = constructorEntity;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}
