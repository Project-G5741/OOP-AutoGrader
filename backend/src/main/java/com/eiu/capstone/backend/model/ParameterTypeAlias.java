package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "parameter_type_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "parameter_type_alias_parent_order_uq",
                columnNames = {"parameter_id", "order_index"}))
public class ParameterTypeAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parameter_id", nullable = false,
            foreignKey = @ForeignKey(name = "parameter_type_alias_parameter_fkey"))
    private Parameter parameter;

    @Column(nullable = false)
    private String value;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public ParameterTypeAlias() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Parameter getParameter() {
        return parameter;
    }

    public void setParameter(Parameter parameter) {
        this.parameter = parameter;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
