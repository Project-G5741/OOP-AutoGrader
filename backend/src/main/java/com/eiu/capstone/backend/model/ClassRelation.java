package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "class_relation")
public class ClassRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_relation_class_id_fkey"))
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_class_id", nullable = false,
            foreignKey = @ForeignKey(name = "class_relation_target_class_id_fkey"))
    private ClassEntity targetClassEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relation_type", nullable = false,
            foreignKey = @ForeignKey(name = "class_relation_relation_type_fkey"))
    private MasterData relationType;

    public ClassRelation() {}

    public UUID getId() {
        return id;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public ClassEntity getTargetClassEntity() {
        return targetClassEntity;
    }

    public void setTargetClassEntity(ClassEntity targetClassEntity) {
        this.targetClassEntity = targetClassEntity;
    }

    public MasterData getRelationType() {
        return relationType;
    }

    public void setRelationType(MasterData relationType) {
        this.relationType = relationType;
    }
}
