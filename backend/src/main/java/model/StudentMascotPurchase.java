package com.eiu.capstone.backend.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_mascot_purchase")
public class StudentMascotPurchase {

    @EmbeddedId
    private StudentMascotPurchaseKey id;

    @Column(name = "price_paid", nullable = false)
    private int pricePaid;

    @Column(name = "purchased_at", nullable = false)
    private OffsetDateTime purchasedAt;

    public StudentMascotPurchaseKey getId() {
        return id;
    }

    public void setId(StudentMascotPurchaseKey id) {
        this.id = id;
    }

    public int getPricePaid() {
        return pricePaid;
    }

    public void setPricePaid(int pricePaid) {
        this.pricePaid = pricePaid;
    }

    public OffsetDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(OffsetDateTime purchasedAt) {
        this.purchasedAt = purchasedAt;
    }
}
