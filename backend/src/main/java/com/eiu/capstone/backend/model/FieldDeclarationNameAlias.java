package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "field_declaration_name_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "field_declaration_name_alias_parent_order_uq",
                columnNames = {"field_declaration_id", "order_index"}))
public class FieldDeclarationNameAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    @JoinColumn(name = "field_declaration_id", nullable = false,
            foreignKey = @ForeignKey(name = "field_declaration_name_alias_fd_fkey"))
    private FieldDeclaration fieldDeclaration;

    @Column(name = "`value`", nullable = false)
    private String value;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public FieldDeclarationNameAlias() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public FieldDeclaration getFieldDeclaration() {
        return fieldDeclaration;
    }

    public void setFieldDeclaration(FieldDeclaration fieldDeclaration) {
        this.fieldDeclaration = fieldDeclaration;
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
