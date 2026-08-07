package com.eiu.capstone.backend.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Keeps {@code term_enrollment} aligned with students who already have lab progress.
 * Idempotent — safe on every application start.
 */
@Component
public class TermEnrollmentSyncService {

  @PersistenceContext
  private EntityManager entityManager;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void backfillFromLabProgress() {
    try {
      entityManager.createNativeQuery("""
              INSERT INTO term_enrollment (id, user_id, term_id)
              SELECT gen_random_uuid(), p.user_id, l.term_id
              FROM student_lab_progress p
              JOIN lab l ON l.id = p.lab_id
              ON CONFLICT ON CONSTRAINT term_enrollment_user_term_key DO NOTHING
              """)
          .executeUpdate();
    } catch (RuntimeException ignored) {
      // term_enrollment may not exist until operator runs docs/term-enrollment-backfill.sql
    }
  }
}
