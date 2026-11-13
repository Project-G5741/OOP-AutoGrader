package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "method_declaration_return_type_alias",
        uniqueConstraints = @UniqueConstraint(
                name = "method_declaration_return_type_alias_parent_order_uq",
                columnNames = {"method_declaration_id", "order_index"}))
public class MethodDeclarationReturnTypeAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "method_declaration_id", nullable = false,
            foreignKey = @ForeignKey(name = "method_declaration_return_type_alias_md_fkey"))
    private MethodDeclaration methodDeclaration;

    @Column(nullable = false)
    private String value;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public MethodDeclarationReturnTypeAlias() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
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
