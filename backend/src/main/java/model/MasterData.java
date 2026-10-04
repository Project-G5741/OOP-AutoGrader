package com.eiu.capstone.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "master_data")
public class MasterData {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "category", nullable = false)
    private String category = "UNSPECIFIED";

    public MasterData() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
