package com.eiu.capstone.backend.model;

// Lowercase to match the Postgres enum labels exactly (Hibernate maps by name).
public enum UserRole {
    teacher, student
}